/**
 * And - logical AND predicate.
 * @JAVA_REF org.modelingvalue.nelumbo.logic.And
 */

import { List } from 'immutable';
import type { AstElement } from '../AstElement';
import { Node } from '../Node';
import type { Functor } from '../patterns/Functor';
import { BinaryPredicate } from './BinaryPredicate';
import { Predicate } from './Predicate';
import { InferResult } from '../InferResult';

/**
 * And - logical conjunction.
 * @JAVA_REF org.modelingvalue.nelumbo.logic.And
 */
export class And extends BinaryPredicate {
  // @JAVA_REF org.modelingvalue.nelumbo.logic.And#FUNCTOR
  static FUNCTOR: Functor | null = null;

  constructor(functor: Functor, elements: List<AstElement>, left: Node, right: Node) {
    super(functor, elements, left, right);
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.And#of(Predicate, Predicate)
  static of(predicate1: Predicate, predicate2: Predicate): And {
    return new And(And.FUNCTOR!, List<AstElement>(), predicate1, predicate2);
  }

  protected static fromDataAnd(data: unknown[], declaration?: Node): And {
    const and = Object.create(And.prototype) as And;
    (and as unknown as { _data: unknown[] })._data = data;
    (and as unknown as { _declaration: Node })._declaration = declaration ?? and;
    (and as any)._binding = null;
    (and as any)._hashCodeCached = false;
    (and as any)._hashCode = 0;
    (and as any)._nrOfUnbound = -1;
    return and;
  }

  protected override struct(data: unknown[], declaration?: Node): And {
    return And.fromDataAnd(data, declaration ?? this.declaration());
  }

  override declaration(): And {
    return super.declaration() as And;
  }

  override set(i: number, ...a: unknown[]): And {
    return super.set(i, ...a) as And;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.And#isTrue(InferResult, int)
  protected isTrueSingle(_predResult: InferResult, _i: number): boolean {
    return false;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.And#isFalse(InferResult, int)
  protected isFalseSingle(predResult: InferResult, _i: number): boolean {
    return predResult.isFalseCC();
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.And#isUnknown(InferResult, int)
  protected isUnknownSingle(_predResult: InferResult, _i: number): boolean {
    return false;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.And#isTrue(InferResult[])
  protected isTrueBoth(predResult: InferResult[]): boolean {
    return predResult[0].isTrueCC() && predResult[1].isTrueCC();
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.And#isFalse(InferResult[])
  protected isFalseBoth(_predResult: InferResult[]): boolean {
    return false;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.And#isLeft(InferResult[])
  protected isLeft(predResult: InferResult[]): boolean {
    return predResult[1].isTrueCC();
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.And#isRight(InferResult[])
  protected isRight(predResult: InferResult[]): boolean {
    return predResult[0].isTrueCC();
  }
}
