/**
 * CompoundPredicate - base for compound predicates with iterative constraint solving.
 * @JAVA_REF org.modelingvalue.nelumbo.logic.CompoundPredicate
 */

import { List, Map, Set } from 'immutable';
import type { AstElement } from '../AstElement';
import { Type } from '../Type';
import { Variable } from '../Variable';
import { Node } from '../Node';
import type { Functor } from '../patterns/Functor';
import { Predicate } from './Predicate';
import { InferResult } from '../InferResult';
import type { InferContext } from '../InferContext';

/**
 * CompoundPredicate - a predicate containing sub-predicates.
 */
export class CompoundPredicate extends Predicate {
  constructor(functor: Functor, elements: List<AstElement>, ...args: unknown[]) {
    super(functor, elements, ...args);
  }

  protected static fromDataCompound(data: unknown[], declaration?: Node): CompoundPredicate {
    const pred = Object.create(CompoundPredicate.prototype) as CompoundPredicate;
    (pred as unknown as { _data: unknown[] })._data = data;
    (pred as unknown as { _declaration: Node })._declaration = declaration ?? pred;
    return pred;
  }

  protected override struct(data: unknown[], declaration?: Node): CompoundPredicate {
    return CompoundPredicate.fromDataCompound(data, declaration ?? this.declaration());
  }

  /**
   * Get sub-predicates from this compound predicate.
   */
  predicates(): List<Predicate> {
    const result: Predicate[] = [];
    for (let i = 0; i < this.length(); i++) {
      const val = this.get(i);
      if (val instanceof Predicate) {
        result.push(val);
      } else if (val instanceof Node) {
        const pred = Predicate.predicate(val);
        if (pred !== null) {
          result.push(pred);
        }
      }
    }
    return List(result);
  }

  /**
   * Resolve this compound predicate.
   */
  override resolve(context: InferContext): InferResult {
    // Get all predicates and local variables
    const allPreds = this.allPredicates();
    const localVars = this.allLocalVars();

    // Shallow context for initial resolution
    const shallowContext = context.withShallow(true);
    const reduceContext = context.withReduce(true);

    // Try to resolve iteratively
    let result = this.resolveIteration(allPreds, localVars, shallowContext, context, reduceContext);

    return result;
  }

  /**
   * Get all predicates including nested ones.
   */
  protected allPredicates(): List<Predicate> {
    let preds = List<Predicate>();
    for (let i = 0; i < this.length(); i++) {
      preds = this.collectPredicates(this.get(i), preds);
    }
    return preds;
  }

  private collectPredicates(val: unknown, preds: List<Predicate>): List<Predicate> {
    if (val instanceof Predicate) {
      return preds.push(val);
    }
    if (val instanceof Node) {
      const pred = Predicate.predicate(val);
      if (pred !== null) {
        return preds.push(pred);
      }
    }
    if (List.isList(val)) {
      for (const e of val as List<unknown>) {
        preds = this.collectPredicates(e, preds);
      }
    }
    return preds;
  }

  /**
   * Iteratively resolve predicates with constraint propagation.
   */
  protected resolveIteration(
    predicates: List<Predicate>,
    localVars: Set<Variable>,
    shallowContext: InferContext,
    deepContext: InferContext,
    reduceContext: InferContext
  ): InferResult {
    let result = InferResult.unknown(this);
    let bindings = Map<Variable, unknown>();
    let changed = true;
    let iterations = 0;
    const maxIterations = 100;

    while (changed && iterations < maxIterations) {
      changed = false;
      iterations++;

      for (const pred of predicates) {
        // Apply current bindings
        const boundPred = pred.setVariables(bindings);

        // Try shallow resolution first
        let predResult = boundPred.resolve(shallowContext);

        // If shallow didn't help, try reduce mode
        if (predResult.isUnresolvable()) {
          predResult = boundPred.resolve(reduceContext);
        }

        // If still unresolvable, try deep
        if (predResult.isUnresolvable()) {
          predResult = boundPred.resolve(deepContext);
        }

        if (predResult.hasStackOverflow()) {
          return predResult;
        }

        // Update bindings from facts
        for (const fact of predResult.facts()) {
          const factBinding = fact.getBinding();
          if (factBinding !== null) {
            const newBindings = factBinding.filter((val, key) =>
              !localVars.contains(key) && !(val instanceof Type)
            );
            if (!newBindings.equals(bindings.filter((_, k) => newBindings.has(k)))) {
              bindings = bindings.merge(newBindings);
              changed = true;
            }
          }
        }

        // Combine results
        result = this.combineResult(result, predResult);

        // Short-circuit on failure
        if (this.shouldShortCircuit(predResult)) {
          return this.shortCircuitResult(result, predResult);
        }
      }
    }

    // Remove local variables from result
    return this.removeLocalVars(result, localVars);
  }

  /**
   * Combine this predicate's result with a sub-predicate's result.
   * Override in And/Or to provide specific combination logic.
   */
  protected combineResult(current: InferResult, subResult: InferResult): InferResult {
    return current.add(subResult);
  }

  /**
   * Check if we should short-circuit based on a result.
   * Override in And/Or for specific behavior.
   */
  protected shouldShortCircuit(_result: InferResult): boolean {
    return false;
  }

  /**
   * Create the short-circuit result.
   * Override in And/Or for specific behavior.
   */
  protected shortCircuitResult(current: InferResult, _trigger: InferResult): InferResult {
    return current;
  }

  /**
   * Remove local variables from result bindings.
   */
  protected removeLocalVars(result: InferResult, localVars: Set<Variable>): InferResult {
    if (localVars.isEmpty()) {
      return result;
    }

    const newFacts = result.facts().map(fact => {
      const binding = fact.getBinding();
      if (binding === null) return fact;
      const filtered = binding.filter((_, key) => !localVars.contains(key));
      return fact.setVariables(filtered);
    }).toSet();

    const newFalsehoods = result.falsehoods().map(falsehood => {
      const binding = falsehood.getBinding();
      if (binding === null) return falsehood;
      const filtered = binding.filter((_, key) => !localVars.contains(key));
      return falsehood.setVariables(filtered);
    }).toSet();

    return InferResult.of(
      newFacts,
      result.completeFacts(),
      newFalsehoods,
      result.completeFalsehoods(),
      result.cycles()
    );
  }
}
