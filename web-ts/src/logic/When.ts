/**
 * When - conditional predicate (if-then).
 * @JAVA_REF org.modelingvalue.nelumbo.logic.When
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
 * When - conditional logic (condition => consequence).
 * True if condition is false, or if condition and consequence are both true.
 */
export class When extends BinaryPredicate {
  constructor(functor: Functor, elements: List<AstElement>, condition: Node, consequence: Node) {
    super(functor, elements, condition, consequence);
  }

  protected static fromDataWhen(data: unknown[], declaration?: Node): When {
    const when = Object.create(When.prototype) as When;
    (when as unknown as { _data: unknown[] })._data = data;
    (when as unknown as { _declaration: Node })._declaration = declaration ?? when;
    return when;
  }

  protected override struct(data: unknown[], declaration?: Node): When {
    return When.fromDataWhen(data, declaration ?? this.declaration());
  }

  /**
   * Get the condition.
   */
  condition(): Predicate {
    return this.left();
  }

  /**
   * Get the consequence.
   */
  consequence(): Predicate {
    return this.right();
  }

  protected override resultIsTrue(_predResult: InferResult): boolean {
    // When is true if condition is false
    return _predResult.isFalseCC();
  }

  protected override resultIsFalse(_predResult: InferResult): boolean {
    // When is false if condition is true and consequence is false
    return false; // Determined by both condition and consequence
  }

  protected override resultIsUnknown(predResult: InferResult): boolean {
    return !predResult.isTrueCC() && !predResult.isFalseCC();
  }

  protected override canShortCircuit(result: InferResult): boolean {
    // Short-circuit if condition is false (implication is vacuously true)
    return result.isFalseCC();
  }

  protected override shortCircuitValue(pred: Predicate, _result: InferResult): InferResult {
    // If condition is false, when is true (vacuous truth)
    return InferResult.factsCC(Set([pred]));
  }

  protected override combineResults(
    pred: Predicate,
    conditionResult: InferResult,
    consequenceResult: InferResult,
    _context: InferContext
  ): InferResult {
    // If condition is false, when is true (vacuous truth)
    if (conditionResult.isFalseCC()) {
      return InferResult.factsCC(Set([pred]));
    }

    // If condition is true and consequence is true, when is true
    if (conditionResult.isTrueCC() && consequenceResult.isTrueCC()) {
      return InferResult.factsCC(Set([pred]));
    }

    // If condition is true and consequence is false, when is false
    if (conditionResult.isTrueCC() && consequenceResult.isFalseCC()) {
      return InferResult.falsehoodsCC(Set([pred]));
    }

    // Otherwise, result depends on consequence
    const completeFacts = conditionResult.completeFacts() && consequenceResult.completeFacts();
    const completeFalsehoods = conditionResult.completeFalsehoods() && consequenceResult.completeFalsehoods();
    const cycles = conditionResult.cycles().union(consequenceResult.cycles());

    // If we have consequence facts, when might be true
    if (!consequenceResult.facts().isEmpty()) {
      return InferResult.of(
        Set([pred]),
        completeFacts,
        Set(),
        completeFalsehoods,
        cycles
      );
    }

    // If we have consequence falsehoods, when might be false
    if (!consequenceResult.falsehoods().isEmpty() && !conditionResult.facts().isEmpty()) {
      return InferResult.of(
        Set(),
        completeFacts,
        Set([pred]),
        completeFalsehoods,
        cycles
      );
    }

    // Unknown
    return InferResult.of(
      Set(),
      completeFacts,
      Set(),
      completeFalsehoods,
      cycles
    );
  }

  override toString(): string {
    return `(${this.condition()} => ${this.consequence()})`;
  }
}
