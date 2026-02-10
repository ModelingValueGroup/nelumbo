/**
 * Predicate - base predicate with resolve, infer, fixpoint.
 * Ported from Java: org.modelingvalue.nelumbo.logic.Predicate
 */

import { List, Map, Set } from 'immutable';
import type { AstElement } from '../AstElement';
import { Type } from '../Type';
import { Variable } from '../Variable';
import { Node } from '../Node';
import type { Functor } from '../patterns/Functor';
import { InferResult } from '../InferResult';
import type { InferContext } from '../InferContext';

const MAX_LOGIC_DEPTH = 32;

/**
 * Predicate - a logical predicate that can be inferred.
 */
export class Predicate extends Node {
  private _nrOfUnbound: number = -1;

  constructor(functor: Functor, elements: List<AstElement>, ...args: unknown[]) {
    super(functor, elements, ...args);
  }

  protected static fromDataPredicate(data: unknown[], declaration?: Node): Predicate {
    const pred = Object.create(Predicate.prototype) as Predicate;
    (pred as unknown as { _data: unknown[] })._data = data;
    (pred as unknown as { _declaration: Node })._declaration = declaration ?? pred;
    return pred;
  }

  protected struct(data: unknown[], declaration?: Node): Predicate {
    return Predicate.fromDataPredicate(data, declaration ?? this.declaration());
  }

  override declaration(): Predicate {
    return super.declaration() as Predicate;
  }

  /**
   * Count number of unbound variables.
   */
  protected nrOfUnbound(): number {
    if (this._nrOfUnbound < 0) {
      this._nrOfUnbound = this.countNrOfUnbound();
    }
    return this._nrOfUnbound;
  }

  protected countNrOfUnbound(): number {
    let count = 0;
    const binding = this.getBinding();
    if (binding !== null) {
      binding.forEach((val) => {
        if (val instanceof Type) {
          count++;
        }
      });
    }
    return count;
  }

  /**
   * Convert a node to a predicate.
   */
  static predicate(node: Node | null): Predicate {
    if (node === null) {
      return null as unknown as Predicate;
    }
    if (node instanceof Predicate) {
      return node;
    }
    if (node instanceof Variable) {
      // Create a boolean predicate from variable
      return new Predicate(null as unknown as Functor, List([node as AstElement]), node);
    }
    throw new Error('Must be Variable or Predicate, is: ' + node);
  }

  /**
   * Cast from another predicate.
   */
  castFrom(from: Predicate): Predicate {
    const data = [...(from as unknown as { _data: unknown[] })._data];
    data[0] = this.functor();
    return from.struct(data, this.declaration());
  }

  /**
   * Create a fact result.
   */
  factCC(): InferResult {
    return InferResult.factsCC(this.singleton());
  }

  /**
   * Create a falsehood result.
   */
  falsehoodCC(): InferResult {
    return InferResult.falsehoodsCC(this.singleton());
  }

  /**
   * Create a fact with incomplete falsehoods.
   */
  factCI(): InferResult {
    return InferResult.factsCI(this.singleton());
  }

  /**
   * Create an unknown result.
   */
  unknown(): InferResult {
    return InferResult.unknown(this);
  }

  /**
   * Create an unresolvable result.
   */
  unresolvable(): InferResult {
    return InferResult.unresolvable(this);
  }

  /**
   * Create a falsehood result with incomplete facts.
   */
  falsehoodCI(): InferResult {
    return InferResult.falsehoodsIC(this.singleton());
  }

  /**
   * Create a falsehoods result with incomplete inference.
   */
  falsehoodsII(): InferResult {
    return InferResult.falsehoodsII(this.singleton());
  }

  /**
   * Create a singleton set of this predicate.
   */
  singleton(): Set<Predicate> {
    return Set([this]);
  }

  /**
   * Check if fully bound (no variables).
   */
  isFullyBound(): boolean {
    return this.nrOfUnbound() === 0;
  }

  /**
   * Check if this predicate is false (for NBoolean override).
   */
  isFalse(): boolean {
    return false;
  }

  /**
   * Check if this is a relation (fact type).
   */
  isRelation(): boolean {
    return Type.FACT_TYPE.isAssignableFrom(this.type());
  }

  /**
   * Get the type at an index.
   */
  getType(i: number): Type {
    const val = this.get(i);
    if (val instanceof Type) {
      return val;
    }
    if (val instanceof Variable) {
      return val.type();
    }
    if (val instanceof Node) {
      return val.type();
    }
    return Type.OBJECT;
  }

