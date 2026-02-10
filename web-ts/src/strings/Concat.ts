/**
 * Concat - String concatenation predicate.
 * Ported from Java: org.modelingvalue.nelumbo.strings.Concat
 */

import { List } from 'immutable';
import type { AstElement } from '../AstElement';
import { Predicate } from '../logic/Predicate';
import type { Functor } from '../patterns/Functor';
import type { InferContext } from '../InferContext';
import type { InferResult } from '../InferResult';
import { NString } from './NString';

export class Concat extends Predicate {
  constructor(functor: Functor, elements: List<AstElement>, ...args: unknown[]) {
    super(functor, elements, args[0], args[1], args[2]);
  }

  protected static fromDataConcat(data: unknown[], declaration?: Predicate): Concat {
    const node = Object.create(Concat.prototype) as Concat;
    (node as unknown as { _data: unknown[] })._data = data;
    (node as unknown as { _declaration: Predicate | undefined })._declaration = declaration ?? node;
    return node;
  }

  protected override struct(data: unknown[], declaration?: Predicate): Predicate {
    return Concat.fromDataConcat(data, declaration ?? this.declaration());
  }

  override infer(_context: InferContext): InferResult {
    const nrOfUnbound = this.nrOfUnbound();
    if (nrOfUnbound > 1) {
      return this.unresolvable();
    }

    const addend1 = this.getStringVal(0);
    const addend2 = this.getStringVal(1);
    const sum = this.getStringVal(2);

    if (addend1 !== null && addend2 !== null) {
      const s = addend1 + addend2;
      if (sum !== null) {
        const eq = s === sum;
        return eq ? this.factCC() : this.falsehoodCC();
      } else {
        return (this.set(2, NString.of(s)) as Concat).factCI();
      }
    } else if (addend1 !== null && sum !== null) {
      if (sum.startsWith(addend1)) {
        return (this.set(1, NString.of(sum.substring(addend1.length))) as Concat).factCI();
      } else {
        return this.falsehoodCI();
      }
    } else if (addend2 !== null && sum !== null) {
      if (sum.endsWith(addend2)) {
        return (this.set(0, NString.of(sum.substring(0, sum.length - addend2.length))) as Concat).factCI();
      } else {
        return this.falsehoodCI();
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
}
