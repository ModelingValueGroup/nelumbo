/**
 * BinaryPredicate - abstract base for binary logical operators.
 * @JAVA_REF org.modelingvalue.nelumbo.logic.BinaryPredicate
 */

import { List } from 'immutable';
import type { AstElement } from '../AstElement';
import { Node } from '../Node';
import type { Functor } from '../patterns/Functor';
import { Predicate } from './Predicate';
import { CompoundPredicate } from './CompoundPredicate';
import { InferResult } from '../InferResult';
import type { InferContext } from '../InferContext';

// Late-bound NBoolean reference (set from NBoolean.ts)
let _NBooleanResult: ((which: 'TRUE' | 'FALSE' | 'UNKNOWN') => InferResult) | null = null;

export function _setNBooleanResultFn(fn: (which: 'TRUE' | 'FALSE' | 'UNKNOWN') => InferResult): void {
  _NBooleanResult = fn;
}

/**
 * BinaryPredicate - a predicate with two operands.
 * @JAVA_REF org.modelingvalue.nelumbo.logic.BinaryPredicate
 */
export abstract class BinaryPredicate extends CompoundPredicate {
  constructor(functor: Functor, elements: List<AstElement>, left: Node, right: Node) {
    super(functor, elements, left, right);
  }

  override declaration(): BinaryPredicate {
    return super.declaration() as BinaryPredicate;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.BinaryPredicate#predicate1()
  predicate1(): Predicate {
    return this.predicateAt(0);
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.BinaryPredicate#predicate2()
  predicate2(): Predicate {
    return this.predicateAt(1);
  }

  /**
   * Override infer (not resolve) matching Java's BinaryPredicate.infer(InferContext).
   * @JAVA_REF org.modelingvalue.nelumbo.logic.BinaryPredicate#infer(InferContext)
   */
  override infer(context: InferContext): InferResult {
    const predicate: Predicate[] = [this.predicate1(), this.predicate2()];
    const predResult: InferResult[] = [null as unknown as InferResult, null as unknown as InferResult];

    for (let i = 0; i < 2; i++) {
      predResult[i] = predicate[i].infer(context);
      if (predResult[i].hasStackOverflow()) {
        return predResult[i];
      } else if (context.reduce()) {
        if (this.isTrueSingle(predResult[i], i)) {
          return _NBooleanResult!('TRUE');
        } else if (this.isFalseSingle(predResult[i], i)) {
          return _NBooleanResult!('FALSE');
        } else if (this.isUnknownSingle(predResult[i], i)) {
          return _NBooleanResult!('UNKNOWN');
        }
      }
    }

    if (context.reduce()) {
      if (this.isTrueBoth(predResult)) {
        return _NBooleanResult!('TRUE');
      } else if (this.isFalseBoth(predResult)) {
        return _NBooleanResult!('FALSE');
      } else if (this.isLeft(predResult)) {
        return predResult[0];
      } else if (this.isRight(predResult)) {
        return predResult[1];
      } else {
        return this.set(0, predResult[0].predicateOf(), predResult[1].predicateOf()).unknown();
      }
    } else {
      return this.resolvedOnly(predResult);
    }
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.BinaryPredicate#resolvedOnly(InferResult[])
  protected resolvedOnly(predResult: InferResult[]): InferResult {
    if (!predResult[0].unresolvable() && !predResult[1].unresolvable()) {
      return predResult[0].add(predResult[1]);
    } else if (!predResult[0].unresolvable()) {
      return predResult[0];
    } else if (!predResult[1].unresolvable()) {
      return predResult[1];
    } else {
      return InferResult.UNRESOLVABLE;
    }
  }

  // Abstract template methods - @JAVA_REF org.modelingvalue.nelumbo.logic.BinaryPredicate
  protected abstract isTrueSingle(predResult: InferResult, i: number): boolean;
  protected abstract isFalseSingle(predResult: InferResult, i: number): boolean;
  protected abstract isUnknownSingle(predResult: InferResult, i: number): boolean;
  protected abstract isTrueBoth(predResult: InferResult[]): boolean;
  protected abstract isFalseBoth(predResult: InferResult[]): boolean;
  protected abstract isLeft(predResult: InferResult[]): boolean;
  protected abstract isRight(predResult: InferResult[]): boolean;
}
