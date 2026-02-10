/**
 * PatternResult - intermediate pattern match results.
 * Ported from Java: org.modelingvalue.nelumbo.syntax.PatternResult
 */

import { List, Map, Set } from 'immutable';
import type { Token } from './Token';
import type { AstElement } from '../AstElement';
import { Type } from '../Type';
import { Variable } from '../Variable';
import { Node } from '../Node';
import { Functor } from '../patterns/Functor';
import { RepetitionPattern } from '../patterns/RepetitionPattern';
import type { ParseContext } from './ParseContext';
import { ParseState } from './ParseState';
import { ParseException } from './ParseException';
import type { Parser } from './Parser';
import type { ParseExceptionHandler } from '../KnowledgeBase';

/**
 * PatternResult - holds the intermediate result of pattern matching.
 */
export class PatternResult implements ParseExceptionHandler {
  private _elements: AstElement[] = [];
  private readonly _parser: Parser;
  private readonly _context: ParseContext;
  private _splitted: Array<[Token, Token]> = [];
  private _merged: Array<[Token, Token]> = [];
  private _typeArgs: Map<Variable, Type> = Map();

  private _functor: Functor | null = null;
  private _state: ParseState | null = null;
  private _leftPrecedence: number | null = null;
  private _endRepetitions: Set<RepetitionPattern> = Set();
  private _nextToken: Token | null = null;
  private _hasLeft: boolean = false;
  private _hasException: boolean = false;

  constructor(parser: Parser, context: ParseContext) {
    this._parser = parser;
    this._context = context;
  }

  addSplit(original: Token, split: Token): Token {
    this._splitted.push([original, split]);
    return split;
  }

  addMerge(original: Token, merge: Token): Token {
    this._merged.push([original, merge]);
    return merge;
  }

  parser(): Parser {
    return this._parser;
  }

  context(): ParseContext {
    return this._context;
  }

  functor(): Functor | null {
    return this._functor;
  }

  state(): ParseState | null {
    return this._state;
  }

  endRepetitions(): Set<RepetitionPattern> {
    return this._endRepetitions;
  }

  nextToken(): Token | null {
    return this._nextToken;
  }

  leftPrecedence(): number | null {
    return this._leftPrecedence;
  }

  endPostParse(functor: Functor, nextToken: Token | null, leftPrecedence: number | null): void {
    this._functor = functor;
    this._nextToken = nextToken;
    this._leftPrecedence = leftPrecedence;
  }

  endPreParse(state: ParseState, nextToken: Token | null, leftPrecedence: number | null): void {
    this._state = state;
    this._nextToken = nextToken;
    this._leftPrecedence = leftPrecedence;
  }

  endRepetition(endRepetitions: Set<RepetitionPattern>, nextToken: Token | null, _i: number): void {
    this._endRepetitions = endRepetitions;
    this._nextToken = nextToken;
  }

  elements(): List<AstElement> {
    return List(this._elements);
  }

  isEmpty(): boolean {
    const size = this._elements.length;
    return (this._hasLeft ? size - 1 : size) === 0;
  }

  hasLeft(): boolean {
    return this._hasLeft;
  }

  left(element: AstElement): void {
    this._elements.push(element);
    this._hasLeft = true;
  }

  add(element: AstElement): void {
    this._elements.push(element);
    this._endRepetitions = Set();
  }

  postParse(_ctx: ParseContext): Node | null {
    if (this._state !== null) {
      this._state.parse(this._nextToken, this, Map(), false);
    }
    if (this._functor !== null) {
      for (const [original, split] of this._splitted) {
        original.connect(split);
      }
      for (const [original, merge] of this._merged) {
        original.merge(merge);
      }
      const elements = this.elements();
      const ta = this._typeArgs;
      const args = this._functor.functorArgs(elements, ta);
      let functor = this._functor;
      if (!ta.isEmpty()) {
        functor = functor.setBinding(ta as Map<Variable, unknown>) as Functor;
      }
      const node = functor.construct(elements, args);
      if (Type.ROOT.isAssignableFrom(node.type())) {
        // Root nodes would be initialized here in a full implementation
      }
      return node;
    }
    return null;
  }

  toString(): string {
    return this.elements().toArray().toString();
  }

  addException(exception: ParseException): void {
    this._hasException = true;
    this._parser.addException(exception);
  }

  exceptions(): List<ParseException> {
    return this._parser.exceptions();
  }

  hasException(): boolean {
    return this._hasException;
  }

  putTypeArg(arg: Variable, val: Type): void {
    this._typeArgs = this._typeArgs.set(arg, val);
  }

  getTypeArg(arg: Variable): Type | undefined {
    return this._typeArgs.get(arg);
  }
}
