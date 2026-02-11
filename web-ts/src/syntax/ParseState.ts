/**
 * ParseState - state machine for parsing with transitions and merge logic.
 * @JAVA_REF org.modelingvalue.nelumbo.syntax.ParseState
 */

import { Map, Set } from 'immutable';
import { TokenType } from './TokenType';
import type { Token } from './Token';
import type { AstElement } from '../AstElement';
import { Type } from '../Type';
import { Variable } from '../Variable';
import { Pattern } from '../patterns/Pattern';
import type { Functor } from '../patterns/Functor';
import type { RepetitionPattern } from '../patterns/RepetitionPattern';
import { createParseContext, type ParseContext } from './ParseContext';
import type { PatternResult } from './PatternResult';
import { ParseException } from './ParseException';

/**
 * ParseState - state machine state for parsing.
 */
export class ParseState {
  static readonly EMPTY: ParseState = new ParseState(
    Map<unknown, ParseState>(),
    null,
    null,
    null,
    null,
    Set<RepetitionPattern>(),
    Set<RepetitionPattern>(),
    false
  );

  private readonly _transitions: Map<unknown, ParseState>;
  private readonly _functor: Functor | null;
  private readonly _leftPrecedence: number | null;
  private readonly _innerPrecedence: number | null;
  private readonly _group: string | null;
  private readonly _startRepetitions: Set<RepetitionPattern>;
  private readonly _endRepetitions: Set<RepetitionPattern>;
  private readonly _isKeyword: boolean;

  // Constructor for functor terminal
  constructor(functor: Functor);
  // Constructor for repetition markers
  constructor(startRepetitions: Set<RepetitionPattern>, endRepetitions: Set<RepetitionPattern>);
  // Constructor for token text transition
  constructor(text: string, isKeyword: boolean, next: ParseState);
  // Constructor for token type transition
  constructor(tokenType: TokenType, next: ParseState);
  // Constructor for node type transition
  constructor(nodeType: Type, next: ParseState, innerPrecedence: number | null);
  // Internal constructor
  constructor(
    transitions: Map<unknown, ParseState>,
    functor: Functor | null,
    leftPrecedence: number | null,
    innerPrecedence: number | null,
    group: string | null,
    startRepetitions: Set<RepetitionPattern>,
    endRepetitions: Set<RepetitionPattern>,
    isKeyword: boolean
  );

  constructor(
    arg1: Functor | Set<RepetitionPattern> | string | TokenType | Type | Map<unknown, ParseState>,
    arg2?: Set<RepetitionPattern> | boolean | ParseState | Functor | null,
    arg3?: ParseState | number | null,
    arg4?: number | null,
    arg5?: string | null,
    arg6?: Set<RepetitionPattern>,
    arg7?: Set<RepetitionPattern>,
    arg8?: boolean
  ) {
    if (Map.isMap(arg1)) {
      // Internal constructor
      this._transitions = arg1;
      this._functor = arg2 as Functor | null;
      this._leftPrecedence = arg3 as number | null;
      this._innerPrecedence = arg4 as number | null;
      this._group = arg5 as string | null;
      this._startRepetitions = arg6 as Set<RepetitionPattern>;
      this._endRepetitions = arg7 as Set<RepetitionPattern>;
      this._isKeyword = arg8 as boolean;
    } else if (typeof arg1 === 'string') {
      // Token text transition
      const text = arg1;
      const isKeyword = arg2 as boolean;
      const next = arg3 as ParseState;
      this._transitions = Map<unknown, ParseState>([[text, isKeyword ? next.setIsKeyword() : next]]);
      this._functor = null;
      this._leftPrecedence = null;
      this._innerPrecedence = null;
      this._group = null;
      this._startRepetitions = Set<RepetitionPattern>();
      this._endRepetitions = Set<RepetitionPattern>();
      this._isKeyword = false;
    } else if (arg1 instanceof TokenType) {
      // Token type transition
      const tokenType = arg1;
      const next = arg2 as ParseState;
      this._transitions = Map<unknown, ParseState>([[tokenType, next]]);
      this._functor = null;
      this._leftPrecedence = null;
      this._innerPrecedence = null;
      this._group = null;
      this._startRepetitions = Set<RepetitionPattern>();
      this._endRepetitions = Set<RepetitionPattern>();
      this._isKeyword = false;
    } else if (arg1 instanceof Type) {
      // Node type transition
      const nodeType = arg1;
      const next = arg2 as ParseState;
      const innerPrecedence = arg3 as number | null;
      this._transitions = Map<unknown, ParseState>([[nodeType, next]]);
      this._functor = null;
      this._leftPrecedence = null;
      this._innerPrecedence = innerPrecedence;
      this._group = nodeType.group();
      this._startRepetitions = Set<RepetitionPattern>();
      this._endRepetitions = Set<RepetitionPattern>();
      this._isKeyword = false;
    } else if (Set.isSet(arg1)) {
      // Repetition markers
      this._transitions = Map<unknown, ParseState>();
      this._functor = null;
      this._leftPrecedence = null;
      this._innerPrecedence = null;
      this._group = null;
      this._startRepetitions = arg1 as Set<RepetitionPattern>;
      this._endRepetitions = arg2 as Set<RepetitionPattern>;
      this._isKeyword = false;
    } else {
      // Functor terminal
      const functor = arg1 as Functor;
      this._transitions = Map<unknown, ParseState>();
      this._functor = functor;
      this._leftPrecedence = null;
      this._innerPrecedence = null;
      this._group = null;
      this._startRepetitions = Set<RepetitionPattern>();
      this._endRepetitions = Set<RepetitionPattern>();
      this._isKeyword = false;
    }
  }

