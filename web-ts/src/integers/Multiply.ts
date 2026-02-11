/**
 * Multiply - Integer multiplication predicate.
 * @JAVA_REF org.modelingvalue.nelumbo.integers.Multiply
 */

import { List } from 'immutable';
import type { AstElement } from '../AstElement';
import { Predicate } from '../logic/Predicate';
import type { Functor } from '../patterns/Functor';
import type { InferContext } from '../InferContext';
import type { InferResult } from '../InferResult';
import { NInteger } from './NInteger';

export class Multiply extends Predicate {
  constructor(functor: Functor, elements: List<AstElement>, ...args: unknown[]) {
    super(functor, elements, args[0], args[1], args[2]);
  }

  protected static fromDataMultiply(data: unknown[], declaration?: Predicate): Multiply {
    const node = Object.create(Multiply.prototype) as Multiply;
    (node as unknown as { _data: unknown[] })._data = data;
    (node as unknown as { _declaration: Predicate | undefined })._declaration = declaration ?? node;
    return node;
  }

  protected override struct(data: unknown[], declaration?: Predicate): Predicate {
    return Multiply.fromDataMultiply(data, declaration ?? this.declaration());
  }

  override infer(_context: InferContext): InferResult {
    const nrOfUnbound = this.nrOfUnbound();
    if (nrOfUnbound > 1) {
      return this.unresolvable();
    }

    const factor1 = this.getIntegerVal(0);
    const factor2 = this.getIntegerVal(1);
    const product = this.getIntegerVal(2);

    if (factor1 !== null && factor2 !== null) {
      const p = factor1 * factor2;
      if (product !== null) {
        const eq = p === product;
        return eq ? this.factCC() : this.falsehoodCC();
      } else {
        return (this.set(2, NInteger.of(p)) as Multiply).factCI();
      }
    } else if (factor1 !== null && product !== null && factor1 !== 0n) {
      const quotient = product / factor1;
      const remainder = product % factor1;
      return remainder === 0n
        ? (this.set(1, NInteger.of(quotient)) as Multiply).factCI()
        : this.falsehoodCI();
    } else if (factor2 !== null && product !== null && factor2 !== 0n) {
      const quotient = product / factor2;
      const remainder = product % factor2;
      return remainder === 0n
        ? (this.set(0, NInteger.of(quotient)) as Multiply).factCI()
        : this.falsehoodCI();
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
