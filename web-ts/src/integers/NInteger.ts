/**
 * NInteger - Integer node type.
 * @JAVA_REF org.modelingvalue.nelumbo.integers.NInteger
 */

import { List } from 'immutable';
import type { AstElement } from '../AstElement';
import { Node } from '../Node';
import type { Functor } from '../patterns/Functor';
import { TokenType } from '../syntax/TokenType';

// Using bigint for arbitrary precision integers (like Java's BigInteger)
const MIN = BigInt(Number.MIN_SAFE_INTEGER);
const MAX = BigInt(Number.MAX_SAFE_INTEGER);

let FUNCTOR: Functor | null = null;

export function setNIntegerFunctor(f: Functor): void {
  FUNCTOR = f;
}

export class NInteger extends Node {
  constructor(functor: Functor, elements: List<AstElement>, ...args: unknown[]) {
    if (args.length === 1 && typeof args[0] === 'string') {
      // Constructor from parsed string
      super(functor, elements, parse(args[0]));
    } else if (args.length === 1 && typeof args[0] === 'bigint') {
      // Constructor from bigint value
      super(functor, elements, args[0]);
    } else {
      super(functor, elements, ...args);
    }
  }

  protected static fromData(data: unknown[], declaration?: Node): NInteger {
    const node = Object.create(NInteger.prototype) as NInteger;
    (node as unknown as { _data: unknown[] })._data = data;
    (node as unknown as { _declaration: Node | undefined })._declaration = declaration ?? node;
    return node;
  }

  protected override struct(data: unknown[], declaration?: Node): Node {
    return NInteger.fromData(data, declaration ?? this.declaration());
  }

  static of(val: bigint): NInteger {
    if (FUNCTOR === null) {
      throw new Error('NInteger FUNCTOR not initialized');
    }
    return new NInteger(FUNCTOR, List(), val);
  }

  value(): bigint {
    return this.get(0) as bigint;
  }

  override toString(_previous?: TokenType[]): string {
    const value = this.value();
    // Use base 36 for very large numbers (like Java's Character.MAX_RADIX)
    const string = value > MAX || value < MIN
      ? `36#${value.toString(36)}`
      : value.toString();

    if (_previous && (_previous[0] === TokenType.NAME || _previous[0] === TokenType.NUMBER || _previous[0] === TokenType.DECIMAL)) {
      _previous[0] = TokenType.NUMBER;
      return ' ' + string;
    }
    if (_previous) {
      _previous[0] = TokenType.NUMBER;
    }
    return string;
  }
}

function parse(string: string): bigint {
  const i = string.indexOf('#');
  if (i > 0) {
    const radix = parseInt(string.substring(0, i), 10);
    return BigInt(parseInt(string.substring(i + 1), radix));
  }
  return BigInt(string);
}
