/**
 * Functor class - connects patterns to AST construction.
 * Ported from Java: org.modelingvalue.nelumbo.patterns.Functor
 */

import { List, Map } from 'immutable';
import { TokenType } from '../syntax/TokenType';
import type { Token } from '../syntax/Token';
import type { AstElement } from '../AstElement';
import { Type } from '../Type';
import { Variable } from '../Variable';
import { Node } from '../Node';
import { Pattern } from './Pattern';
import { SequencePattern } from './SequencePattern';
import { ParseState } from '../syntax/ParseState';
import { Predicate } from '../logic/Predicate';

/**
 * Function type for node construction.
 * Returns Node or any subtype (Type, Variable, Predicate, etc.)
 */
export type NodeConstructor<T extends Node = Node> = (
  elements: List<AstElement>,
  args: unknown[],
  functor: Functor
) => T;

/**
 * Functor - connects patterns to AST node construction.
 */
export class Functor extends Node {
  // Cached values
  private _name: string | null = null;
  private _argTypes: List<Type> | null = null;
  private _start: ParseState | null = null;
  private _startPre: ParseState | null = null;
  private _startPost: ParseState | null = null;

  constructor(
    elements: List<AstElement>,
    pattern: Pattern,
    result: Type,
    local: boolean,
    constructorFn: NodeConstructor | null,
    leftPrecedence: number | null
  ) {
    super(Type.FUNCTOR, elements, pattern, result, local, constructorFn, leftPrecedence);
  }

  protected static fromData(data: unknown[], declaration?: Node): Functor {
    const functor = Object.create(Functor.prototype) as Functor;
    (functor as unknown as { _data: unknown[] })._data = data;
    (functor as unknown as { _declaration: Node })._declaration = declaration ?? functor;
    return functor;
  }

  protected struct(data: unknown[], declaration?: Node): Functor {
    return Functor.fromData(data, declaration ?? this.declaration());
  }

  /**
   * Factory methods.
   */
  static of(
    pattern: Pattern,
    result: Type,
    local: boolean,
    constructorFn: NodeConstructor | null,
    leftPrecedence: number | null
  ): Functor {
    return new Functor(List(), pattern, result, local, constructorFn, leftPrecedence);
  }

  static ofWithElements(
    elements: List<AstElement>,
    pattern: Pattern,
    result: Type,
    local: boolean,
    constructorFn: NodeConstructor | null,
    leftPrecedence: number | null
  ): Functor {
    return new Functor(elements, pattern, result, local, constructorFn, leftPrecedence);
  }

  /**
   * Get the pattern.
   */
  pattern(): Pattern {
    return this.get(0) as Pattern;
  }

  /**
   * Get the result type.
   */
  resultType(): Type {
    return this.get(1) as Type;
  }

  /**
   * Check if this functor is local.
   */
  local(): boolean {
    return this.get(2) as boolean;
  }

  /**
   * Get the constructor function.
   */
  constructorFn(): NodeConstructor | null {
    return this.get(3) as NodeConstructor | null;
  }

  /**
   * Get the left precedence.
   */
  leftPrecedence(): number | null {
    return this.get(4) as number | null;
  }

  /**
   * Get the functor name (derived from pattern).
   */
  name(): string {
    if (this._name === null) {
      this._name = this.pattern().name();
    }
    return this._name;
  }

  /**
   * Get the argument types.
   */
  argTypes(): List<Type> {
    if (this._argTypes === null) {
      this._argTypes = this.pattern().argTypes(List());
    }
    return this._argTypes;
  }

  toString(_previous?: TokenType[]): string {
    return this.resultType().name() + '::=' + this.pattern();
  }

  /**
   * Construct a node from parsed elements.
   */
  construct(elements: List<AstElement>, args: unknown[]): Node {
    const fn = this.constructorFn();
    if (fn !== null) {
      return fn(elements, args, this);
    }

    // Default construction - create a generic Node or Predicate
    if (Type.BOOLEAN.isAssignableFrom(this.resultType())) {
      return new Predicate(this, elements, ...args);
    }
    return new Node(this, elements, ...args);
  }

