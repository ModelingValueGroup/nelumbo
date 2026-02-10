/**
 * TokenTypePattern - matches a specific token type.
 * Ported from Java: org.modelingvalue.nelumbo.patterns.TokenTypePattern
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

export class TokenTypePattern extends Pattern {
  constructor(type: Type, elements: List<AstElement>, ...args: unknown[]) {
    super(type, elements, ...args);
  }

  protected static fromData(data: unknown[], declaration?: Node): TokenTypePattern {
    const pattern = Object.create(TokenTypePattern.prototype) as TokenTypePattern;
    (pattern as unknown as { _data: unknown[] })._data = data;
    (pattern as unknown as { _declaration: Node })._declaration = declaration ?? pattern;
    return pattern;
  }

  protected struct(data: unknown[], declaration?: Node): TokenTypePattern {
    return TokenTypePattern.fromData(data, declaration ?? this.declaration());
  }

  tokenType(): TokenType {
    return this.get(0) as TokenType;
  }

  toString(_previous?: TokenType[]): string {
    return '<' + this.tokenType().name + '>';
  }

  parseState(next: ParseState, _functor: Functor): ParseState {
    return new ParseState(this.tokenType(), next);
  }

  argTypes(types: List<Type>): List<Type> {
    const tt = this.tokenType();
    return !this.isEmpty(tt) ? types.push(Type.$STRING) : types;
  }

  string(args: List<unknown>, ai: number, sb: string[], previous: TokenType[], _alt: boolean): number {
    const tt = this.tokenType();
    if (!this.isEmpty(tt)) {
      const val = args.get(ai);
      let text: string | null = null;

      if (typeof val === 'string') {
        text = val;
      } else if (val !== null && val !== undefined) {
        text = String(val);
      }

      if (text !== null && this.matches(tt, text)) {
        this.addText(sb, previous, text);
        return ai + 1;
      }
      return -1;
    }
    return ai;
  }

  private matches(tt: TokenType, text: string): boolean {
    // Check if the text matches this token type pattern
    if (tt.pattern) {
      const match = text.match(tt.pattern);
      return match !== null && match[0] === text;
    }
    return false;
  }

  extractArgs(
    elements: List<AstElement>,
    i: number,
    args: unknown[],
    _alt: boolean,
    _functor: Functor,
    _typeArgs: Map<Variable, Type>
  ): number {
    if (i < elements.size) {
      const e = elements.get(i);
      const tt = this.tokenType();

      if (e instanceof Token) {
        if (e.isTextMatch && tt.isVariableContent()) {
          return -1;
        } else if (e.type === tt) {
          if (!this.isEmpty(tt)) {
            args.push(e.text);
          }
          return i + 1;
        } else if (TokenType.NEWLINE === tt && Pattern.isEndOfLine(e)) {
          return i;
        }
      } else if (e instanceof Variable && tt === e.type().tokenType()) {
        args.push(e);
        return i + 1;
      }
    }
    return -1;
  }

  private isEmpty(tt: TokenType): boolean {
    return tt === TokenType.NEWLINE ||
           tt === TokenType.BEGINOFFILE ||
           tt === TokenType.ENDOFFILE ||
           tt === TokenType.ENDOFLINE;
  }

  tokenDeclaration(_token: Token): Pattern | null {
    return null;
  }
}
