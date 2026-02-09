/**
 * AlternationPattern - matches one of several pattern alternatives.
 * Ported from Java: org.modelingvalue.nelumbo.patterns.AlternationPattern
 */

import { List, Map } from 'immutable';
import { TokenType } from '../TokenType';
import type { Token } from '../Token';
import type { AstElement } from '../core/AstElement';
import { Type } from '../core/Type';
import { Variable } from '../core/Variable';
import { Node } from '../core/Node';
import { Pattern } from './Pattern';
import type { ParseState } from '../syntax/ParseState';
import type { Functor } from './Functor';

export class AlternationPattern extends Pattern {
  constructor(type: Type, elements: List<AstElement>, ...args: unknown[]) {
    super(type, elements, ...args);
  }

  protected static fromData(data: unknown[], declaration?: Node): AlternationPattern {
    const pattern = Object.create(AlternationPattern.prototype) as AlternationPattern;
    (pattern as unknown as { _data: unknown[] })._data = data;
    (pattern as unknown as { _declaration: Node })._declaration = declaration ?? pattern;
    return pattern;
  }

  protected struct(data: unknown[], declaration?: Node): AlternationPattern {
    return AlternationPattern.fromData(data, declaration ?? this.declaration());
  }

  options(): List<Pattern> {
    return this.get(0) as List<Pattern>;
  }

  parseState(next: ParseState, functor: Functor): ParseState {
    const { ParseState: PS } = require('../syntax/ParseState');
    let result: ParseState = PS.EMPTY;
    for (const option of this.options()) {
      result = result.merge(option.parseState(next, functor));
    }
    return result;
  }

  toString(_previous?: TokenType[]): string {
    const optStr = this.options().map(o => String(o)).filter(s => s.length > 0).join('<|>');
    return '<(>' + optStr + '<)>';
  }

  setPrecedence(precedence: number): Pattern {
    let options = this.options();
    for (let i = 0; i < options.size; i++) {
      const pa = options.get(i)!;
      const pb = pa.setPrecedence(precedence);
      if (!pb.equals(pa)) {
        options = options.set(i, pb);
      }
    }
    return this.set(0, options) as AlternationPattern;
  }

  setTypes(typeFunction: (type: Type) => Type): Pattern {
    let options = this.options();
    for (let i = 0; i < options.size; i++) {
      const pa = options.get(i)!;
      const pb = pa.setTypes(typeFunction);
      if (!pb.equals(pa)) {
        options = options.set(i, pb);
      }
    }
    return this.set(0, options) as AlternationPattern;
  }

  argTypes(types: List<Type>): List<Type> {
    return types.push(Type.$OBJECT);
  }

  string(args: List<unknown>, ai: number, sb: string[], previous: TokenType[], _alt: boolean): number {
    const o = args.get(ai);
    for (const option of this.options()) {
      const inner: string[] = [];
      const ii = option.string(List([o]), 0, inner, previous, true);
      if (ii >= 0) {
        sb.push(...inner);
        return ai + 1;
      }
    }
    return -1;
  }

  extractArgs(
    elements: List<AstElement>,
    i: number,
    args: unknown[],
    _alt: boolean,
    functor: Functor,
    typeArgs: Map<Variable, Type>
  ): number {
    for (const option of this.options()) {
      const inner: unknown[] = [];
      const ii = option.extractArgs(elements, i, inner, true, functor, typeArgs);
      if (ii >= 0) {
        args.push(...inner);
        return ii;
      }
    }
    return -1;
  }

  tokenDeclaration(token: Token): Pattern | null {
    for (const option of this.options()) {
      const decl = option.tokenDeclaration(token);
      if (decl !== null) {
        return decl;
      }
    }
    return null;
  }
}
