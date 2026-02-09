/**
 * MatchState - trie for rule/transform signature matching.
 * Ported from Java: org.modelingvalue.nelumbo.MatchState
 */

import { Map, Set } from 'immutable';
import { TokenType } from '../TokenType';
import { Type } from '../core/Type';
import { Node } from '../core/Node';
import type { Functor } from '../patterns/Functor';

/**
 * MatchState - trie for matching rules and transforms.
 */
export class MatchState<E> {
  static readonly EMPTY: MatchState<never> = new MatchState<never>(
    null,
    Map(),
    Set()
  );

  static empty<E>(): MatchState<E> {
    return MatchState.EMPTY as MatchState<E>;
  }

  private readonly _element: E | null;
  private readonly _transitions: Map<unknown, MatchState<E>>;
  private readonly _elements: Set<E>;

  constructor(
    element: E | null,
    transitions: Map<unknown, MatchState<E>> = Map(),
    elements: Set<E> = Set()
  ) {
    this._element = element;
    this._transitions = transitions;
    this._elements = elements;
  }

  /**
   * Create a terminal state for an element.
   */
  static of<E>(element: E): MatchState<E> {
    return new MatchState(element, Map(), Set([element]));
  }

  /**
   * Add a transition by type.
   */
  withType(type: Type): MatchState<E> {
    return new MatchState(
      this._element,
      this._transitions.set(type, this),
      this._elements
    );
  }

  /**
   * Add a transition by token type.
   */
  withTokenType(tokenType: TokenType): MatchState<E> {
    return new MatchState(
      this._element,
      this._transitions.set(tokenType, this),
      this._elements
    );
  }

  /**
   * Add a transition by class.
   */
  withClass(clss: unknown): MatchState<E> {
    return new MatchState(
      this._element,
      this._transitions.set(clss, this),
      this._elements
    );
  }

  /**
   * Add a transition by functor.
   */
  withFunctor(functor: Functor): MatchState<E> {
    return new MatchState(
      this._element,
      this._transitions.set(functor, this),
      this._elements
    );
  }

  /**
   * Merge with another match state.
   */
  merge(other: MatchState<E>): MatchState<E> {
    let transitions = this._transitions;
    for (const [key, state] of other._transitions) {
      const existing = transitions.get(key);
      if (existing !== undefined) {
        transitions = transitions.set(key, existing.merge(state));
      } else {
        transitions = transitions.set(key, state);
      }
    }

    return new MatchState(
      this._element ?? other._element,
      transitions,
      this._elements.union(other._elements)
    );
  }

  /**
   * Match a node against this state.
   */
  match(node: Node): Set<E> {
    let result = this._elements;

    // Match by type
    for (const sup of node.type().allSupers()) {
      const state = this._transitions.get(sup);
      if (state !== undefined) {
        result = result.union(state._elements);
      }
    }

    // Match by functor
    const functor = node.functor();
    if (functor !== null) {
      const state = this._transitions.get(functor);
      if (state !== undefined) {
        result = result.union(state._elements);
      }
    }

    return result;
  }
}
