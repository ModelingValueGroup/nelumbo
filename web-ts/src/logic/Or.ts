/**
 * Or - logical OR predicate.
 * Ported from Java: org.modelingvalue.nelumbo.logic.Or
 */

import { List, Set } from 'immutable';
import type { AstElement } from '../core/AstElement';
import { Node } from '../core/Node';
import type { Functor } from '../patterns/Functor';
import { Predicate } from './Predicate';
import { BinaryPredicate } from './BinaryPredicate';
import { InferResult } from './InferResult';
import type { InferContext } from './InferContext';

/**
 * Or - logical disjunction.
 */
export class Or extends BinaryPredicate {
  constructor(functor: Functor, elements: List<AstElement>, left: Node, right: Node) {
    super(functor, elements, left, right);
  }

  protected static fromDataOr(data: unknown[], declaration?: Node): Or {
    const or = Object.create(Or.prototype) as Or;
    (or as unknown as { _data: unknown[] })._data = data;
    (or as unknown as { _declaration: Node })._declaration = declaration ?? or;
    return or;
  }

  protected override struct(data: unknown[], declaration?: Node): Or {
    return Or.fromDataOr(data, declaration ?? this.declaration());
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
    // Short-circuit if first is definitely true
    return result.isTrueCC();
  }

  protected override shortCircuitValue(pred: Predicate, _result: InferResult): InferResult {
    // OR is true if either operand is true
    return InferResult.factsCC(Set([pred]));
  }

  protected override combineResults(
    pred: Predicate,
    firstResult: InferResult,
    secondResult: InferResult,
    _context: InferContext
  ): InferResult {
    // If either is true, result is true
    if (firstResult.isTrueCC() || secondResult.isTrueCC()) {
      return InferResult.factsCC(Set([pred]));
    }

    // If both are false, result is false
    if (firstResult.isFalseCC() && secondResult.isFalseCC()) {
      return InferResult.falsehoodsCC(Set([pred]));
    }

    // Otherwise combine with OR semantics
    const combinedFacts = firstResult.facts().union(secondResult.facts());
    const combinedFalsehoods = firstResult.falsehoods().intersect(secondResult.falsehoods());
    const completeFacts = firstResult.completeFacts() || secondResult.completeFacts();
    const completeFalsehoods = firstResult.completeFalsehoods() && secondResult.completeFalsehoods();
    const cycles = firstResult.cycles().union(secondResult.cycles());

    // If we have facts from union, return those
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
    return `(${this.left()} | ${this.right()})`;
  }
}
