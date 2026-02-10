/**
 * And - logical AND predicate.
 * Ported from Java: org.modelingvalue.nelumbo.logic.And
 */

import { List, Set } from 'immutable';
import type { AstElement } from '../AstElement';
import { Node } from '../Node';
import type { Functor } from '../patterns/Functor';
import { Predicate } from './Predicate';
import { BinaryPredicate } from './BinaryPredicate';
import { InferResult } from '../InferResult';
import type { InferContext } from '../InferContext';

/**
 * And - logical conjunction.
 */
export class And extends BinaryPredicate {
  constructor(functor: Functor, elements: List<AstElement>, left: Node, right: Node) {
    super(functor, elements, left, right);
  }

  protected static fromDataAnd(data: unknown[], declaration?: Node): And {
    const and = Object.create(And.prototype) as And;
    (and as unknown as { _data: unknown[] })._data = data;
    (and as unknown as { _declaration: Node })._declaration = declaration ?? and;
    return and;
  }

  protected override struct(data: unknown[], declaration?: Node): And {
    return And.fromDataAnd(data, declaration ?? this.declaration());
  }

  protected override resultIsTrue(predResult: InferResult): boolean {
    return predResult.isTrueCC();
  }

  protected override resultIsFalse(predResult: InferResult): boolean {
    return predResult.isFalseCC();
  }

  protected override resultIsUnknown(predResult: InferResult): boolean {
    return !predResult.isTrueCC() && !predResult.isFalseCC();
  }

  protected override canShortCircuit(result: InferResult): boolean {
    // Short-circuit if first is definitely false
    return result.isFalseCC();
  }

  protected override shortCircuitValue(pred: Predicate, _result: InferResult): InferResult {
    // AND is false if either operand is false
    return InferResult.falsehoodsCC(Set([pred]));
  }

  protected override combineResults(
    pred: Predicate,
    firstResult: InferResult,
    secondResult: InferResult,
    _context: InferContext
  ): InferResult {
    // If either is false, result is false
    if (firstResult.isFalseCC() || secondResult.isFalseCC()) {
      return InferResult.falsehoodsCC(Set([pred]));
    }

    // If both are true, result is true
    if (firstResult.isTrueCC() && secondResult.isTrueCC()) {
      return InferResult.factsCC(Set([pred]));
    }

    // Otherwise combine with AND semantics
    const combinedFacts = firstResult.facts().intersect(secondResult.facts());
    const combinedFalsehoods = firstResult.falsehoods().union(secondResult.falsehoods());
    const completeFacts = firstResult.completeFacts() && secondResult.completeFacts();
    const completeFalsehoods = firstResult.completeFalsehoods() || secondResult.completeFalsehoods();
    const cycles = firstResult.cycles().union(secondResult.cycles());

    // If we have facts from intersection, return those
    if (!combinedFacts.isEmpty()) {
      return InferResult.of(
        Set([pred]),
        completeFacts,
        combinedFalsehoods,
        completeFalsehoods,
        cycles
      );
    }

    // Return combined result
    return InferResult.of(
      combinedFacts,
      completeFacts,
      combinedFalsehoods,
      completeFalsehoods,
      cycles
    );
  }

  override toString(): string {
    return `(${this.left()} & ${this.right()})`;
  }
}
