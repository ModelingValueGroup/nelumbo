/**
 * When - conditional predicate (if-then).
 * @JAVA_REF org.modelingvalue.nelumbo.logic.When
 */

import { List } from 'immutable';
import type { AstElement } from '../AstElement';
import { Type } from '../Type';
import { Node } from '../Node';
import { Predicate } from './Predicate';
import { BinaryPredicate } from './BinaryPredicate';
import { InferResult } from '../InferResult';
import { TokenType } from '../syntax/TokenType';

/**
 * When - conditional logic (predicate2 if predicate1).
 * @JAVA_REF org.modelingvalue.nelumbo.logic.When
 */
export class When extends BinaryPredicate {
  constructor(when: Node, predicate: Node) {
    // Java: super(Type.BOOLEAN, List.of(), when, predicate)
    super(Type.BOOLEAN as any, List<AstElement>(), when, predicate);
  }

  protected static fromDataWhen(data: unknown[], declaration?: Node): When {
    const when = Object.create(When.prototype) as When;
    (when as unknown as { _data: unknown[] })._data = data;
    (when as unknown as { _declaration: Node })._declaration = declaration ?? when;
    (when as any)._binding = null;
    (when as any)._hashCodeCached = false;
    (when as any)._hashCode = 0;
    (when as any)._nrOfUnbound = -1;
    return when;
  }

  static of(when: Node, predicate: Node): When {
    return new When(when, predicate);
  }

  protected override struct(data: unknown[], declaration?: Node): When {
    return When.fromDataWhen(data, declaration ?? this.declaration());
  }

  override declaration(): When {
    return super.declaration() as When;
  }

  override set(i: number, ...a: unknown[]): When {
    return super.set(i, ...a) as When;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.When#isTrue(InferResult, int)
  protected isTrueSingle(_predResult: InferResult, _i: number): boolean {
    return false;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.When#isFalse(InferResult, int)
  protected isFalseSingle(_predResult: InferResult, _i: number): boolean {
    return false;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.When#isUnknown(InferResult, int)
  protected isUnknownSingle(predResult: InferResult, i: number): boolean {
    return i === 0 && predResult.isFalseCC();
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.When#isTrue(InferResult[])
  protected isTrueBoth(predResult: InferResult[]): boolean {
    return predResult[0].isTrueCC() && predResult[1].isTrueCC();
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.When#isFalse(InferResult[])
  protected isFalseBoth(predResult: InferResult[]): boolean {
    return predResult[0].isTrueCC() && predResult[1].isFalseCC();
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.When#isLeft(InferResult[])
  protected isLeft(predResult: InferResult[]): boolean {
    return predResult[1].isTrueCC();
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.When#isRight(InferResult[])
  protected isRight(predResult: InferResult[]): boolean {
    return predResult[0].isTrueCC();
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.When#resolvedOnly(InferResult[])
  protected override resolvedOnly(predResult: InferResult[]): InferResult {
    if (!predResult[0].unresolvable() && !predResult[1].unresolvable()) {
      return predResult[0].complete().add(predResult[1]);
    } else if (!predResult[0].unresolvable()) {
      return predResult[0].complete();
    } else if (!predResult[1].unresolvable()) {
      return predResult[1];
    } else {
      return InferResult.UNRESOLVABLE;
    }
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.When#toString(TokenType[])
  override toString(previous?: TokenType[]): string {
    return this.predicate2() + ' if ' + this.predicate1();
  }
}