  transitions(): Map<unknown, ParseState> {
    return this._transitions;
  }

  functor(): Functor | null {
    return this._functor;
  }

  leftPrecedence(): number | null {
    return this._leftPrecedence;
  }

  innerPrecedence(): number | null {
    return this._innerPrecedence;
  }

  group(): string | null {
    return this._group;
  }

  startRepetitions(): Set<RepetitionPattern> {
    return this._startRepetitions;
  }

  endRepetitions(): Set<RepetitionPattern> {
    return this._endRepetitions;
  }

  isKeyword(): boolean {
    return this._isKeyword;
  }

  /**
   * Get the pre-parse state (non-type transitions).
   */
  pre(): ParseState | null {
    const t = this._transitions.filter((_, key) => !(key instanceof Type));
    if (t.isEmpty()) {
      return null;
    }
    return new ParseState(
      t,
      this._functor,
      null,
      null,
      this._group,
      this._startRepetitions,
      this._endRepetitions,
      this._isKeyword
    );
  }

  /**
   * Get the post-parse state (type transitions only).
   */
  post(): ParseState | null {
    const t = this._transitions.filter((_, key) => key instanceof Type);
    if (t.isEmpty()) {
      return null;
    }
    return new ParseState(
      t,
      this._functor,
      this._innerPrecedence,
      null,
      this._group,
      this._startRepetitions,
      this._endRepetitions,
      this._isKeyword
    );
  }

  /**
   * Set the left precedence on this state and all transitions.
   */
  setLeftPrecedence(leftPrecedence: number): ParseState {
    const t = this._transitions.map(v => v.setLeftPrecedence(leftPrecedence));
    return new ParseState(
      t,
      this._functor,
      leftPrecedence,
      this._innerPrecedence,
      this._group,
      this._startRepetitions,
      this._endRepetitions,
      this._isKeyword
    );
  }

  private setIsKeyword(): ParseState {
    return new ParseState(
      this._transitions,
      this._functor,
      this._leftPrecedence,
      this._innerPrecedence,
      this._group,
      this._startRepetitions,
      this._endRepetitions,
      true
    );
  }

  /**
   * Merge two parse states.
   */
  merge(state: ParseState | null): ParseState {
    if (state === null) {
      return this;
    }

    // Merge transitions with special handling for Type keys
    let transitions = this._transitions.mergeWith(
      (a, b) => a.merge(b),
      state._transitions
    );

    // Merge type hierarchy transitions
    for (const key of transitions.keys()) {
      if (key instanceof Type) {
        const subType = key;
        for (const superType of subType.allSupers()) {
          if (!superType.equals(subType)) {
            const superState = transitions.get(superType);
            if (superState !== undefined) {
              const subState = transitions.get(subType);
              if (subState !== undefined) {
                const mergedState = subState.merge(superState);
                transitions = transitions.set(subType, mergedState);
              }
            }
          }
        }
      }
    }

    return new ParseState(
      transitions,
      this.functorMerge(state),
      this.leftPrecedenceMerge(state),
      this.elementMerge(this._innerPrecedence, state._innerPrecedence),
      this.elementMerge(this._group, state._group),
      this._startRepetitions.union(state._startRepetitions),
      this._endRepetitions.union(state._endRepetitions),
      this.booleanMerge(this._isKeyword, state._isKeyword)
    );
  }

