/**
 * RepetitionPattern - matches a pattern zero or more times (with optional separator).
 * Ported from Java: org.modelingvalue.nelumbo.patterns.RepetitionPattern
 */

import { List, Map, Set } from 'immutable';
import { TokenType } from '../TokenType';
import type { Token } from '../Token';
import type { AstElement } from '../core/AstElement';
import { Type } from '../core/Type';
import { Variable } from '../core/Variable';
import { Node } from '../core/Node';
import { Pattern } from './Pattern';
import type { ParseState } from '../syntax/ParseState';
import type { Functor } from './Functor';

export class RepetitionPattern extends Pattern {
  constructor(type: Type, elements: List<AstElement>, ...args: unknown[]) {
    super(type, elements, ...args);
  }

  protected static fromData(data: unknown[], declaration?: Node): RepetitionPattern {
    const pattern = Object.create(RepetitionPattern.prototype) as RepetitionPattern;
    (pattern as unknown as { _data: unknown[] })._data = data;
    (pattern as unknown as { _declaration: Node })._declaration = declaration ?? pattern;
    return pattern;
  }

  protected struct(data: unknown[], declaration?: Node): RepetitionPattern {
    return RepetitionPattern.fromData(data, declaration ?? this.declaration());
  }

  repeated(): Pattern {
    return this.get(0) as Pattern;
  }

  mandatory(): boolean {
    return this.get(1) as boolean;
  }

  separator(): Pattern | null {
    return this.get(2) as Pattern | null;
  }

  toString(_previous?: TokenType[]): string {
    const sep = this.separator();
    const sepStr = sep !== null ? '<,>' + sep : '';
    return '<(>' + this.repeated() + sepStr + (this.mandatory() ? '<)+>' : '<)*>');
  }

  setPrecedence(precedence: number): Pattern {
    return this.set(0, this.repeated().setPrecedence(precedence)) as RepetitionPattern;
  }

  setTypes(typeFunction: (type: Type) => Type): Pattern {
    return this.set(0, this.repeated().setTypes(typeFunction)) as RepetitionPattern;
  }

  parseState(next: ParseState, functor: Functor): ParseState {
    const { ParseState: PS } = require('../syntax/ParseState');

    const start = new PS(Set<RepetitionPattern>([this]), Set<RepetitionPattern>());
    let end = new PS(Set<RepetitionPattern>(), Set<RepetitionPattern>([this]));

    const separator = this.separator();
    if (separator !== null) {
      end = separator.parseState(end, functor);
    }
    end = end.merge(next);

    const repeated = this.repeated();
    let state = repeated.parseState(end, functor).merge(start);

    if (!this.mandatory()) {
      if (separator !== null) {
        state = separator.parseState(state, functor).merge(next);
        state = repeated.parseState(state, functor);
      }
      state = state.merge(next);
    }

    return state;
  }

  argTypes(types: List<Type>): List<Type> {
    return this.repeated().argTypes(types);
  }

  string(args: List<unknown>, ai: number, sb: string[], previous: TokenType[], _alt: boolean): number {
    const listArg = args.get(ai);
    if (List.isList(listArg)) {
      const list = listArg as List<unknown>;
      const repeated = this.repeated();
      const separator = this.separator();
      const inner: string[] = [];

      for (let idx = 0; idx < list.size; idx++) {
        const o = list.get(idx);
        if (separator !== null && inner.length > 0) {
          if (separator.string(List([o]), 0, inner, previous, false) < 0) {
            return -1;
          }
        }
        if (repeated.string(List([o]), 0, inner, previous, false) < 0) {
          return -1;
        }
      }
      sb.push(...inner);
      return ai + 1;
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
    const repeated = this.repeated();
    const separator = this.separator();
    let mandatory = this.mandatory();
    let result: unknown[] = [];

    while (true) {
      const inner: unknown[] = [];
      const ii = repeated.extractArgs(elements, i, inner, false, functor, typeArgs);
      if (ii >= 0) {
        result.push(...inner);
        i = ii;
        mandatory = false;
      } else if (mandatory) {
        return -1;
      } else {
        break;
      }

      if (separator !== null) {
        const sepInner: unknown[] = [];
        const sepii = separator.extractArgs(elements, i, sepInner, false, functor, typeArgs);
        if (sepii >= 0) {
          mandatory = true;
          i = sepii;
        } else {
          break;
        }
      }
    }

    args.push(List(result));
    return i;
  }

  tokenDeclaration(token: Token): Pattern | null {
    const decl = this.repeated().tokenDeclaration(token);
    if (decl !== null) {
      return decl;
    }
    const separator = this.separator();
    return separator !== null ? separator.tokenDeclaration(token) : null;
  }
}
