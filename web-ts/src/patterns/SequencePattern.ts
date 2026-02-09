/**
 * SequencePattern - matches a sequence of patterns.
 * Ported from Java: org.modelingvalue.nelumbo.patterns.SequencePattern
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

export class SequencePattern extends Pattern {
  constructor(type: Type, elements: List<AstElement>, ...args: unknown[]) {
    super(type, elements, ...args);
  }

  protected static fromData(data: unknown[], declaration?: Node): SequencePattern {
    const pattern = Object.create(SequencePattern.prototype) as SequencePattern;
    (pattern as unknown as { _data: unknown[] })._data = data;
    (pattern as unknown as { _declaration: Node })._declaration = declaration ?? pattern;
    return pattern;
  }

  protected struct(data: unknown[], declaration?: Node): SequencePattern {
    return SequencePattern.fromData(data, declaration ?? this.declaration());
  }

  patternElements(): List<Pattern> {
    return this.get(0) as List<Pattern>;
  }

  name(): string {
    let name = '';
    for (const element of this.patternElements()) {
      name += element.name();
    }
    return name;
  }

  toString(_previous?: TokenType[]): string {
    return this.patternElements().map(e => String(e)).join('');
  }

  setPrecedence(precedence: number): Pattern {
    let elems = this.patternElements();
    for (let i = 0; i < elems.size; i++) {
      const pa = elems.get(i)!;
      const pb = pa.setPrecedence(precedence);
      if (!pb.equals(pa)) {
        elems = elems.set(i, pb);
      }
    }
    return this.set(0, elems) as SequencePattern;
  }

  setTypes(typeFunction: (type: Type) => Type): Pattern {
    let elems = this.patternElements();
    for (let i = 0; i < elems.size; i++) {
      const pa = elems.get(i)!;
      const pb = pa.setTypes(typeFunction);
      if (!pb.equals(pa)) {
        elems = elems.set(i, pb);
      }
    }
    return this.set(0, elems) as SequencePattern;
  }

  parseState(next: ParseState, functor: Functor): ParseState {
    let state = next;
    for (const element of this.patternElements().reverse()) {
      state = element.parseState(state, functor);
    }
    return state;
  }

  argTypes(types: List<Type>): List<Type> {
    for (const element of this.patternElements()) {
      types = element.argTypes(types);
    }
    return types;
  }

  string(args: List<unknown>, ai: number, sb: string[], previous: TokenType[], _alt: boolean): number {
    let argList = args;
    if (this.argTypes(List()).size === 1) {
      argList = List([List([args.get(ai)])]);
    }

    const listArg = argList.get(ai);
    if (List.isList(listArg)) {
      const list = listArg as List<unknown>;
      const inner: string[] = [];
      let ii = 0;
      for (const element of this.patternElements()) {
        ii = element.string(list, ii, inner, previous, false);
        if (ii < 0) {
          return -1;
        }
      }
      sb.push(...inner);
      return ai + 1;
    }
    return -1;
  }

  extractArgs(
    astElements: List<AstElement>,
    i: number,
    args: unknown[],
    _alt: boolean,
    functor: Functor,
    typeArgs: Map<Variable, Type>
  ): number {
    const result: unknown[] = [];
    for (const element of this.patternElements()) {
      const inner: unknown[] = [];
      const ii = element.extractArgs(astElements, i, inner, false, functor, typeArgs);
      if (ii >= 0) {
        result.push(...inner);
        i = ii;
      } else {
        return -1;
      }
    }
    args.push(result.length > 1 ? List(result) : result[0]);
    return i;
  }

  tokenDeclaration(token: Token): Pattern | null {
    for (const element of this.patternElements()) {
      const decl = element.tokenDeclaration(token);
      if (decl !== null) {
        return decl;
      }
    }
    return null;
  }
}