  private booleanMerge(b1: boolean, b2: boolean): boolean {
    // In Java, elementMerge with Booleans throws if both are non-null and different
    // For primitive booleans (always "non-null"), this means throw if they differ
    if (b1 !== b2) {
      throw new Error('Non deterministic pattern merge: ' + b1 + ' <> ' + b2);
    }
    return b1;
  }

  private functorMerge(state: ParseState): Functor | null {
    if (this._functor === null) {
      return state._functor;
    }
    if (state._functor === null) {
      return this._functor;
    }
    if (this._functor.equals(state._functor)) {
      return this._functor;
    }
    return this._functor.mostSpecific(state._functor);
  }

  private leftPrecedenceMerge(state: ParseState): number | null {
    if (this._functor !== null && state._functor === null) {
      return this._leftPrecedence;
    }
    if (state._functor !== null && this._functor === null) {
      return state._leftPrecedence;
    }
    if (this._leftPrecedence === state._leftPrecedence) {
      return this._leftPrecedence;
    }
    return null;
  }

  private elementMerge<T>(t1: T | null, t2: T | null): T | null {
    if (t1 !== null && t2 !== null && t1 !== t2) {
      throw new Error('Non deterministic pattern merge: ' + t1 + ' <> ' + t2);
    }
    return t1 === null ? t2 : t1;
  }

  /**
   * Parse from this state.
   */
  parse(
    token: Token | null,
    result: PatternResult,
    outerRepetitions: Map<RepetitionPattern, ParseState>,
    pre: boolean
  ): boolean {
    const ctx = result.context();

    // Avoid infinite loops
    if (ctx.state() === this && ctx.token() === token) {
      return false;
    }

    // Handle repetition start in pre-parse
    if (pre && !result.isEmpty() && !this._startRepetitions.isEmpty()) {
      result.endPreParse(this, token, this._leftPrecedence);
      return true;
    }

    // Track inner repetitions
    let innerRepetitions = outerRepetitions;
    for (const start of this._startRepetitions) {
      innerRepetitions = innerRepetitions.set(start, this);
    }

    let nrOfExceptions: number = 0;
    let iterationCount = 0;
    const MAX_ITERATIONS = 10000;
    do {
      iterationCount++;
      if (iterationCount > MAX_ITERATIONS) {
        console.error('ParseState.parse: Maximum iterations exceeded, breaking to prevent infinite loop');
        break;
      }
      nrOfExceptions = result.exceptions().size;

      // Check if token matching succeeds
      const tokenMatched = token !== null && this.token(token, result, ctx, innerRepetitions, pre, true);
      if (!tokenMatched) {
        if (pre && this._group !== null) {
          result.endPreParse(this, token, this._leftPrecedence);
          return true;
        } else if (!pre && token !== null && this.outerEnd(token, result, ctx, outerRepetitions)) {
          result.endPostParse(this._functor!, token, this._leftPrecedence);
        } else if (token === null || !this.node(token, result, innerRepetitions, pre)) {
          // Error recovery: skip to end of line
          if (result.exceptions().size > nrOfExceptions &&
              token !== null &&
              token.type !== TokenType.ENDOFFILE) {
            // Skip tokens until we reach end of line
            while (token !== null && !Pattern.isEndOfLine(token)) {
              token = token.next;
            }
            // Advance past the end-of-line marker to prevent infinite loop
            if (token !== null && token.type !== TokenType.ENDOFFILE) {
              token = token.next;
              if (token !== null && token.type !== TokenType.ENDOFFILE) {
                continue;
              }
            }
          }
          break;
        }
      }

      // Check if we should continue repetition
      if (!this._startRepetitions.some(r => result.endRepetitions().has(r))) {
        return true;
      }

      token = result.nextToken();
    } while (true);

    // Check for end of repetition
    if (this._endRepetitions.some(r => outerRepetitions.has(r))) {
      result.endRepetition(this._endRepetitions, token, 1);
      return true;
    }

    // Handle final state
    if (result.functor() === null) {
      if (this._functor === null || result.hasException()) {
        if ((!pre || !result.isEmpty()) && nrOfExceptions === result.exceptions().size) {
          const message = 'Unexpected token ' + token + ', expected ' + this.expectedTokens(ctx);
          if (token !== null) {
            result.addException(ParseException.fromToken(message, token));
          } else {
            result.addException(new ParseException(message, 0, 0, 0, 0, ''));
          }
        }
        return false;
      }
      result.endPostParse(this._functor, token, this._leftPrecedence);
    }

    return true;
  }