  /**
   * Set the type at an index.
   */
  override setType(i: number, type: Type): Predicate {
    return super.setType(i, type) as Predicate;
  }

  /**
   * Set variables from a map.
   */
  setVariables(vars: Map<Variable, unknown>): Predicate {
    return this.setBinding(vars) as Predicate;
  }

  /**
   * Create literal variables from a map.
   */
  static literals(vars: Map<Variable, unknown>): Map<Variable, unknown> {
    return vars.map((val, key) => {
      if (val instanceof Type) {
        return key.literal();
      }
      return val;
    });
  }

  override setBinding(vars: Map<Variable, unknown>): Predicate {
    return super.setBinding(vars) as Predicate;
  }

  override setFunctor(functor: Functor): Predicate {
    return super.setFunctor(functor) as Predicate;
  }

  override resetDeclaration(): Predicate {
    return super.resetDeclaration() as Predicate;
  }

  override set(i: number, ...a: unknown[]): Predicate {
    return super.set(i, ...a) as Predicate;
  }

  /**
   * Infer this predicate.
   */
  infer(context: InferContext): InferResult {
    const nrOfUnbound = this.nrOfUnbound();
    return this.inferInternal(nrOfUnbound, context);
  }

  protected inferInternal(nrOfUnbound: number, context: InferContext): InferResult {
    const functor = this.functor();
    if (nrOfUnbound > 1 || (nrOfUnbound === 1 && functor !== null && functor.argTypes().size === 1)) {
      return this.unresolvable();
    }
    const knowledgeBase = context.knowledgeBase();
    if (this.isRelation()) {
      return knowledgeBase.getFacts(this, context);
    } else {
      const memoiz = knowledgeBase.getMemoiz(this);
      if (memoiz !== null) {
        return memoiz;
      }
      const cycleResult = context.getCycleResult(this);
      if (cycleResult !== null) {
        return cycleResult;
      }
      const stack = context.stack();
      if (stack.size >= MAX_LOGIC_DEPTH) {
        return InferResult.overflow(stack.push(this));
      }
      const result = this.fixpoint(context.pushOnStack(this));
      knowledgeBase.memoization(this, result);
      return result;
    }
  }

  private fixpoint(context: InferContext): InferResult {
    let previousResult: InferResult | null = null;
    let cycleResult = InferResult.cycle(Set(), Set(), this);
    let nextResult: InferResult;

    do {
      nextResult = this.inferRules(context.putCycleResult(this, cycleResult));
      if (nextResult.hasStackOverflow()) {
        return nextResult;
      }
      if (nextResult.hasCycleWith(this)) {
        if (previousResult === null || !nextResult.equals(previousResult)) {
          previousResult = nextResult;
          cycleResult = InferResult.cycle(nextResult.facts(), nextResult.falsehoods(), this);
          context.knowledgeBase().memoization(this, cycleResult);
          continue;
        } else {
          cycleResult = InferResult.of(nextResult.facts(), true, nextResult.falsehoods(), true, nextResult.cycles().remove(this));
          context.knowledgeBase().memoization(this, cycleResult);
          nextResult = this.inferRules(context.putCycleResult(this, cycleResult));
          if (nextResult.hasStackOverflow()) {
            return nextResult;
          }
          return InferResult.of(
            nextResult.facts(),
            nextResult.completeFacts(),
            nextResult.falsehoods(),
            nextResult.completeFalsehoods(),
            nextResult.cycles().remove(this)
          );
        }
      }
      return nextResult;
    } while (true);
  }

  private inferRules(context: InferContext): InferResult {
    let result = this.unknown();
    const rules = context.knowledgeBase().getRules(this);
    for (const rule of rules) {
      result = rule.biimply(this, context, result);
      if (result.hasStackOverflow()) {
        return result;
      }
    }
    return result;
  }

  /**
   * Resolve this predicate.
   */
  resolve(context: InferContext): InferResult {
    return this.infer(context);
  }
}

// Add methods to KnowledgeBase interface
declare module '../KnowledgeBase' {
  interface KnowledgeBase {
    getFacts(predicate: Predicate, context: InferContext): InferResult;
    getMemoiz(predicate: Predicate): InferResult | null;
    memoization(predicate: Predicate, result: InferResult): void;
    getRules(predicate: Predicate): Set<import('../Rule').Rule>;
    createInferContext(): InferContext;
  }
}
