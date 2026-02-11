/**
 * OptionalPattern - matches a pattern zero or one time.
 * @JAVA_REF org.modelingvalue.nelumbo.patterns.OptionalPattern
 */

import { List, Map } from 'immutable';
import { TokenType } from '../syntax/TokenType';
import type { Token } from '../syntax/Token';
import type { AstElement } from '../AstElement';
import { Type } from '../Type';
import { Variable } from '../Variable';
import { Node } from '../Node';
import { Pattern } from './Pattern';
import type { ParseState } from '../syntax/ParseState';
import type { Functor } from './Functor';

export class OptionalPattern extends Pattern {
  constructor(type: Type, elements: List<AstElement>, ...args: unknown[]) {
    super(type, elements, ...args);
  }

  protected static fromData(data: unknown[], declaration?: Node): OptionalPattern {
    const pattern = Object.create(OptionalPattern.prototype) as OptionalPattern;
    (pattern as unknown as { _data: unknown[] })._data = data;
    (pattern as unknown as { _declaration: Node })._declaration = declaration ?? pattern;
    return pattern;
  }

  protected struct(data: unknown[], declaration?: Node): OptionalPattern {
    return OptionalPattern.fromData(data, declaration ?? this.declaration());
  }

  optional(): Pattern {
    return this.get(0) as Pattern;
  }

  // @JAVA_REF OptionalPattern.state(ParseState next)
  parseState(next: ParseState): ParseState {
    return this.optional().parseState(next).merge(next);
  }

  toString(_previous?: TokenType[]): string {
    return '<(>' + this.optional() + '<)?>';
  }

  setPrecedence(precedence: number): Pattern {
    return this.set(0, this.optional().setPrecedence(precedence)) as OptionalPattern;
  }

  setTypes(typeFunction: (type: Type) => Type): Pattern {
    return this.set(0, this.optional().setTypes(typeFunction)) as OptionalPattern;
  }

  argTypes(types: List<Type>): List<Type> {
    return this.optional().argTypes(types);
  }

  string(args: List<unknown>, ai: number, sb: string[], previous: TokenType[], _alt: boolean): number {
    const opt = args.get(ai);
    // Check if it's an "Optional" (represented as undefined for empty, or a value for present)
    if (opt === undefined || opt === null) {
      // Empty optional - produce nothing
      return ai + 1;
    }

    // If wrapped in an object with isPresent/value pattern
    if (typeof opt === 'object' && 'isPresent' in opt) {
      const inner: string[] = [];
      if ((opt as { isPresent: boolean }).isPresent) {
        const val = (opt as { isPresent: boolean; value: unknown }).value;
        const ii = this.optional().string(List([val]), 0, inner, previous, false);
        if (ii < 0) {
          return -1;
        }
      }
      sb.push(...inner);
      return ai + 1;
    }

    // Try to match the value directly
    const inner: string[] = [];
    const ii = this.optional().string(List([opt]), 0, inner, previous, false);
    if (ii >= 0) {
      sb.push(...inner);
    }
    return ai + 1;
  }

  extractArgs(
    elements: List<AstElement>,
    i: number,
    args: unknown[],
    _alt: boolean,
    functor: Functor,
    typeArgs: Map<Variable, Type>
  ): number {
    const inner: unknown[] = [];
    const ii = this.optional().extractArgs(elements, i, inner, true, functor, typeArgs);
    if (ii >= 0) {
      const first = inner[0];
      args.push(first !== undefined ? Optional.of(first) : Optional.empty());
      return ii;
    } else {
      args.push(Optional.empty());
      return i;
    }
  }

  tokenDeclaration(token: Token): Pattern | null {
    return this.optional().tokenDeclaration(token);
  }
}

/**
 * Optional wrapper type (TypeScript equivalent of Java Optional).
 */
export class Optional<T> {
  private readonly _value: T | undefined;
  private readonly _present: boolean;

  private constructor(value: T | undefined, present: boolean) {
    this._value = value;
    this._present = present;
  }

  static of<T>(value: T): Optional<T> {
    return new Optional(value, true);
  }

  static empty<T>(): Optional<T> {
    return new Optional<T>(undefined, false);
  }

  isPresent(): boolean {
    return this._present;
  }

  isEmpty(): boolean {
    return !this._present;
  }

  get(): T | undefined {
    return this._value;
  }

  orElse(defaultValue: T): T {
    return this._present ? this._value! : defaultValue;
  }
}

export function some<T>(value: T): Optional<T> {
  return Optional.of(value);
}

export function none<T>(): Optional<T> {
  return Optional.empty<T>();
}
