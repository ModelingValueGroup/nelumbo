/**
 * BinaryPredicate - abstract base for binary logical operators.
 * Ported from Java: org.modelingvalue.nelumbo.logic.BinaryPredicate
 */

import { List } from 'immutable';
import type { AstElement } from '../core/AstElement';
import { Node } from '../core/Node';
import type { Functor } from '../patterns/Functor';
import { Predicate } from './Predicate';
import { CompoundPredicate } from './CompoundPredicate';
import { InferResult } from './InferResult';
import type { InferContext } from './InferContext';

/**
 * BinaryPredicate - a predicate with two operands.
 */
export abstract class BinaryPredicate extends CompoundPredicate {
  constructor(functor: Functor, elements: List<AstElement>, left: Node, right: Node) {
    super(functor, elements, left, right);
  }

  /**
   * Get the left operand.
   */
  left(): Predicate {
    return Predicate.predicate(this.get(0) as Node);
  }

  /**
   * Get the right operand.
   */
  right(): Predicate {
    return Predicate.predicate(this.get(1) as Node);
  }

  /**
   * Check if sub-result indicates true.
   */
  protected abstract resultIsTrue(predResult: InferResult): boolean;

  /**
   * Check if sub-result indicates false.
   */
  protected abstract resultIsFalse(predResult: InferResult): boolean;

  /**
   * Check if sub-result is unknown.
   */
  protected abstract resultIsUnknown(predResult: InferResult): boolean;

  override resolve(context: InferContext): InferResult {
    const left = this.left();
    const right = this.right();

    // Order predicates for optimal evaluation
    const [first, second] = this.order(left, right);

    // Resolve first predicate
    const firstResult = first.resolve(context);
    if (firstResult.hasStackOverflow()) {
      return firstResult;
    }

    // Check for short-circuit
    if (this.canShortCircuit(firstResult)) {
      return this.shortCircuitValue(this, firstResult);
    }

    // Resolve second predicate with bindings from first
    const secondWithBindings = this.applyBindings(second, firstResult);
    const secondResult = secondWithBindings.resolve(context);
    if (secondResult.hasStackOverflow()) {
      return secondResult;
    }

    // Combine results
    return this.combineResults(this, firstResult, secondResult, context);
  }

  /**
   * Order predicates for evaluation (more constrained first).
   */
  protected order(left: Predicate, right: Predicate): [Predicate, Predicate] {
    const leftUnbound = this.countUnbound(left);
    const rightUnbound = this.countUnbound(right);
    if (rightUnbound < leftUnbound) {
      return [right, left];
    }
    return [left, right];
  }

  private countUnbound(pred: Predicate): number {
    const binding = pred.getBinding();
    if (binding === null) return 0;
    let count = 0;
    binding.forEach((val) => {
      if (val instanceof Object && 'isLiteral' in val) {
        count++;
      }
    });
    return count;
  }

  /**
   * Check if we can short-circuit based on first result.
   */
  protected abstract canShortCircuit(result: InferResult): boolean;

  /**
   * Get the short-circuit result.
   */
  protected abstract shortCircuitValue(pred: Predicate, result: InferResult): InferResult;

  /**
   * Apply bindings from a result to a predicate.
   */
  protected applyBindings(pred: Predicate, result: InferResult): Predicate {
    // Get bindings from facts in the result
    for (const fact of result.facts()) {
      const binding = fact.getBinding();
      if (binding !== null && !binding.isEmpty()) {
        return pred.setVariables(binding);
      }
    }
    return pred;
  }

  /**
   * Combine two sub-results into the final result.
   */
  protected abstract combineResults(
    pred: Predicate,
    firstResult: InferResult,
    secondResult: InferResult,
    context: InferContext
  ): InferResult;
}
