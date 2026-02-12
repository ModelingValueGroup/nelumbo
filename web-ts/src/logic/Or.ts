/**
 * Or - logical OR predicate.
 * @JAVA_REF org.modelingvalue.nelumbo.logic.Or
 */

import { List } from 'immutable';
import type { AstElement } from '../AstElement';
import { Node } from '../Node';
import type { Functor } from '../patterns/Functor';
import { BinaryPredicate } from './BinaryPredicate';
import { Predicate } from './Predicate';
import { InferResult } from '../InferResult';

/**
 * Or - logical disjunction.
 * @JAVA_REF org.modelingvalue.nelumbo.logic.Or
 */
export class Or extends BinaryPredicate {
  // @JAVA_REF org.modelingvalue.nelumbo.logic.Or#FUNCTOR
  static FUNCTOR: Functor | null = null;

  constructor(functor: Functor, elements: List<AstElement>, left: Node, right: Node) {
    super(functor, elements, left, right);
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.Or#of(Predicate, Predicate)
  static of(predicate1: Predicate, predicate2: Predicate): Or {
    return new Or(Or.FUNCTOR!, List<AstElement>(), predicate1, predicate2);
  }

  protected static fromDataOr(data: unknown[], declaration?: Node): Or {
    const or = Object.create(Or.prototype) as Or;
    (or as unknown as { _data: unknown[] })._data = data;
    (or as unknown as { _declaration: Node })._declaration = declaration ?? or;
    (or as any)._binding = null;
    (or as any)._hashCodeCached = false;
    (or as any)._hashCode = 0;
    (or as any)._nrOfUnbound = -1;
    return or;
  }

  protected override struct(data: unknown[], declaration?: Node): Or {
    return Or.fromDataOr(data, declaration ?? this.declaration());
  }

  override declaration(): Or {
    return super.declaration() as Or;
  }

  override set(i: number, ...a: unknown[]): Or {
    return super.set(i, ...a) as Or;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.Or#isTrue(InferResult, int)
  protected isTrueSingle(predResult: InferResult, _i: number): boolean {
    return predResult.isTrueCC();
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.Or#isFalse(InferResult, int)
  protected isFalseSingle(_predResult: InferResult, _i: number): boolean {
    return false;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.Or#isUnknown(InferResult, int)
  protected isUnknownSingle(_predResult: InferResult, _i: number): boolean {
    return false;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.Or#isTrue(InferResult[])
  protected isTrueBoth(_predResult: InferResult[]): boolean {
    return false;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.Or#isFalse(InferResult[])
  protected isFalseBoth(predResult: InferResult[]): boolean {
    return predResult[0].isFalseCC() && predResult[1].isFalseCC();
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.Or#isLeft(InferResult[])
  protected isLeft(predResult: InferResult[]): boolean {
    return predResult[1].isFalseCC();
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.Or#isRight(InferResult[])
  protected isRight(predResult: InferResult[]): boolean {
    return predResult[0].isFalseCC();
  }
}
