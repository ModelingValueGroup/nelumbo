/**
 * ToInteger - String to integer conversion predicate.
 * @JAVA_REF org.modelingvalue.nelumbo.strings.ToInteger
 */

import { List } from 'immutable';
import type { AstElement } from '../AstElement';
import { Predicate } from '../logic/Predicate';
import type { Functor } from '../patterns/Functor';
import type { InferContext } from '../InferContext';
import type { InferResult } from '../InferResult';
import { NInteger } from '../integers/NInteger';
import { NString } from './NString';

export class ToInteger extends Predicate {
  constructor(functor: Functor, elements: List<AstElement>, ...args: unknown[]) {
    super(functor, elements, args[0], args[1]);
  }

  protected static fromDataToInteger(data: unknown[], declaration?: Predicate): ToInteger {
    const node = Object.create(ToInteger.prototype) as ToInteger;
    (node as unknown as { _data: unknown[] })._data = data;
    (node as unknown as { _declaration: Predicate | undefined })._declaration = declaration ?? node;
    return node;
  }

  protected override struct(data: unknown[], declaration?: Predicate): Predicate {
    return ToInteger.fromDataToInteger(data, declaration ?? this.declaration());
  }

  override infer(_context: InferContext): InferResult {
    const nrOfUnbound = this.nrOfUnbound();
    if (nrOfUnbound > 1) {
      return this.unresolvable();
    }

    const integer = this.getIntegerVal(0);
    const string = this.getStringVal(1);

    if (string !== null) {
      try {
        const parsed = BigInt(parseInt(string, 10));
        if (integer !== null) {
          const eq = integer === parsed;
          return eq ? this.factCC() : this.falsehoodCC();
        } else {
          return (this.set(0, NInteger.of(parsed)) as ToInteger).factCI();
        }
      } catch {
        return integer !== null ? this.falsehoodCC() : this.falsehoodCI();
      }
    } else if (integer !== null) {
      const s = integer.toString();
      return (this.set(1, NString.of(s)) as ToInteger).factCI();
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

  private getStringVal(index: number): string | null {
    const val = this.get(index);
    if (val instanceof NString) {
      return val.value();
    }
    return null;
  }
}
