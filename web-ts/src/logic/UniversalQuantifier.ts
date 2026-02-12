/**
 * UniversalQuantifier - forall quantifier.
 * @JAVA_REF org.modelingvalue.nelumbo.logic.UniversalQuantifier
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
 * UniversalQuantifier - "for all" quantification.
 * @JAVA_REF org.modelingvalue.nelumbo.logic.UniversalQuantifier
 */
export class UniversalQuantifier extends Quantifier {
  // @JAVA_REF org.modelingvalue.nelumbo.logic.UniversalQuantifier#FUNCTOR
  static FUNCTOR: Functor | null = null;

  constructor(functor: Functor, elements: List<AstElement>, ...args: unknown[]) {
    super(functor, elements, ...args);
  }

  protected static fromDataUniversal(data: unknown[], declaration?: Node): UniversalQuantifier {
    const uq = Object.create(UniversalQuantifier.prototype) as UniversalQuantifier;
    (uq as unknown as { _data: unknown[] })._data = data;
    (uq as unknown as { _declaration: Node })._declaration = declaration ?? uq;
    (uq as any)._binding = null;
    (uq as any)._hashCodeCached = false;
    (uq as any)._hashCode = 0;
    (uq as any)._nrOfUnbound = -1;
    return uq;
  }

  protected override struct(data: unknown[], declaration?: Node): UniversalQuantifier {
    return UniversalQuantifier.fromDataUniversal(data, declaration ?? this.declaration());
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.UniversalQuantifier#resolve(InferContext, InferResult)
  protected override resolveWithResult(_context: InferContext, predResult: InferResult): InferResult {
    const localVars = this.localVars();
    let facts = Set<Predicate>();
    let falsehoods = Set<Predicate>();
    let completeFacts = true;
    let completeFalsehoods = true;

    for (const predFalsehood of predResult.falsehoods()) {
      const binding = predFalsehood.getBinding();
      const cleanBinding = binding !== null ? binding.deleteAll(localVars) : binding;
      const falsehood = this.setBinding(cleanBinding!);
      if (falsehood.isFullyBound()) {
        falsehoods = falsehoods.add(falsehood);
      } else {
        completeFalsehoods = false;
      }
    }

    for (const predFact of predResult.facts()) {
      const binding = predFact.getBinding();
      const cleanBinding = binding !== null ? binding.deleteAll(localVars) : binding;
      const fact = this.setBinding(cleanBinding!);
      if (!falsehoods.contains(fact)) {
        if (fact.isFullyBound()) {
          facts = facts.add(fact);
        } else {
          completeFacts = false;
        }
      }
    }

    if (!this.isFullyBound()) {
      if (!predResult.completeFacts()) {
        completeFacts = false;
      }
      if (!predResult.completeFalsehoods()) {
        completeFalsehoods = false;
      }
    } else if (falsehoods.isEmpty() && facts.isEmpty()) {
      facts = facts.add(this as Predicate);
    }

    return InferResult.of(facts, completeFacts, falsehoods, completeFalsehoods, predResult.cycles());
  }
}