  private expectedTokens(ctx: ParseContext): string {
    const states = this.outerStates(ctx).add(this);
    const tokens: string[] = [];

    for (const s of states) {
      for (const k of s.transitions().keys()) {
        if (typeof k === 'string') {
          tokens.push("'" + k + "'");
        } else if (k instanceof TokenType) {
          tokens.push(k.name);
        }
      }
    }

    return tokens.join(',');
  }

  private outerStates(ctx: ParseContext): Set<ParseState> {
    let result = Set<ParseState>();
    if (this._functor !== null) {
      const type = this._functor.resultType();
      for (let pc: ParseContext | null = ctx; pc !== null && pc.state() !== null; pc = pc.outer()) {
        for (const sup of type.allSupers()) {
          const next = pc.state()!.transitions().get(sup);
          if (next !== undefined) {
            result = result.add(next);
          }
        }
      }
    }
    return result;
  }

  private token(
    token: Token,
    result: PatternResult,
    ctx: ParseContext,
    repetitions: Map<RepetitionPattern, ParseState> | null,
    pre: boolean,
    matchType: boolean
  ): boolean {
    if (this._transitions.isEmpty()) {
      return false;
    }

    let element: AstElement | null = null;
    const type = token.type;
    const text = token.text;

    // Try exact text match
    let next = this._transitions.get(text);
    if (next !== undefined) {
      element = token;
      token.setTextMatch(next.isKeyword());
    }

    // Handle negative numbers
    if (next === undefined && this.isNumeric(type) && text.startsWith('-') && !this._transitions.has(type)) {
      next = this._transitions.get('-');
      if (next !== undefined) {
        token = result.addSplit(token, token.split(1));
        element = token;
        token.setTextMatch(next.isKeyword());
      }
    }

    // Handle operator splitting
    if (next === undefined && type === TokenType.OPERATOR) {
      for (let i = text.length - 1; i > 0; i--) {
        const key = text.substring(0, i);
        next = this._transitions.get(key);
        if (next !== undefined) {
          token = result.addSplit(token, token.split(i));
          element = token;
          token.setTextMatch(next.isKeyword());
          break;
        }
      }
    }

    // Handle newline
    if (next === undefined) {
      next = this._transitions.get(TokenType.NEWLINE);
      if (next !== undefined) {
        if (Pattern.isEndOfLine(token)) {
          for (let prev = token.previousAll; prev !== null && prev !== token.previous; prev = prev!.previousAll) {
            if (prev!.type === TokenType.NEWLINE) {
              element = prev;
              break;
            }
          }
          token = token.previous!;
        } else {
          next = undefined;
        }
      }
    }

    // Handle variable lookup
    if (next === undefined && type === TokenType.NAME) {
      const variable = result.parser().variable(token, ctx);
      if (variable !== null) {
        const tt = variable.type().tokenType();
        next = tt !== null ? this._transitions.get(tt) as ParseState | undefined : undefined;
        if (next !== undefined) {
          element = variable;
        } else {
          return false;
        }
      }
    }

    // Handle token type match
    if (next === undefined && matchType) {
      next = this._transitions.get(type);
      if (next !== undefined) {
        if (this._group !== null && type.isVariableContent()) {
          const groupState = result.parser().groupState(this._group);
          if (groupState !== null && groupState.token(token, result, ctx.outer()!, null, true, false)) {
            return false;
          }
        }
        element = token;
      }
    }

    if (next !== undefined) {
      if (repetitions === null) {
        return true;
      }
      if (element !== null) {
        result.add(element);
      }
      token.setState(next);
      if (next.parse(token.next, result, repetitions, pre)) {
        return true;
      }
    }

    return false;
  }