  /**
   * Get the pre-start state (for prefix parsing).
   */
  preStart(): ParseState | null {
    this.start();
    return this._startPre;
  }

  /**
   * Get the post-start state (for infix/postfix parsing).
   */
  postStart(): ParseState | null {
    this.start();
    return this._startPost;
  }

  /**
   * Build and cache the start states.
   */
  start(): ParseState {
    if (this._start === null) {
      const s = this.pattern().parseState(new ParseState(this), this);
      this._startPre = s.pre();

      const post = s.post();
      if (post !== null) {
        const left = this.leftPrecedence();
        const inner = post.leftPrecedence();
        const prec = left ?? inner ?? Number.MAX_SAFE_INTEGER;
        this._startPost = post.setLeftPrecedence(prec);
      }
      this._start = s;
    }
    return this._start;
  }

  setFunctor(functor: Functor): Functor {
    return super.setFunctor(functor) as Functor;
  }

  setAstElements(elements: List<AstElement>): Functor {
    return super.setAstElements(elements) as Functor;
  }

  setBinding(vars: Map<Variable, unknown>, declaration?: Node): Functor {
    let functor = super.setBinding(vars, declaration) as Functor;
    const from = this.astElements();
    let to = this.setBindingList(from, vars);

    // Convert strings to token text patterns
    to = to.map(e => {
      if (typeof e === 'string') {
        return Pattern.t(e);
      }
      return e;
    }) as List<AstElement>;

    return from.equals(to) ? functor : functor.setAstElements(to);
  }

  private setBindingList(list: List<AstElement>, vars: Map<Variable, unknown>): List<AstElement> {
    return list.map(e => {
      if (e instanceof Node && !(e instanceof Type)) {
        return e.setBinding(vars) as AstElement;
      }
      return e;
    });
  }

  resetDeclaration(): Functor {
    return super.resetDeclaration() as Functor;
  }

  /**
   * Extract arguments from AST elements.
   */
  functorArgs(elements: List<AstElement>, typeArgs: Map<Variable, Type>): unknown[] {
    const pattern = this.pattern();
    const args: unknown[] = [];
    const i = pattern.extractArgs(elements, 0, args, false, this, typeArgs);
    if (i < 0) {
      return [];
    }

    // Unwrap single-element lists for sequence patterns
    if (pattern instanceof SequencePattern && args.length === 1 && List.isList(args[0])) {
      return (args[0] as List<unknown>).toArray();
    }
    return args;
  }

  /**
   * Convert arguments to string representation.
   */
  string(args: List<unknown>, previous: TokenType[]): string | null {
    const pattern = this.pattern();
    let argList = args;

    // Wrap args for sequence patterns with multiple arg types
    if (pattern instanceof SequencePattern && this.argTypes().size > 1) {
      argList = List([args]);
    }

    const sb: string[] = [];
    if (pattern.string(argList, 0, sb, previous, false) < 0) {
      return null;
    }
    return sb.join('');
  }

  /**
   * Get the declaration pattern for a token.
   */
  tokenDeclaration(token: Token): Pattern | null {
    return this.pattern().tokenDeclaration(token);
  }

  /**
   * Get the most specific functor between this and another.
   */
  mostSpecific(other: Functor): Functor {
    const thisTypes = this.argTypes();
    const otherTypes = other.argTypes();

    for (let i = 0; i < thisTypes.size && i < otherTypes.size; i++) {
      const thisType = thisTypes.get(i)!;
      const otherType = otherTypes.get(i)!;

      if (!thisType.equals(otherType)) {
        if (thisType.isAssignableFrom(otherType)) {
          return other;
        } else if (otherType.isAssignableFrom(thisType)) {
          return this;
        }
      }
    }

    throw new Error('Non deterministic pattern merge: ' + this + ' <> ' + other);
  }

  /**
   * Initialize this functor in a knowledge base.
   */
  initInKb(knowledgeBase: { register: (f: Functor) => Functor }): Functor {
    return knowledgeBase.register(this);
  }
}
