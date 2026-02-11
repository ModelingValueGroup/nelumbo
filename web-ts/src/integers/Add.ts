/**
 * Add - Integer addition predicate.
 * @JAVA_REF org.modelingvalue.nelumbo.integers.Add
 */

import { List } from 'immutable';
import type { AstElement } from '../AstElement';
import { Predicate } from '../logic/Predicate';
import type { Functor } from '../patterns/Functor';
import type { InferContext } from '../InferContext';
import type { InferResult } from '../InferResult';
import { NInteger } from './NInteger';

export class Add extends Predicate {
  constructor(functor: Functor, elements: List<AstElement>, ...args: unknown[]) {
    super(functor, elements, args[0], args[1], args[2]);
  }

  protected static fromDataAdd(data: unknown[], declaration?: Predicate): Add {
    const node = Object.create(Add.prototype) as Add;
    (node as unknown as { _data: unknown[] })._data = data;
    (node as unknown as { _declaration: Predicate | undefined })._declaration = declaration ?? node;
    return node;
  }

  protected override struct(data: unknown[], declaration?: Predicate): Predicate {
    return Add.fromDataAdd(data, declaration ?? this.declaration());
  }

  override infer(_context: InferContext): InferResult {
    const nrOfUnbound = this.nrOfUnbound();
    if (nrOfUnbound > 1) {
      return this.unresolvable();
    }

    const addend1 = this.getIntegerVal(0);
    const addend2 = this.getIntegerVal(1);
    const sum = this.getIntegerVal(2);

    if (addend1 !== null && addend2 !== null) {
      const s = addend1 + addend2;
      if (sum !== null) {
        const eq = s === sum;
        return eq ? this.factCC() : this.falsehoodCC();
      } else {
        return (this.set(2, NInteger.of(s)) as Add).factCI();
      }
    } else if (addend1 !== null && sum !== null) {
      return (this.set(1, NInteger.of(sum - addend1)) as Add).factCI();
    } else if (addend2 !== null && sum !== null) {
      return (this.set(0, NInteger.of(sum - addend2)) as Add).factCI();
    }

    return this.unknown();
  }

  private getIntegerVal(index: number): bigint | null {
    const val = this.get(index);
    if (val instanceof NInteger) {
      return val.value();
    }
    return null;
  }
}
