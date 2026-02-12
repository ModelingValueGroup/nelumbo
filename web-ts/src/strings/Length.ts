/**
 * Length - String length predicate.
 * @JAVA_REF org.modelingvalue.nelumbo.strings.Length
 */

import { List } from 'immutable';
import type { AstElement } from '../AstElement';
import { Predicate } from '../logic/Predicate';
import type { Functor } from '../patterns/Functor';
import type { InferContext } from '../InferContext';
import type { InferResult } from '../InferResult';
import { NInteger } from '../integers/NInteger';
import { NString } from './NString';

export class Length extends Predicate {
  constructor(functor: Functor, elements: List<AstElement>, ...args: unknown[]) {
    super(functor, elements, args[0], args[1]);
  }

  protected static fromDataLength(data: unknown[], declaration?: Predicate): Length {
    const node = Object.create(Length.prototype) as Length;
    (node as unknown as { _data: unknown[] })._data = data;
    (node as unknown as { _declaration: Predicate | undefined })._declaration = declaration ?? node;
    (node as any)._binding = null;
    (node as any)._hashCodeCached = false;
    (node as any)._hashCode = 0;
    (node as any)._nrOfUnbound = -1;
    return node;
  }

  protected override struct(data: unknown[], declaration?: Predicate): Predicate {
    return Length.fromDataLength(data, declaration ?? this.declaration());
  }

  // @JAVA_REF org.modelingvalue.nelumbo.strings.Length#infer(int, InferContext)
  protected override inferInternal(_nrOfUnbound: number, _context: InferContext): InferResult {
    const nrOfUnbound = this.nrOfUnbound();
    if (nrOfUnbound > 1) {
      return this.unresolvable();
    }

    const string = this.getStringVal(0);
    const length = this.getIntegerVal(1);

    if (string !== null) {
      const actual = BigInt(string.length);
      if (length !== null) {
        const eq = length === actual;
        return eq ? this.factCC() : this.falsehoodCC();
      } else {
        return (this.set(1, NInteger.of(actual)) as Length).factCI();
      }
    }

    return this.unknown();
  }

  private getStringVal(index: number): string | null {
    const val = this.get(index);
    if (val instanceof NString) {
      return val.value();
    }
    return null;
  }

  private getIntegerVal(index: number): bigint | null {
    const val = this.get(index);
    if (val instanceof NInteger) {
      return val.value();
    }
    return null;
  }
}
