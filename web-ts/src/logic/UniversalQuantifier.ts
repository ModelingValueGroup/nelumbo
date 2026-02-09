/**
 * UniversalQuantifier - forall quantifier.
 * Ported from Java: org.modelingvalue.nelumbo.logic.UniversalQuantifier
 */

import { List, Set } from 'immutable';
import type { AstElement } from '../core/AstElement';
import { Variable } from '../core/Variable';
import { Node } from '../core/Node';
import type { Functor } from '../patterns/Functor';
import { Predicate } from './Predicate';
import { Quantifier } from './Quantifier';
import { InferResult } from './InferResult';
import type { InferContext } from './InferContext';

/**
 * UniversalQuantifier - "for all" quantification.
 * True if the body is true for all bindings of the quantified variables.
 */
export class UniversalQuantifier extends Quantifier {
  constructor(functor: Functor, elements: List<AstElement>, variables: List<Variable>, body: Node) {
    super(functor, elements, variables, body);
  }

  protected static fromDataUniversal(data: unknown[], declaration?: Node): UniversalQuantifier {
    const uq = Object.create(UniversalQuantifier.prototype) as UniversalQuantifier;
    (uq as unknown as { _data: unknown[] })._data = data;
    (uq as unknown as { _declaration: Node })._declaration = declaration ?? uq;
    return uq;
  }

  protected override struct(data: unknown[], declaration?: Node): UniversalQuantifier {
    return UniversalQuantifier.fromDataUniversal(data, declaration ?? this.declaration());
  }

  protected override processResult(
    bodyResult: InferResult,
    localVars: Set<Variable>,
    _context: InferContext
  ): InferResult {
    // For universal: true if all bindings make body true, false if any makes it false
    const facts = bodyResult.facts();
    const falsehoods = bodyResult.falsehoods();

    // Remove local variables from falsehoods first (check for counterexamples)
    const cleanFalsehoods = this.removeLocalVars(falsehoods, localVars);

    // If we have any falsehoods (counterexamples), universal is false
    if (!cleanFalsehoods.isEmpty() || !falsehoods.isEmpty()) {
      return InferResult.of(
        Set(),
        bodyResult.completeFacts(),
        Set([this as Predicate]),
        bodyResult.completeFalsehoods(),
        bodyResult.cycles()
      );
    }

    // If body is completely true, universal is true
    if (bodyResult.isTrueCC()) {
      return InferResult.factsCC(Set([this as Predicate]));
    }

    // Remove local variables from facts
    const cleanFacts = this.removeLocalVars(facts, localVars);

    // Otherwise unknown or partially true
    return InferResult.of(
      cleanFacts.isEmpty() ? Set() : Set([this as Predicate]),
      bodyResult.completeFacts(),
      Set(),
      bodyResult.completeFalsehoods(),
      bodyResult.cycles()
    );
  }

  override toString(): string {
    const vars = this.localVars().map(v => v.name()).join(', ');
    return `∀(${vars}): ${this.body()}`;
  }
}
