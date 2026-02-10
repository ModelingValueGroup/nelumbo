/**
 * TokenTextPattern - matches a specific token text.
 * Ported from Java: org.modelingvalue.nelumbo.patterns.TokenTextPattern
 */

import { List, Map } from 'immutable';
import { TokenType } from '../syntax/TokenType';
import { Token } from '../syntax/Token';
import type { AstElement } from '../AstElement';
import { Type } from '../Type';
import { Variable } from '../Variable';
import { Node } from '../Node';
import { Pattern } from './Pattern';
import { ParseState } from '../syntax/ParseState';
import type { Functor } from './Functor';

export class TokenTextPattern extends Pattern {
  constructor(type: Type, elements: List<AstElement>, ...args: unknown[]) {
    super(type, elements, ...args);
  }

  protected static fromData(data: unknown[], declaration?: Node): TokenTextPattern {
    const pattern = Object.create(TokenTextPattern.prototype) as TokenTextPattern;
    (pattern as unknown as { _data: unknown[] })._data = data;
    (pattern as unknown as { _declaration: Node })._declaration = declaration ?? pattern;
    return pattern;
  }

  protected struct(data: unknown[], declaration?: Node): TokenTextPattern {
    return TokenTextPattern.fromData(data, declaration ?? this.declaration());
  }

  tokenText(): string {
    const val = this.get(0);
    if (val instanceof Variable) {
      return val.name();
    }
    return val as string;
  }

  variable(): Variable | null {
    const val = this.get(0);
    return val instanceof Variable ? val : null;
  }

  isKeyword(): boolean {
    if (this.length() > 1) {
      const isKw = this.get(1);
      return isKw === true;
    }
    return false;
  }

  setBinding(vars: Map<Variable, unknown>, declaration?: Node): TokenTextPattern {
    const v = this.variable();
    if (v !== null) {
      const val = vars.get(v);
      if (typeof val === 'string') {
        return this.set(0, val) as TokenTextPattern;
      }
    }
    return super.setBinding(vars, declaration) as TokenTextPattern;
  }

  parseState(next: ParseState, _functor: Functor): ParseState {
    return new ParseState(this.tokenText(), this.isKeyword(), next);
  }

  name(): string {
    return this.tokenText();
  }

  toString(_previous?: TokenType[]): string {
    return this.tokenText();
  }

  argTypes(types: List<Type>): List<Type> {
    return types;
  }

  string(args: List<unknown>, ai: number, sb: string[], previous: TokenType[], alt: boolean): number {
    if (alt) {
      const arg = args.get(ai);
      if (typeof arg === 'string' && arg === this.tokenText()) {
        this.addText(sb, previous, arg);
        return ai + 1;
      }
      return -1;
    }
    this.addText(sb, previous, this.tokenText());
    return ai;
  }

  extractArgs(
    elements: List<AstElement>,
    i: number,
    args: unknown[],
    alt: boolean,
    _functor: Functor,
    _typeArgs: Map<Variable, Type>
  ): number {
    if (i < elements.size) {
      const e = elements.get(i);
      if (e instanceof Token && e.text === this.tokenText()) {
        if (alt) {
          args.push(e.text);
        }
        return i + 1;
      }
    }
    return -1;
  }

  tokenDeclaration(token: Token): Pattern | null {
    return this.tokenText() === token.text ? this : null;
  }
}
