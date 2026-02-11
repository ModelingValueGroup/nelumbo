/**
 * GreaterThan - Integer comparison predicate.
 * @JAVA_REF org.modelingvalue.nelumbo.integers.GreaterThan
 */

import { List } from 'immutable';
import type { AstElement } from '../AstElement';
import { Predicate } from '../logic/Predicate';
import type { Functor } from '../patterns/Functor';
import type { InferContext } from '../InferContext';
import type { InferResult } from '../InferResult';
import { NInteger } from './NInteger';

export class GreaterThan extends Predicate {
  constructor(functor: Functor, elements: List<AstElement>, ...args: unknown[]) {
    super(functor, elements, args[0], args[1]);
  }

  protected static fromDataGreaterThan(data: unknown[], declaration?: Predicate): GreaterThan {
    const node = Object.create(GreaterThan.prototype) as GreaterThan;
    (node as unknown as { _data: unknown[] })._data = data;
    (node as unknown as { _declaration: Predicate | undefined })._declaration = declaration ?? node;
    return node;
  }

  protected override struct(data: unknown[], declaration?: Predicate): Predicate {
    return GreaterThan.fromDataGreaterThan(data, declaration ?? this.declaration());
  }

  override infer(_context: InferContext): InferResult {
    const nrOfUnbound = this.nrOfUnbound();
    if (nrOfUnbound > 1) {
      return this.unresolvable();
    }

    const l = this.getIntegerVal(0);
    const r = this.getIntegerVal(1);

    if (l === null) {
      return (this.set(0, this.get(1)) as GreaterThan).falsehoodsII();
    }
    if (r === null) {
      return (this.set(1, this.get(0)) as GreaterThan).falsehoodsII();
    }

    return l > r ? this.factCC() : this.falsehoodCC();
  }

  private getIntegerVal(index: number): bigint | null {
    const val = this.get(index);
    if (val instanceof NInteger) {
      return val.value();
    }
    return null;
  }
}
