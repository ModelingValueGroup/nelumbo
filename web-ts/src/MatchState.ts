/**
 * MatchState - trie for rule/transform signature matching.
 * @JAVA_REF org.modelingvalue.nelumbo.MatchState
 *
 * Note: Java uses HashMap which uses equals/hashCode for key comparison.
 * immutable.js Map uses === for non-Immutable keys. Since Functor and Type
 * are plain JS classes, we need custom lookup that uses structural equality.
 */

import { Set } from 'immutable';
import { TokenType } from './syntax/TokenType';
import { Type } from './Type';
import { Variable } from './Variable';
import { Node } from './Node';
import type { Functor } from './patterns/Functor';

/**
 * MatchState - trie for matching rules and transforms.
 * Uses a plain array of [key, value] entries instead of immutable.js Map
 * to support structural equality for Node-based keys (like Functor, Type).
 */
export class MatchState<E> {
  static readonly EMPTY: MatchState<never> = new MatchState<never>(
    [],
    Set()
  );

  static empty<E>(): MatchState<E> {
    return MatchState.EMPTY as MatchState<E>;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.MatchState uses HashMap with equals/hashCode.
  // We use an array of entries with custom equality to match Java behavior.
  private readonly _entries: [unknown, MatchState<E>][];
  private readonly _elements: Set<E>;

  private constructor(
    entries: [unknown, MatchState<E>][],
    elements: Set<E>
  ) {
    this._entries = entries;
    this._elements = elements;
  }

  /**
   * Compare two keys for equality (matching Java's equals/hashCode behavior).
   */
  private static keysEqual(a: unknown, b: unknown): boolean {
    if (a === b) return true;
    if (a instanceof Node && b instanceof Node) return a.equals(b);
    if (a instanceof Type && b instanceof Type) return a.equals(b);
    return false;
  }

  /**
   * Look up a key in the entries array using structural equality.
   */
  private getEntry(key: unknown): MatchState<E> | undefined {
    for (const [k, v] of this._entries) {
      if (MatchState.keysEqual(k, key)) return v;
    }
    return undefined;
  }

  /**
   * Set a key/value in the entries array using structural equality.
   */
  private setEntry(entries: [unknown, MatchState<E>][], key: unknown, value: MatchState<E>): [unknown, MatchState<E>][] {
    const result = [...entries];
    for (let i = 0; i < result.length; i++) {
      if (MatchState.keysEqual(result[i][0], key)) {
        result[i] = [key, value];
        return result;
      }
    }
    result.push([key, value]);
    return result;
  }

  /**
   * Create a terminal state for an element.
   */
  static of<E>(element: E): MatchState<E> {
    return new MatchState(
      [],
      element !== null && element !== undefined ? Set([element]) : Set()
    );
  }

  /**
   * Add a transition by type.
   */
  withType(type: Type): MatchState<E> {
    return new MatchState(
      [[type, this]],
      Set()
    );
  }

  /**
   * Add a transition by token type.
   */
  withTokenType(tokenType: TokenType): MatchState<E> {
    return new MatchState(
      [[tokenType, this]],
      Set()
    );
  }

  /**
   * Add a transition by class.
   */
  withClass(clss: unknown): MatchState<E> {
    return new MatchState(
      [[clss, this]],
      Set()
    );
  }

  /**
   * Add a transition by functor.
   */
  withFunctor(functor: Functor): MatchState<E> {
    return new MatchState(
      [[functor, this]],
      Set()
    );
  }

  transitions(): [unknown, MatchState<E>][] {
    return this._entries;
  }

  elements(): Set<E> {
    return this._elements;
  }

  /**
   * Merge with another match state.
   */
  merge(other: MatchState<E> | null): MatchState<E> {
    if (other === null) {
      return this;
    }

    let entries = [...this._entries] as [unknown, MatchState<E>][];
    for (const [key, state] of other._entries) {
      const existingIdx = entries.findIndex(([k]) => MatchState.keysEqual(k, key));
      if (existingIdx >= 0) {
        entries[existingIdx] = [key, entries[existingIdx][1].merge(state)];
      } else {
        entries.push([key, state]);
      }
    }

    // Handle type hierarchy: subtypes inherit supertype patterns
    for (let i = 0; i < entries.length; i++) {
      const key = entries[i][0];
      if (key instanceof Type) {
        const subType = key;
        for (const superType of subType.allSupers()) {
          if (!superType.equals(subType)) {
            const superIdx = entries.findIndex(([k]) => MatchState.keysEqual(k, superType));
            if (superIdx >= 0) {
              entries[i] = [key, entries[i][1].merge(entries[superIdx][1])];
            }
          }
        }
      }
    }

    return new MatchState(
      entries,
      this._elements.union(other._elements)
    );
  }

  /**
   * Match an object against this state, returning matching elements.
   */
  match(obj: unknown): Set<E> {
    const state = this.doMatch(obj);
    return state !== null ? state._elements : Set();
  }

  /**
   * Internal recursive match traversal.
   * @JAVA_REF org.modelingvalue.nelumbo.MatchState#doMatch(Object)
   */
  private doMatch(obj: unknown): MatchState<E> | null {
    if (obj instanceof Type) {
      return this.matchType(obj);
    }
    if (obj instanceof Variable) {
      return this.matchType(obj.type());
    }
    if (obj instanceof Node) {
      const node = obj;
      const functor = node.functor();
      let state: MatchState<E> | null = functor !== null ? (this.getEntry(functor) ?? null) : null;
      if (state !== null) {
        for (const arg of node.args()) {
          state = state!.doMatch(arg);
          if (state === null) {
            break;
          }
        }
      }
      if (state === null) {
        state = this.matchType(node.type());
      }
      return state;
    }
    if (typeof obj === 'string') {
      return this.getEntry(TokenType.of(obj)) ?? null;
    }
    if (obj !== null && obj !== undefined) {
      return this.getEntry((obj as object).constructor) ?? null;
    }
    return null;
  }

  /**
   * Match a type by walking its supertype hierarchy.
   */
  private matchType(type: Type): MatchState<E> | null {
    for (const sup of type.allSupers()) {
      const state = this.getEntry(sup);
      if (state !== undefined) {
        return state;
      }
    }
    return this.getEntry(Type.TYPE) ?? null;
  }
}
