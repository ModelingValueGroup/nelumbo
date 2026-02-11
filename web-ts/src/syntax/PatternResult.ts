/**
 * PatternResult - intermediate pattern match results.
 * @JAVA_REF org.modelingvalue.nelumbo.syntax.PatternResult
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
import { Transform } from '../Transform';

/**
 * PatternResult - holds the intermediate result of pattern matching.
 * @JAVA_REF org.modelingvalue.nelumbo.syntax.PatternResult
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

  constructor(parser: Parser, context: ParseContext) {
    this._parser = parser;
    this._context = context;
  }

  // @JAVA_REF PatternResult.addSplit(Token, Token)
  addSplit(original: Token, split: Token): Token {
    this._splitted.push([original, split]);
    return split;
  }

  // @JAVA_REF PatternResult.addMerge(Token, Token)
  addMerge(original: Token, merge: Token): Token {
    this._merged.push([original, merge]);
    return merge;
  }

  // @JAVA_REF PatternResult.parser()
  parser(): Parser {
    return this._parser;
  }

  // @JAVA_REF PatternResult.context()
  context(): ParseContext {
    return this._context;
  }

  // @JAVA_REF PatternResult.functor()
  functor(): Functor | null {
    return this._functor;
  }

  // @JAVA_REF PatternResult.state()
  state(): ParseState | null {
    return this._state;
  }

  // @JAVA_REF PatternResult.endRepetitions()
  endRepetitions(): Set<RepetitionPattern> {
    return this._endRepetitions;
  }

  // @JAVA_REF PatternResult.nextToken()
  nextToken(): Token | null {
    return this._nextToken;
  }

  // @JAVA_REF PatternResult.leftPrecedence()
  leftPrecedence(): number | null {
    return this._leftPrecedence;
  }

  // @JAVA_REF PatternResult.endPostParse(Functor, Token, Integer)
  endPostParse(functor: Functor, nextToken: Token | null, leftPrecedence: number | null): void {
    this._endRepetitions = Set();
    this._functor = functor;
    this._nextToken = nextToken;
    this._leftPrecedence = leftPrecedence;
    // assert (functor != null)
    // assert (!hasLeft || leftPrecedence != null)
  }

  // @JAVA_REF PatternResult.endPreParse(ParseState, Token, Integer)
  endPreParse(state: ParseState, nextToken: Token | null, leftPrecedence: number | null): void {
    this._state = state;
    this._nextToken = nextToken;
    this._leftPrecedence = leftPrecedence;
    // assert (!hasLeft || leftPrecedence != null)
  }

  // @JAVA_REF PatternResult.endRepetition(Set, Token)
  endRepetition(endRepetitions: Set<RepetitionPattern>, nextToken: Token | null): void {
    this._endRepetitions = endRepetitions;
    this._nextToken = nextToken;
  }

  // @JAVA_REF PatternResult.startRepetition()
  startRepetition(): void {
    this._endRepetitions = Set();
  }

  // @JAVA_REF PatternResult.elements()
  elements(): List<AstElement> {
    return List(this._elements);
  }

  // @JAVA_REF PatternResult.hasLeft()
  hasLeft(): boolean {
    return this._hasLeft;
  }

  // @JAVA_REF PatternResult.left(AstElement)
  left(element: AstElement): void {
    this._elements.push(element);
    this._hasLeft = true;
  }

  // @JAVA_REF PatternResult.add(AstElement)
  add(element: AstElement): void {
    this._elements.push(element);
  }

  // @JAVA_REF PatternResult.removeLast()
  removeLast(): void {
    this._elements.pop();
  }

  // @JAVA_REF PatternResult.nrOfExceptions() (from ParseExceptionHandler)
  nrOfExceptions(): number {
    return this.exceptions().size;
  }

  // @JAVA_REF PatternResult.postParse(ParseContext)
  postParse(_ctx: ParseContext): Node | null {
    const next = this._state;
    if (next !== null) {
      this._state = null;
      next.parse(this._nextToken, this, Map(), false);
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
      // Circular reference check
      if (this._hasLeft && args.length === 1 && args[0] instanceof Node) {
        const arg = args[0] as Node;
        if (node.functor() !== null && node.functor()!.equals(arg.functor())) {
          this.addException(ParseException.fromElements('Circular object construction, caused by ' + functor, node));
          return null;
        }
      }
      if (Type.ROOT.isAssignableFrom(node.type())) {
        const kb = this._parser.knowledgeBase();
        // Java virtual dispatch: node.init(kb)
        // Transform.init → addTransform only (no transform application)
        // Functor.init → register only (already done via createFunctor)
        // Other subclasses have their own init overrides
        // Only base Node.init applies transforms
        if (node instanceof Transform) {
          node.initInKb(kb);
        } else if (node instanceof Functor) {
          // Functor.init in Java calls register() — already done via createFunctor
          // Don't apply transforms to Functor nodes
        } else {
          // Base Node.init(kb) - apply transforms
          for (const transform of kb.getTransforms(node)) {
            transform.rewrite(transform.source(), node, kb);
          }
        }
      }
      return node;
    }
    return null;
  }

  // @JAVA_REF PatternResult.toString()
  toString(): string {
    return this.elements().toArray().toString();
  }

  // @JAVA_REF PatternResult.addException(ParseException)
  addException(exception: ParseException): void {
    this._parser.addException(exception);
  }

  // @JAVA_REF PatternResult.exceptions()
  exceptions(): List<ParseException> {
    return this._parser.exceptions();
  }

  // @JAVA_REF PatternResult.putTypeArg(Variable, Type)
  putTypeArg(arg: Variable, val: Type): void {
    this._typeArgs = this._typeArgs.set(arg, val);
  }

  // @JAVA_REF PatternResult.getTypeArg(Variable)
  getTypeArg(arg: Variable): Type | undefined {
    return this._typeArgs.get(arg);
  }
}
