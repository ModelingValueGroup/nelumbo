/**
 * CompoundPredicate - base for compound predicates with iterative constraint solving.
 * @JAVA_REF org.modelingvalue.nelumbo.logic.CompoundPredicate
 */

import { List, Map, Set } from 'immutable';
import type { AstElement } from '../AstElement';
import { Variable } from '../Variable';
import { Node } from '../Node';
import type { Functor } from '../patterns/Functor';
import { Predicate } from './Predicate';
import { InferResult } from '../InferResult';
import type { InferContext } from '../InferContext';

// Late-bound reference to NBoolean (set from NBoolean.ts to avoid circular import)
let _NBooleanTRUE: (() => Predicate) | null = null;
let _NBooleanFALSE: (() => Predicate) | null = null;

export function _setNBooleanRefs(getTRUE: () => Predicate, getFALSE: () => Predicate): void {
  _NBooleanTRUE = getTRUE;
  _NBooleanFALSE = getFALSE;
}

/**
 * CompoundPredicate - a predicate containing sub-predicates.
 * @JAVA_REF org.modelingvalue.nelumbo.logic.CompoundPredicate
 */
export class CompoundPredicate extends Predicate {
  constructor(functor: Functor, elements: List<AstElement>, ...args: unknown[]) {
    super(functor, elements, ...args);
  }

  protected static fromDataCompound(data: unknown[], declaration?: Node): CompoundPredicate {
    const pred = Object.create(CompoundPredicate.prototype) as CompoundPredicate;
    (pred as unknown as { _data: unknown[] })._data = data;
    (pred as unknown as { _declaration: Node })._declaration = declaration ?? pred;
    (pred as any)._binding = null;
    (pred as any)._hashCodeCached = false;
    (pred as any)._hashCode = 0;
    (pred as any)._nrOfUnbound = -1;
    return pred;
  }

  protected override struct(data: unknown[], declaration?: Node): CompoundPredicate {
    return CompoundPredicate.fromDataCompound(data, declaration ?? this.declaration());
  }

  /**
   * Resolve this compound predicate.
   * Uses iterative shallow → reduce → deep inference with NBoolean.TRUE/FALSE replacement.
   * @JAVA_REF org.modelingvalue.nelumbo.logic.CompoundPredicate#resolve(InferContext)
   */
  override resolve(context: InferContext): InferResult {
    type BindingKey = Map<Variable, unknown>;
    let now: Map<BindingKey, Predicate>;
    let next: Map<BindingKey, Predicate> = Map<BindingKey, Predicate>().set(this.getBinding() ?? Map(), this as Predicate);
    let facts = Set<Predicate>();
    let falsehoods = Set<Predicate>();
    let cycles = Set<Predicate>();
    let completeFacts = true;
    let completeFalsehoods = true;
    const deep = context;
    const shallow = deep.toShallow();
    const reduce = deep.toReduce();

    do {
      now = next;
      next = Map<BindingKey, Predicate>();

      for (const [binding, predicate] of now.entries()) {
        // Phase 1: Shallow inference
        let result = predicate.infer(shallow);
        if (result.hasStackOverflow()) {
          return result;
        }
        if (!result.unresolvable()) {
          for (const pred of result.allFacts()) {
            let b = pred.getBinding();
            if (b !== null && !b.isEmpty()) {
              b = binding.merge(b);
              next = next.set(b, predicate.setBinding(b).replacePredicate(pred, _NBooleanTRUE!() as Predicate));
            }
          }
          for (const pred of result.allFalsehoods()) {
            let b = pred.getBinding();
            if (b !== null && !b.isEmpty()) {
              b = binding.merge(b);
              next = next.set(b, predicate.setBinding(b).replacePredicate(pred, _NBooleanFALSE!() as Predicate));
            }
          }
          completeFacts = completeFacts && result.completeFacts();
          completeFalsehoods = completeFalsehoods && result.completeFalsehoods();
          cycles = cycles.union(result.cycles());
        }

        // Phase 2: Reduce inference
        result = predicate.infer(reduce);
        if (result.hasStackOverflow()) {
          return result;
        } else if (result.isFalseCC()) {
          falsehoods = falsehoods.add(this.setBinding(binding));
        } else if (result.isTrueCC()) {
          facts = facts.add(this.setBinding(binding));
        } else {
          // Phase 3: Deep inference
          const resultPredicate = result.predicateOf();
          if (resultPredicate !== null) {
            result = resultPredicate.infer(deep);
            if (result.hasStackOverflow()) {
              return result;
            }
            if (!result.unresolvable()) {
              for (const pred of result.allFacts()) {
                let b = pred.getBinding();
                if (b !== null && !b.isEmpty()) {
                  b = binding.merge(b);
                  next = next.set(b, resultPredicate.setBinding(b).replacePredicate(pred, _NBooleanTRUE!() as Predicate));
                }
              }
              for (const pred of result.allFalsehoods()) {
                let b = pred.getBinding();
                if (b !== null && !b.isEmpty()) {
                  b = binding.merge(b);
                  next = next.set(b, resultPredicate.setBinding(b).replacePredicate(pred, _NBooleanFALSE!() as Predicate));
                }
              }
              completeFacts = completeFacts && result.completeFacts();
              completeFalsehoods = completeFalsehoods && result.completeFalsehoods();
              cycles = cycles.union(result.cycles());
            }
          }
        }
      }
    } while (!next.isEmpty());

    // @JAVA_REF: if no results found and both complete, set both to incomplete
    if (facts.isEmpty() && completeFacts && falsehoods.isEmpty() && completeFalsehoods) {
      completeFacts = false;
      completeFalsehoods = false;
    }

    return InferResult.of(facts, completeFacts, falsehoods, completeFalsehoods, cycles);
  }
}
