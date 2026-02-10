/**
 * ExistentialQuantifier - exists quantifier.
 * Ported from Java: org.modelingvalue.nelumbo.logic.ExistentialQuantifier
 */

import { List, Set } from 'immutable';
import type { AstElement } from '../AstElement';
import { Variable } from '../Variable';
import { Node } from '../Node';
import type { Functor } from '../patterns/Functor';
import { Predicate } from './Predicate';
import { Quantifier } from './Quantifier';
import { InferResult } from '../InferResult';
import type { InferContext } from '../InferContext';

/**
 * ExistentialQuantifier - "there exists" quantification.
 * True if the body is true for at least one binding of the quantified variables.
 */
export class ExistentialQuantifier extends Quantifier {
  constructor(functor: Functor, elements: List<AstElement>, variables: List<Variable>, body: Node) {
    super(functor, elements, variables, body);
  }

  protected static fromDataExistential(data: unknown[], declaration?: Node): ExistentialQuantifier {
    const eq = Object.create(ExistentialQuantifier.prototype) as ExistentialQuantifier;
    (eq as unknown as { _data: unknown[] })._data = data;
    (eq as unknown as { _declaration: Node })._declaration = declaration ?? eq;
    return eq;
  }

  protected override struct(data: unknown[], declaration?: Node): ExistentialQuantifier {
    return ExistentialQuantifier.fromDataExistential(data, declaration ?? this.declaration());
  }

  protected override processResult(
    bodyResult: InferResult,
    localVars: Set<Variable>,
    _context: InferContext
  ): InferResult {
    // For existential: true if any binding makes body true
    const facts = bodyResult.facts();
    const falsehoods = bodyResult.falsehoods();

    // Remove local variables from facts
    const cleanFacts = this.removeLocalVars(facts, localVars);

    // If we have any facts (after removing locals), existential is true
    if (!cleanFacts.isEmpty() || !facts.isEmpty()) {
      return InferResult.of(
        Set([this as Predicate]),
        bodyResult.completeFacts(),
        Set(),
        bodyResult.completeFalsehoods(),
        bodyResult.cycles()
      );
    }

    // If body is completely false, existential is false
    if (bodyResult.isFalseCC()) {
      return InferResult.falsehoodsCC(Set([this as Predicate]));
    }

    // Remove local variables from falsehoods
    const cleanFalsehoods = this.removeLocalVars(falsehoods, localVars);

    // Otherwise unknown
    return InferResult.of(
      Set(),
      bodyResult.completeFacts(),
      cleanFalsehoods.isEmpty() ? Set() : Set([this as Predicate]),
      bodyResult.completeFalsehoods(),
      bodyResult.cycles()
    );
  }

  override toString(): string {
    const vars = this.localVars().map(v => v.name()).join(', ');
    return `∃(${vars}): ${this.body()}`;
  }
}
