/**
 * Predicate - base predicate with resolve, infer, fixpoint.
 * @JAVA_REF org.modelingvalue.nelumbo.logic.Predicate
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
 * @JAVA_REF org.modelingvalue.nelumbo.logic.Predicate
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
    (pred as any)._binding = null;
    (pred as any)._hashCodeCached = false;
    (pred as any)._hashCode = 0;
    (pred as any)._nrOfUnbound = -1;
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
   * @JAVA_REF org.modelingvalue.nelumbo.logic.Predicate#nrOfUnbound()
   */
  protected nrOfUnbound(): number {
    if (this._nrOfUnbound < 0) {
      this._nrOfUnbound = this.countNrOfUnbound();
    }
    return this._nrOfUnbound;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.Predicate#countNrOfUnbound()
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

  // Factory hook for creating NBoolean from Variable (avoids circular import)
  // @JAVA_REF org.modelingvalue.nelumbo.logic.Predicate#predicate(Node)
  static _booleanFromVariable: ((v: Variable) => Predicate) | null = null;

  // Late-bound reference to equalsFunctor (avoids circular import with KnowledgeBase)
  // @JAVA_REF org.modelingvalue.nelumbo.logic.Predicate#isShallow uses KnowledgeBase.equalsFunctor()
  static _equalsFunctor: Functor | null = null;

  // Late-bound reference to KnowledgeBase.literal() for setVariables (avoids circular import)
  // @JAVA_REF org.modelingvalue.nelumbo.logic.Predicate#setVariables uses KnowledgeBase.CURRENT.literal()
  static _literalFn: ((functor: Functor) => Functor | null) | null = null;

  /**
   * Convert a node to a predicate.
   * @JAVA_REF org.modelingvalue.nelumbo.logic.Predicate#predicate(Node)
   */
  static predicate(node: Node | null): Predicate {
    if (node instanceof Predicate) {
      return node;
    }
    if (node === null) {
      return null as unknown as Predicate;
    }
    if (node instanceof Variable) {
      // Java: return new NBoolean(var)
      if (Predicate._booleanFromVariable !== null) {
        return Predicate._booleanFromVariable(node);
      }
      // Fallback before NBoolean is loaded
      return new Predicate(null as unknown as Functor, List([node as AstElement]), node);
    }
    throw new Error('Must be Variable or Predicate, is: ' + node);
  }

  /**
   * Cast from another predicate.
   * @JAVA_REF org.modelingvalue.nelumbo.logic.Predicate#castFrom(Predicate)
   */
  castFrom(from: Predicate): Predicate {
    const data = [...(from as unknown as { _data: unknown[] })._data];
    data[0] = this.functor();
    return from.struct(data, this.declaration());
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.Predicate#factCC()
  factCC(): InferResult {
    return InferResult.factsCC(this.singleton());
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.Predicate#falsehoodCC()
  falsehoodCC(): InferResult {
    return InferResult.falsehoodsCC(this.singleton());
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.Predicate#factCI()
  factCI(): InferResult {
    return InferResult.factsCI(this.singleton());
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.Predicate#unknown()
  unknown(): InferResult {
    return InferResult.unknown(this);
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.Predicate#unresolvable()
  unresolvable(): InferResult {
    return InferResult.unresolvable(this);
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.Predicate#falsehoodIC()
  falsehoodCI(): InferResult {
    return InferResult.falsehoodsIC(this.singleton());
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.Predicate#falsehoodsII()
  falsehoodsII(): InferResult {
    return InferResult.falsehoodsII(this.singleton());
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.Predicate#singleton()
  singleton(): Set<Predicate> {
    return Set([this]);
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.Predicate#isFullyBound()
  isFullyBound(): boolean {
    return this.nrOfUnbound() === 0;
  }

  /**
   * Check if this predicate is false (for NBoolean override).
   */
  isFalse(): boolean {
    return false;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.Predicate#isFact()
  isFact(): boolean {
    return Type.FACT_TYPE.isAssignableFrom(this.type());
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.Predicate#getType(int)
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

  override setType(i: number, type: Type): Predicate {
    return super.setType(i, type) as Predicate;
  }

  /**
   * Set variables from a map, then reset declaration.
   * @JAVA_REF org.modelingvalue.nelumbo.logic.Predicate#setVariables(Map)
   */
  setVariables(vars: Map<Variable, unknown>): Predicate {
    const predicate = this.setBinding(vars) as Predicate;
    if (predicate === this) {
      return this;
    }
    // @JAVA_REF org.modelingvalue.nelumbo.logic.Predicate#setVariables(Map)
    // Replace non-literal functors with literal functors when all args are literal
    const literalFn = Predicate._literalFn;
    if (literalFn !== null) {
      const replaced = predicate.replace((n: Node) => {
        const functor = n.functor();
        if (functor !== null) {
          const lit = literalFn(functor);
          if (lit !== null) {
            const args = n.args();
            // Note: Java Variable extends Node, but TS Variable does not extend Node
            if (args.every((a: unknown) => (a instanceof Node || a instanceof Variable) && (a as Node | Variable).type().isLiteral())) {
              return lit.construct(n.astElements(), args.toArray());
            }
          }
        }
        return n;
      }) as Predicate;
      return replaced.resetDeclaration();
    }
    return predicate.resetDeclaration();
  }

  /**
   * Create literal variables from a map.
   * @JAVA_REF org.modelingvalue.nelumbo.logic.Predicate#literals(Map)
   */
  static literals(vars: Map<Variable, unknown>): Map<Variable, unknown> {
    // Java: vars.replaceAll(e -> Entry.of(e.getKey(), e.getKey().literal()))
    return vars.map((_val, key) => key.literal());
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
   * Resolve this predicate - entry point for inference.
   * Override point for CompoundPredicate and Quantifier.
   * @JAVA_REF org.modelingvalue.nelumbo.logic.Predicate#resolve(InferContext)
   */
  resolve(context: InferContext): InferResult {
    return this.inferInternal(this.nrOfUnbound(), context);
  }

  /**
   * Infer with context - called from CompoundPredicate.resolve() loop.
   * Override point for BinaryPredicate, Not, NBoolean.
   * Adds reduce/shallow guard checks before calling inferInternal.
   * @JAVA_REF org.modelingvalue.nelumbo.logic.Predicate#infer(InferContext)
   */
  infer(context: InferContext): InferResult {
    const nrOfUnbound = this.nrOfUnbound();
    if (nrOfUnbound > 0 && context.reduce()) {
      return this.unresolvable();
    } else if (nrOfUnbound === 0 && context.shallow()) {
      return this.unresolvable();
    }
    return this.inferInternal(nrOfUnbound, context);
  }

  /**
   * Main inference logic.
   * @JAVA_REF org.modelingvalue.nelumbo.logic.Predicate#infer(int, InferContext)
   */
  protected inferInternal(nrOfUnbound: number, context: InferContext): InferResult {
    const functor = this.functor();
    if (nrOfUnbound > 1 || (context.shallow() && !this.isShallow(nrOfUnbound, functor)) || (nrOfUnbound === 1 && functor !== null && functor.argTypes().size === 1)) {
      return this.unresolvable();
    }
    const knowledgeBase = context.knowledgeBase();
    if ((globalThis as any).__DEBUG_RULES__ && this.toString().includes('friends')) {
      console.log(`DEBUG inferInternal: ${this} isFact=${this.isFact()} type=${this.type()} FACT_TYPE=${Type.FACT_TYPE}`);
    }
    if (this.isFact()) {
      return knowledgeBase.getFacts(this, context);
    } else {
      const memoiz = knowledgeBase.getMemoiz(this);
      if (memoiz !== null) {
        return memoiz;
      }
      const cycleResult = context.getCycleResult(this);
      if (cycleResult !== null) {
        // @JAVA_REF: return context.reduce() ? unknown() : result;
        return context.reduce() ? this.unknown() : cycleResult;
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

  /**
   * Check if shallow inference applies (for Equal with one bound literal).
   * @JAVA_REF org.modelingvalue.nelumbo.logic.Predicate#isShallow(int, Functor)
   */
  // @JAVA_REF org.modelingvalue.nelumbo.logic.Predicate#isShallow(int, Functor)
  private isShallow(nrOfUnbound: number, functor: Functor | null): boolean {
    if (nrOfUnbound === 1 && Predicate._equalsFunctor !== null && Predicate._equalsFunctor.equals(functor)) {
      const a = this.getVal(0);
      const b = this.getVal(1);
      return (b === null && a !== null && a instanceof Node && Type.LITERAL.isAssignableFrom(a.type())) ||
             (a === null && b !== null && b instanceof Node && Type.LITERAL.isAssignableFrom(b.type()));
    }
    return false;
  }

  private fixpoint(context: InferContext): InferResult {
    let previousResult: InferResult | null = null;
    let cycleResult = InferResult.cycle(Set(), Set(), this);
    let nextResult: InferResult;
    let _debugFixIter = 0;

    do {
      _debugFixIter++;
      if (_debugFixIter > 100) {
        throw new Error(`Fixpoint loop exceeded 100 iterations for: ${this}`);
      }
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
    if ((globalThis as any).__DEBUG_RULES__) {
      console.log(`DEBUG inferRules: ${this} functor=${this.functor()} rules.size=${rules.size}`);
    }
    for (const rule of rules) {
      if ((globalThis as any).__DEBUG_RULES__) {
        console.log(`DEBUG inferRules: applying rule ${rule} to ${this}`);
      }
      result = rule.biimply(this, context, result);
      if ((globalThis as any).__DEBUG_RULES__) {
        console.log(`DEBUG inferRules: result after rule = ${result}`);
      }
      if (result.hasStackOverflow()) {
        return result;
      }
    }
    return result;
  }

  /**
   * Replace a sub-predicate with another.
   * @JAVA_REF org.modelingvalue.nelumbo.logic.Predicate#replace(Predicate, Predicate)
   */
  replacePredicate(from: Predicate, to: Predicate): Predicate {
    if (this.equals(from)) {
      return to;
    }
    let decl = this.declaration();
    let array: unknown[] | null = null;
    for (let i = 0; i < this.length(); i++) {
      const thisVal = this.get(i);
      if (thisVal instanceof Predicate) {
        const fromDecl = thisVal as Predicate;
        const toDecl = fromDecl.replacePredicate(from, to);
        if (toDecl !== fromDecl) {
          decl = decl.set(i, toDecl.declaration());
          if (array === null) {
            array = [...this._data];
          }
          array[i + Node.START] = toDecl;
        }
      }
    }
    return array !== null ? this.struct(array, decl) : this;
  }

  /**
   * Get the sub-predicate at index i, handling Type indirection through declaration.
   * @JAVA_REF org.modelingvalue.nelumbo.logic.CompoundPredicate#predicate(int)
   */
  predicateAt(i: number): Predicate {
    let node = this.get(i) as Node;
    if ((globalThis as any).__DEBUG_RULES__ && String(this).includes('friends')) {
      console.log(`DEBUG predicateAt(${i}): this=${this} this.type()=${this.type()} node=${node} nodeIsType=${node instanceof Type} nodeType=${node instanceof Node ? node.type() : 'N/A'}`);
      if (node instanceof Type) {
        const declNode = this.declaration().get(i) as Node;
        console.log(`DEBUG predicateAt(${i}): using declaration: declNode=${declNode} declNodeType=${declNode instanceof Node ? declNode.type() : 'N/A'}`);
      }
    }
    if (node instanceof Type) {
      node = this.declaration().get(i) as Node;
    }
    return Predicate.predicate(node);
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
