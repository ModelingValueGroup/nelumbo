/**
 * NString - String node type.
 * @JAVA_REF org.modelingvalue.nelumbo.strings.NString
 */

import { List } from 'immutable';
import type { AstElement } from '../AstElement';
import { Node } from '../Node';
import type { Functor } from '../patterns/Functor';
import { TokenType } from '../syntax/TokenType';

const DELIM = '"';

let FUNCTOR: Functor | null = null;

export function setNStringFunctor(f: Functor): void {
  FUNCTOR = f;
}

export class NString extends Node {
  constructor(functor: Functor, elements: List<AstElement>, ...args: unknown[]) {
    if (args.length === 1 && typeof args[0] === 'string') {
      // Constructor from parsed string - need to strip quotes
      const val = args[0] as string;
      super(functor, elements, strip(val) ?? val);
    } else {
      super(functor, elements, ...args);
    }
  }

  protected static fromData(data: unknown[], declaration?: Node): NString {
    const node = Object.create(NString.prototype) as NString;
    (node as unknown as { _data: unknown[] })._data = data;
    (node as unknown as { _declaration: Node | undefined })._declaration = declaration ?? node;
    return node;
  }

  protected override struct(data: unknown[], declaration?: Node): Node {
    return NString.fromData(data, declaration ?? this.declaration());
  }

  static of(val: string): NString {
    if (FUNCTOR === null) {
      throw new Error('NString FUNCTOR not initialized');
    }
    return new NString(FUNCTOR, List(), val);
  }

  static strip(val: string | null): string | null {
    return val !== null && val.startsWith(DELIM)
      ? val.substring(1, val.length - 1)
      : null;
  }

  value(): string {
    return this.get(0) as string;
  }

  override toString(_previous?: TokenType[]): string {
    return DELIM + this.value() + DELIM;
  }
}

function strip(val: string): string | null {
  return NString.strip(val);
}
