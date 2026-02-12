/**
 * ExistentialQuantifier - exists quantifier.
 * @JAVA_REF org.modelingvalue.nelumbo.logic.ExistentialQuantifier
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
 * @JAVA_REF org.modelingvalue.nelumbo.logic.ExistentialQuantifier
 */
export class ExistentialQuantifier extends Quantifier {
  // @JAVA_REF org.modelingvalue.nelumbo.logic.ExistentialQuantifier#FUNCTOR
  static FUNCTOR: Functor | null = null;

  constructor(functor: Functor, elements: List<AstElement>, ...args: unknown[]) {
    super(functor, elements, ...args);
  }

  protected static fromDataExistential(data: unknown[], declaration?: Node): ExistentialQuantifier {
    const eq = Object.create(ExistentialQuantifier.prototype) as ExistentialQuantifier;
    (eq as unknown as { _data: unknown[] })._data = data;
    (eq as unknown as { _declaration: Node })._declaration = declaration ?? eq;
    (eq as any)._binding = null;
    (eq as any)._hashCodeCached = false;
    (eq as any)._hashCode = 0;
    (eq as any)._nrOfUnbound = -1;
    return eq;
  }

  protected override struct(data: unknown[], declaration?: Node): ExistentialQuantifier {
    return ExistentialQuantifier.fromDataExistential(data, declaration ?? this.declaration());
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.ExistentialQuantifier#resolve(InferContext, InferResult)
  protected override resolveWithResult(_context: InferContext, predResult: InferResult): InferResult {
    const localVars = this.localVars();
    let facts = Set<Predicate>();
    let falsehoods = Set<Predicate>();
    let completeFacts = true;
    let completeFalsehoods = true;

    for (const predFact of predResult.facts()) {
      const binding = predFact.getBinding();
      const cleanBinding = binding !== null ? binding.deleteAll(localVars) : binding;
      const fact = this.setBinding(cleanBinding!);
      if (fact.isFullyBound()) {
        facts = facts.add(fact);
      } else {
        completeFacts = false;
      }
    }

    for (const predFalsehood of predResult.falsehoods()) {
      const binding = predFalsehood.getBinding();
      const cleanBinding = binding !== null ? binding.deleteAll(localVars) : binding;
      const falsehood = this.setBinding(cleanBinding!);
      if (!facts.contains(falsehood)) {
        if (falsehood.isFullyBound()) {
          falsehoods = falsehoods.add(falsehood);
        } else {
          completeFalsehoods = false;
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
      falsehoods = falsehoods.add(this as Predicate);
    }

    return InferResult.of(facts, completeFacts, falsehoods, completeFalsehoods, predResult.cycles());
  }
}
