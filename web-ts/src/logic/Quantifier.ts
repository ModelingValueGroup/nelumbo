/**
 * Quantifier - base class for quantified predicates.
 * @JAVA_REF org.modelingvalue.nelumbo.logic.Quantifier
 */

import { List, Set } from 'immutable';
import type { AstElement } from '../AstElement';
import { Type } from '../Type';
import { Variable } from '../Variable';
import { Node } from '../Node';
import type { Functor } from '../patterns/Functor';
import { Predicate } from './Predicate';
import { InferResult } from '../InferResult';
import type { InferContext } from '../InferContext';

/**
 * Quantifier - base for existential and universal quantification.
 */
export abstract class Quantifier extends Predicate {
  constructor(functor: Functor, elements: List<AstElement>, variables: List<Variable>, body: Node) {
    super(functor, elements, variables, body);
  }

  /**
   * Get the quantified variables.
   */
  override localVars(): List<Variable> {
    return this.get(0) as List<Variable>;
  }

  /**
   * Get the body predicate.
   */
  body(): Predicate {
    return Predicate.predicate(this.get(1) as Node);
  }

  protected override countNrOfUnbound(): number {
    // Don't count local variables
    const localVars = this.localVars().toSet();
    let count = 0;
    const binding = this.getBinding();
    if (binding !== null) {
      binding.forEach((val, key) => {
        if (val instanceof Type && !localVars.contains(key)) {
          count++;
        }
      });
    }
    return count;
  }

  /**
   * Resolve the quantifier.
   */
  override resolve(context: InferContext): InferResult {
    const body = this.body();
    const localVars = this.localVars().toSet();

    // Resolve the body
    const bodyResult = body.resolve(context);

    if (bodyResult.hasStackOverflow()) {
      return bodyResult;
    }

    return this.processResult(bodyResult, localVars, context);
  }

  /**
   * Process the body result for this quantifier type.
   * Override in ExistentialQuantifier and UniversalQuantifier.
   */
  protected abstract processResult(
    bodyResult: InferResult,
    localVars: Set<Variable>,
    context: InferContext
  ): InferResult;

  /**
   * Remove local variables from facts/falsehoods.
   */
  protected removeLocalVars(preds: Set<Predicate>, localVars: Set<Variable>): Set<Predicate> {
    return preds.map(pred => {
      const binding = pred.getBinding();
      if (binding === null) return pred;
      const filtered = binding.filter((_, key) => !localVars.contains(key));
      return pred.setVariables(filtered);
    }).filter(pred => pred.isFullyBound()).toSet();
  }

  /**
   * Check if a predicate still has unbound local variables.
   */
  protected hasUnboundLocals(pred: Predicate, localVars: Set<Variable>): boolean {
    const binding = pred.getBinding();
    if (binding === null) return false;
    return binding.some((val, key) => localVars.contains(key) && val instanceof Type);
  }
}