  private outerEnd(
    token: Token,
    result: PatternResult,
    ctx: ParseContext,
    repetitions: Map<RepetitionPattern, ParseState>
  ): boolean {
    if (this._functor !== null) {
      for (const [r, state] of repetitions) {
        if (this._endRepetitions.has(r) && state.token(token, result, ctx, null, true, true)) {
          return false;
        }
      }
      const type = this._functor.resultType();
      for (let pc: ParseContext | null = ctx; pc !== null && pc.state() !== null; pc = pc.outer()) {
        for (const sup of type.allSupers()) {
          const next = pc.state()!.transitions().get(sup);
          if (next !== undefined && next.token(token, result, ctx.outer()!, null, true, true)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private isNumeric(type: TokenType): boolean {
    return type === TokenType.NUMBER || type === TokenType.DECIMAL;
  }

  private node(
    token: Token,
    result: PatternResult,
    repetitions: Map<RepetitionPattern, ParseState>,
    pre: boolean
  ): boolean {
    if (this._group === null) {
      return false;
    }

    let currentToken = token;
    const nextToken = token.next;

    // Handle negative number merging
    if (nextToken !== null &&
        token.text === '-' &&
        this.isNumeric(nextToken.type) &&
        !nextToken.text.startsWith('-')) {
      currentToken = result.addMerge(token, nextToken.prepend('-'));
    }

    const inner = this._innerPrecedence;
    const node = result.parser().parseNode(
      currentToken,
      createParseContext(
        this,
        currentToken,
        this._group,
        inner ?? Number.MIN_SAFE_INTEGER,
        result.context()
      )
    );

    if (node !== null) {
      result.add(node);

      // Handle Variable type
      const isVariable = node instanceof Variable;
      if (isVariable) {
        const next = this._transitions.get(Type.VARIABLE);
        if (next !== undefined) {
          if (next.parse((node as unknown as Variable).nextToken(), result, repetitions, pre)) {
            return true;
          }
        }
      }

      // Handle normal type matching
      for (const sup of node.type().allSupers()) {
        const next = this._transitions.get(sup);
        if (next !== undefined) {
          const nextToken = node.nextToken();
          if (next.parse(nextToken, result, repetitions, pre)) {
            return true;
          } else {
            break;
          }
        }
      }

      // Handle type variable matching
      let foundTypeEntry: [ParseState, unknown] | undefined;
      for (const entry of this._transitions.entries()) {
        const [entryKey, entryState] = entry;
        if (entryKey instanceof Type && entryKey.variable() !== null) {
          foundTypeEntry = [entryState, entryKey];
          break;
        }
      }
      if (foundTypeEntry !== undefined) {
        const [next, keyType] = foundTypeEntry;
        const variable = (keyType as Type).variable()!;
        const resolvedType = result.getTypeArg(variable);

        if (resolvedType !== undefined) {
          if (resolvedType.isAssignableFrom(node.type()) && next.parse(node.nextToken(), result, repetitions, pre)) {
            return true;
          } else {
            result.addException(
              ParseException.fromElements(
                'Node ' + node + ' of unexpected type ' + node.type() + ', expected ' + resolvedType,
                node
              )
            );
            return true;
          }
        } else {
          result.putTypeArg(variable, node.type());
          if (next.parse(node.nextToken(), result, repetitions, pre)) {
            return true;
          }
        }
      }

      result.addException(
        ParseException.fromElements(
          'Node ' + node + ' of unexpected type ' + node.type() + ', expected ' + this.expectedTypes(),
          node
        )
      );
      return true;
    }

    return false;
  }

  private expectedTypes(): string {
    const types: string[] = [];
    for (const key of this._transitions.keys()) {
      if (key instanceof Type) {
        types.push(String(key));
      }
    }
    return types.join(' or ');
  }

  toString(): string {
    const keys: string[] = [];
    for (const key of this._transitions.keys()) {
      keys.push(String(key));
    }
    return '{' + keys.join(', ') + '}';
  }
}
