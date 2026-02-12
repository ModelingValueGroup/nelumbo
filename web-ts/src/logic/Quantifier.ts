/**
 * Quantifier - base class for quantified predicates.
 * @JAVA_REF org.modelingvalue.nelumbo.logic.Quantifier
 */

import { List } from 'immutable';
import type { AstElement } from '../AstElement';
import { Type } from '../Type';
import { Variable } from '../Variable';
import { Node } from '../Node';
import type { Functor } from '../patterns/Functor';
import { Predicate } from './Predicate';
import { CompoundPredicate } from './CompoundPredicate';
import { InferResult } from '../InferResult';
import type { InferContext } from '../InferContext';

/**
 * Quantifier - base for existential and universal quantification.
 * @JAVA_REF org.modelingvalue.nelumbo.logic.Quantifier
 */
export abstract class Quantifier extends CompoundPredicate {
  constructor(functor: Functor, elements: List<AstElement>, ...args: unknown[]) {
    super(functor, elements, ...args);
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.Quantifier#doGetBinding(Object, int)
  protected override doGetBinding(_varVal: unknown, i: number): boolean {
    return i > 0;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.Quantifier#doSetBinding(Object, int)
  protected override doSetBinding(varVal: unknown, i: number): boolean {
    return i > 0 || varVal instanceof Variable;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.Quantifier#localVars()
  override localVars(): List<Variable> {
    return this.get(0) as List<Variable>;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.Quantifier#predicate()
  predicateBody(): Predicate {
    return this.predicateAt(1);
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.Quantifier#countNrOfUnbound()
  protected override countNrOfUnbound(): number {
    const binding = this.getBinding();
    if (binding === null) return 0;
    const localVarsList = this.localVars();
    let count = 0;
    binding.forEach((val, key) => {
      if (val instanceof Type && !localVarsList.contains(key)) {
        count++;
      }
    });
    return count;
  }

  /**
   * Override inferInternal matching Java's Quantifier.infer(int, InferContext).
   * @JAVA_REF org.modelingvalue.nelumbo.logic.Quantifier#infer(int, InferContext)
   */
  protected override inferInternal(_nrOfUnbound: number, context: InferContext): InferResult {
    return context.shallow() ? this.unresolvable() : this.resolve(context.toDeep());
  }

  /**
   * Override resolve matching Java's Quantifier.resolve(InferContext).
   * @JAVA_REF org.modelingvalue.nelumbo.logic.Quantifier#resolve(InferContext)
   */
  override resolve(context: InferContext): InferResult {
    const predicate = this.predicateBody();
    const predResult = predicate.resolve(context);
    if (predResult.hasStackOverflow()) {
      return predResult;
    }
    return this.resolveWithResult(context, predResult);
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.Quantifier#resolve(InferContext, InferResult)
  protected abstract resolveWithResult(context: InferContext, predResult: InferResult): InferResult;
}
