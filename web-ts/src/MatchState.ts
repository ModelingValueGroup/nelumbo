/**
 * MatchState - trie for rule/transform signature matching.
 * @JAVA_REF org.modelingvalue.nelumbo.MatchState
 */

import { Map, Set } from 'immutable';
import { TokenType } from './syntax/TokenType';
import { Type } from './Type';
import { Variable } from './Variable';
import { Node } from './Node';
import type { Functor } from './patterns/Functor';

/**
 * MatchState - trie for matching rules and transforms.
 */
export class MatchState<E> {
  static readonly EMPTY: MatchState<never> = new MatchState<never>(
    Map(),
    Set()
  );

  static empty<E>(): MatchState<E> {
    return MatchState.EMPTY as MatchState<E>;
  }

  private readonly _transitions: Map<unknown, MatchState<E>>;
  private readonly _elements: Set<E>;

  private constructor(
    transitions: Map<unknown, MatchState<E>>,
    elements: Set<E>
  ) {
    this._transitions = transitions;
    this._elements = elements;
  }

  /**
   * Create a terminal state for an element.
   */
  static of<E>(element: E): MatchState<E> {
    return new MatchState(
      Map(),
      element !== null && element !== undefined ? Set([element]) : Set()
    );
  }

  /**
   * Add a transition by type.
   */
  withType(type: Type): MatchState<E> {
    return new MatchState(
      Map<unknown, MatchState<E>>([[type, this]]),
      Set()
    );
  }

  /**
   * Add a transition by token type.
   */
  withTokenType(tokenType: TokenType): MatchState<E> {
    return new MatchState(
      Map<unknown, MatchState<E>>([[tokenType, this]]),
      Set()
    );
  }

  /**
   * Add a transition by class.
   */
  withClass(clss: unknown): MatchState<E> {
    return new MatchState(
      Map<unknown, MatchState<E>>([[clss, this]]),
      Set()
    );
  }

  /**
   * Add a transition by functor.
   */
  withFunctor(functor: Functor): MatchState<E> {
    return new MatchState(
      Map<unknown, MatchState<E>>([[functor, this]]),
      Set()
    );
  }

  transitions(): Map<unknown, MatchState<E>> {
    return this._transitions;
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

    let transitions = this._transitions;
    for (const [key, state] of other._transitions) {
      const existing = transitions.get(key);
      if (existing !== undefined) {
        transitions = transitions.set(key, existing.merge(state));
      } else {
        transitions = transitions.set(key, state);
      }
    }

    // Handle type hierarchy: subtypes inherit supertype patterns
    for (const key of transitions.keys()) {
      if (key instanceof Type) {
        const subType = key;
        for (const superType of subType.allSupers()) {
          if (!superType.equals(subType)) {
            const superState = transitions.get(superType);
            if (superState !== undefined) {
              const subState = transitions.get(subType)!;
              transitions = transitions.set(subType, subState.merge(superState));
            }
          }
        }
      }
    }

    return new MatchState(
      transitions,
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
      let state: MatchState<E> | null = functor !== null ? (this._transitions.get(functor) ?? null) : null;
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
      return this._transitions.get(TokenType.of(obj)) ?? null;
    }
    if (obj !== null && obj !== undefined) {
      return this._transitions.get((obj as object).constructor) ?? null;
    }
    return null;
  }

  /**
   * Match a type by walking its supertype hierarchy.
   */
  private matchType(type: Type): MatchState<E> | null {
    for (const sup of type.allSupers()) {
      const state = this._transitions.get(sup);
      if (state !== undefined) {
        return state;
      }
    }
    return this._transitions.get(Type.TYPE) ?? null;
  }
}
