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

// @JAVA_REF ParseState.Direction
enum Direction {
  outer = 'outer',
  repeat = 'repeat',
  node = 'node',
  token = 'token',
}

// @JAVA_REF ParseState.TokenState
interface TokenState {
  token: Token | null;
  state: ParseState;
}

/**
 * ParseState - state machine state for parsing.
 * @JAVA_REF org.modelingvalue.nelumbo.syntax.ParseState
 */
export class ParseState {
  static readonly EMPTY: ParseState = new ParseState(
    Map<string, ParseState>(),
    Map<TokenType, ParseState>(),
    Map<Type, ParseState>(),
    null,
    null,
    null,
    null,
    Set<RepetitionPattern>(),
    Set<RepetitionPattern>(),
    false
  );

  // @JAVA_REF ParseState fields
  private readonly _tokenTexts: Map<string, ParseState>;
  private readonly _tokenTypes: Map<TokenType, ParseState>;
  private readonly _nodeTypes: Map<Type, ParseState>;
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
    tokenTexts: Map<string, ParseState>,
    tokenTypes: Map<TokenType, ParseState>,
    nodeTypes: Map<Type, ParseState>,
    functor: Functor | null,
    leftPrecedence: number | null,
    innerPrecedence: number | null,
    group: string | null,
    startRepetitions: Set<RepetitionPattern>,
    endRepetitions: Set<RepetitionPattern>,
    isKeyword: boolean
  );

  constructor(
    arg1: Functor | Set<RepetitionPattern> | string | TokenType | Type | Map<string, ParseState>,
    arg2?: Set<RepetitionPattern> | boolean | ParseState | Map<TokenType, ParseState> | null,
    arg3?: ParseState | number | Map<Type, ParseState> | null,
    arg4?: Functor | number | null,
    arg5?: number | string | null,
    arg6?: number | Set<RepetitionPattern> | null,
    arg7?: string | Set<RepetitionPattern> | null,
    arg8?: Set<RepetitionPattern>,
    arg9?: Set<RepetitionPattern>,
    arg10?: boolean
  ) {
    if (Map.isMap(arg1) && Map.isMap(arg2)) {
      // Internal constructor (10 args)
      this._tokenTexts = arg1 as Map<string, ParseState>;
      this._tokenTypes = arg2 as Map<TokenType, ParseState>;
      this._nodeTypes = arg3 as Map<Type, ParseState>;
      this._functor = arg4 as Functor | null;
      this._leftPrecedence = arg5 as number | null;
      this._innerPrecedence = arg6 as number | null;
      this._group = arg7 as string | null;
      this._startRepetitions = arg8 as Set<RepetitionPattern>;
      this._endRepetitions = arg9 as Set<RepetitionPattern>;
      this._isKeyword = arg10 as boolean;
    } else if (typeof arg1 === 'string') {
      // Token text transition: (text, isKeyword, next)
      const text = arg1;
      const isKeyword = arg2 as boolean;
      const next = arg3 as ParseState;
      this._tokenTexts = Map<string, ParseState>([[text, isKeyword ? next.setIsKeyword() : next]]);
      this._tokenTypes = Map<TokenType, ParseState>();
      this._nodeTypes = Map<Type, ParseState>();
      this._functor = null;
      this._leftPrecedence = null;
      this._innerPrecedence = null;
      this._group = null;
      this._startRepetitions = Set<RepetitionPattern>();
      this._endRepetitions = Set<RepetitionPattern>();
      this._isKeyword = false;
    } else if (arg1 instanceof TokenType) {
      // Token type transition: (tokenType, next)
      const tokenType = arg1;
      const next = arg2 as ParseState;
      this._tokenTexts = Map<string, ParseState>();
      this._tokenTypes = Map<TokenType, ParseState>([[tokenType, next]]);
      this._nodeTypes = Map<Type, ParseState>();
      this._functor = null;
      this._leftPrecedence = null;
      this._innerPrecedence = null;
      this._group = null;
      this._startRepetitions = Set<RepetitionPattern>();
      this._endRepetitions = Set<RepetitionPattern>();
      this._isKeyword = false;
    } else if (arg1 instanceof Type) {
      // Node type transition: (nodeType, next, innerPrecedence)
      const nodeType = arg1;
      const next = arg2 as ParseState;
      const innerPrecedence = arg3 as number | null;
      this._tokenTexts = Map<string, ParseState>();
      this._tokenTypes = Map<TokenType, ParseState>();
      this._nodeTypes = Map<Type, ParseState>([[nodeType, next]]);
      this._functor = null;
      this._leftPrecedence = null;
      this._innerPrecedence = innerPrecedence;
      this._group = nodeType.group();
      this._startRepetitions = Set<RepetitionPattern>();
      this._endRepetitions = Set<RepetitionPattern>();
      this._isKeyword = false;
    } else if (Set.isSet(arg1)) {
      // Repetition markers: (startReps, endReps)
      this._tokenTexts = Map<string, ParseState>();
      this._tokenTypes = Map<TokenType, ParseState>();
      this._nodeTypes = Map<Type, ParseState>();
      this._functor = null;
      this._leftPrecedence = null;
      this._innerPrecedence = null;
      this._group = null;
      this._startRepetitions = arg1 as Set<RepetitionPattern>;
      this._endRepetitions = arg2 as Set<RepetitionPattern>;
      this._isKeyword = false;
    } else {
      // Functor terminal: (functor)
      const functor = arg1 as Functor;
      this._tokenTexts = Map<string, ParseState>();
      this._tokenTypes = Map<TokenType, ParseState>();
      this._nodeTypes = Map<Type, ParseState>();
      this._functor = functor;
      this._leftPrecedence = null;
      this._innerPrecedence = null;
      this._group = null;
      this._startRepetitions = Set<RepetitionPattern>();
      this._endRepetitions = Set<RepetitionPattern>();
      this._isKeyword = false;
    }
  }

  // @JAVA_REF ParseState.tokenTexts()
  tokenTexts(): Map<string, ParseState> {
    return this._tokenTexts;
  }

  // @JAVA_REF ParseState.tokenTypes()
  tokenTypes(): Map<TokenType, ParseState> {
    return this._tokenTypes;
  }

  // @JAVA_REF ParseState.nodeTypes()
  nodeTypes(): Map<Type, ParseState> {
    return this._nodeTypes;
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

  // @JAVA_REF ParseState.pre()
  pre(): ParseState | null {
    if (this._tokenTexts.isEmpty() && this._tokenTypes.isEmpty()) {
      return null;
    }
    return new ParseState(
      this._tokenTexts,
      this._tokenTypes,
      Map<Type, ParseState>(),
      this._functor,
      null,
      null,
      this._group,
      this._startRepetitions,
      this._endRepetitions,
      this._isKeyword
    );
  }

  // @JAVA_REF ParseState.post()
  post(): ParseState | null {
    if (this._nodeTypes.isEmpty()) {
      return null;
    }
    return new ParseState(
      Map<string, ParseState>(),
      Map<TokenType, ParseState>(),
      this._nodeTypes,
      this._functor,
      this._innerPrecedence,
      null,
      this._group,
      this._startRepetitions,
      this._endRepetitions,
      this._isKeyword
    );
  }

  // @JAVA_REF ParseState.setLeftPrecedence(Integer)
  setLeftPrecedence(leftPrecedence: number): ParseState {
    const a = this._tokenTexts.map(v => v.setLeftPrecedence(leftPrecedence));
    const b = this._tokenTypes.map(v => v.setLeftPrecedence(leftPrecedence));
    const c = this._nodeTypes.map(v => v.setLeftPrecedence(leftPrecedence));
    return new ParseState(
      a,
      b,
      c,
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
      this._tokenTexts,
      this._tokenTypes,
      this._nodeTypes,
      this._functor,
      this._leftPrecedence,
      this._innerPrecedence,
      this._group,
      this._startRepetitions,
      this._endRepetitions,
      true
    );
  }

  // @JAVA_REF ParseState.tokenState(Token)
  tokenState(token: Token | null): TokenState {
    return { token, state: this };
  }

  // @JAVA_REF ParseState.parse(Token, PatternResult, Map, boolean)
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
    if (pre && !this._startRepetitions.isEmpty()) {
      result.endPreParse(this, token, this._leftPrecedence);
      return true;
    }

    const parser = result.parser();

    // Track inner repetitions
    let innerRepetitions = outerRepetitions;
    for (const start of this._startRepetitions) {
      innerRepetitions = innerRepetitions.set(start, this);
    }

    do {
      const nrOfExceptions = result.nrOfExceptions();

      const direction = this.direction(token, parser, outerRepetitions, ctx);

      if (direction === Direction.outer) {
        result.endPostParse(this._functor!, token, this._leftPrecedence);
        return true;
      }

      if (direction === Direction.repeat) {
        result.endRepetition(this._endRepetitions, token);
        return true;
      }

      let next: TokenState | null = null;

      if (direction === Direction.node) {
        if (pre && !this._nodeTypes.isEmpty()) {
          result.endPreParse(this, token, this._leftPrecedence);
          return true;
        }
        next = this.nodeNext(token, result);
      }

      if (direction === Direction.token) {
        next = this.tokenNext(token, parser, ctx, result);
      }

      if (direction === null) {
        next = this.tokenNext(token, parser, ctx, result);
        if (next === null) {
          if (pre && !this._nodeTypes.isEmpty()) {
            result.endPreParse(this, token, this._leftPrecedence);
            return true;
          }
          next = this.nodeNext(token, result);
        }
        if (next === null && this._endRepetitions.some(r => outerRepetitions.has(r))) {
          result.endRepetition(this._endRepetitions, token);
          return true;
        }
      }

      if (next !== null && next.state.parse(next.token, result, innerRepetitions, pre)) {
        if (result.endRepetitions().isEmpty()) {
          break;
        } else if (this._startRepetitions.some(r => result.endRepetitions().has(r))) {
          token = result.nextToken();
          result.startRepetition();
          continue;
        } else {
          return true;
        }
      }

      if (result.nrOfExceptions() > nrOfExceptions) {
        if (!this._startRepetitions.isEmpty() && token !== null && token.type !== TokenType.ENDOFFILE && Pattern.isEndOfLine(token)) {
          do {
            token = token!.next;
          } while (token !== null && !Pattern.isEndOfLine(token));
          if (token !== null && token.type !== TokenType.ENDOFFILE) {
            result.startRepetition();
            continue;
          }
        }
        return false;
      }

      break;
    } while (true);

    if (result.functor() === null && result.state() === null) {
      if (this._functor === null) {
        if (!pre) {
          const expectedTokens = this.expectedTokens(parser, outerRepetitions, ctx);
          if (token !== null) {
            result.addException(ParseException.fromToken('Unexpected token ' + token + ', expected ' + expectedTokens, token));
          } else {
            result.addException(new ParseException('Unexpected token, expected ' + expectedTokens, 0, 0, 0, 0, ''));
          }
        }
        return false;
      }
      result.endPostParse(this._functor, token, this._leftPrecedence);
    }

    return true;
  }

  // @JAVA_REF ParseState.expectedTokens(Parser, Map, ParseContext)
  private expectedTokens(parser: { groupState(group: string): ParseState | null; variable(token: Token, ctx: ParseContext): Variable | null }, outerRepetitions: Map<RepetitionPattern, ParseState>, ctx: ParseContext): string {
    const allStates = this.states(parser, outerRepetitions, ctx);
    const tokens: string[] = [];
    for (const s of allStates) {
      for (const k of s.tokenTexts().keys()) {
        tokens.push("'" + k + "'");
      }
      for (const k of s.tokenTypes().keys()) {
        tokens.push(k.name);
      }
    }
    return tokens.join(',');
  }

  // @JAVA_REF ParseState.direction(Token, Parser, Map, ParseContext)
  private direction(token: Token | null, parser: { groupState(group: string): ParseState | null; variable(token: Token, ctx: ParseContext): Variable | null }, outerRepetitions: Map<RepetitionPattern, ParseState>, ctx: ParseContext): Direction | null {
    if (token === null) {
      return null;
    }
    let dirStates = this.dirStates(token, parser, outerRepetitions, ctx);
    do {
      let newDirStates = dirStates;
      for (const [dir, tokenStates] of dirStates.entries()) {
        let nexts = Set<TokenState>();
        for (const ts of tokenStates) {
          const next = ts.state.tokenNext(ts.token, parser, ctx, null);
          if (next !== null) {
            nexts = nexts.add(next);
            const t = next.token;
            for (const ns of next.state.nodeStates(parser)) {
              nexts = nexts.add({ token: t, state: ns });
            }
          }
        }
        newDirStates = nexts.isEmpty() ? newDirStates.remove(dir) : newDirStates.set(dir, nexts);
      }
      dirStates = newDirStates;
    } while (dirStates.size > 1);
    if (dirStates.size === 1) {
      return dirStates.keySeq().first() as Direction;
    }
    return null;
  }

  // @JAVA_REF ParseState.dirStates(Token, Parser, Map, ParseContext)
  private dirStates(token: Token, parser: { groupState(group: string): ParseState | null; variable(token: Token, ctx: ParseContext): Variable | null }, outerRepetitions: Map<RepetitionPattern, ParseState>, ctx: ParseContext): Map<Direction, Set<TokenState>> {
    let dirStates = Map<Direction, Set<TokenState>>();

    const tokenStates: Set<TokenState> = Set([this.tokenState(token)]);
    dirStates = dirStates.set(Direction.token, tokenStates);

    let nodeTokenStates = Set<TokenState>();
    for (const ns of this.nodeStates(parser)) {
      nodeTokenStates = nodeTokenStates.add({ token, state: ns });
    }
    if (!nodeTokenStates.isEmpty()) {
      dirStates = dirStates.set(Direction.node, nodeTokenStates);
    }

    let repTokenStates = Set<TokenState>();
    for (const rs of this.repetitionStates(outerRepetitions)) {
      repTokenStates = repTokenStates.add({ token, state: rs });
    }
    if (!repTokenStates.isEmpty()) {
      dirStates = dirStates.set(Direction.repeat, repTokenStates);
    }

    let outerTokenStates = Set<TokenState>();
    for (const os of this.outerStates(ctx)) {
      outerTokenStates = outerTokenStates.add({ token, state: os });
    }
    if (!outerTokenStates.isEmpty()) {
      dirStates = dirStates.set(Direction.outer, outerTokenStates);
    }

    return dirStates;
  }

  // @JAVA_REF ParseState.states(Parser, Map, ParseContext)
  private states(parser: { groupState(group: string): ParseState | null; variable(token: Token, ctx: ParseContext): Variable | null }, outerRepetitions: Map<RepetitionPattern, ParseState>, ctx: ParseContext): Set<ParseState> {
    let states = Set<ParseState>([this]);
    for (const ns of this.nodeStates(parser)) {
      states = states.add(ns);
    }
    for (const rs of this.repetitionStates(outerRepetitions)) {
      states = states.add(rs);
    }
    for (const os of this.outerStates(ctx)) {
      states = states.add(os);
    }
    return states;
  }

  // @JAVA_REF ParseState.nodeStates(Parser)
  private nodeStates(parser: { groupState(group: string): ParseState | null }): Set<ParseState> {
    if (!this._nodeTypes.isEmpty()) {
      const gs = parser.groupState(this._group!);
      if (gs !== null) {
        return Set<ParseState>([gs]);
      }
    }
    return Set<ParseState>();
  }

  // @JAVA_REF ParseState.repetitionStates(Map)
  private repetitionStates(repetitions: Map<RepetitionPattern, ParseState>): Set<ParseState> {
    let result = Set<ParseState>();
    if (!this._endRepetitions.isEmpty()) {
      for (const [key, value] of repetitions.entries()) {
        if (this._endRepetitions.has(key)) {
          result = result.add(value);
        }
      }
    }
    return result;
  }

  // @JAVA_REF ParseState.outerStates(ParseContext)
  private outerStates(ctx: ParseContext): Set<ParseState> {
    let result = Set<ParseState>();
    if (this._functor !== null) {
      const type = this._functor.resultType();
      for (let pc: ParseContext | null = ctx; pc !== null && pc.state() !== null; pc = pc.outer()) {
        if (!pc.state()!.nodeTypes().isEmpty()) {
          for (const sup of type.allSupers()) {
            const next = pc.state()!.nodeTypes().get(sup);
            if (next !== undefined) {
              result = result.add(next);
              break;
            }
          }
        }
      }
    }
    return result;
  }

  // @JAVA_REF ParseState.tokenNext(Token, Parser, ParseContext, PatternResult)
  private tokenNext(token: Token | null, parser: { groupState(group: string): ParseState | null; variable(token: Token, ctx: ParseContext): Variable | null }, ctx: ParseContext, result: PatternResult | null): TokenState | null {
    let next = this.tokenTextNext(token, result);
    if (next === null) {
      next = this.tokenTypeNext(token, parser, ctx, result);
    }
    return next;
  }

  // @JAVA_REF ParseState.tokenTextNext(Token, PatternResult)
  private tokenTextNext(token: Token | null, result: PatternResult | null): TokenState | null {
    if (token === null || this._tokenTexts.isEmpty()) {
      return null;
    }
    const type = token.type;
    const text = token.text;
    let next = this._tokenTexts.get(text);

    if (next !== undefined) {
      if (result !== null) {
        result.add(token);
        token.setTextMatch(next.isKeyword());
        token.setState(next);
      }
      return next.tokenState(token.next);
    }

    // Handle negative numbers
    if (this.isNumeric(type) && text.startsWith('-') && this._tokenTypes.get(type) === undefined) {
      const key = '-';
      next = this._tokenTexts.get(key);
      if (next !== undefined) {
        const min = token.split(1);
        if (result !== null) {
          result.addSplit(token, min);
          result.add(min);
          min.setTextMatch(next.isKeyword());
          min.setState(next);
        }
        return next.tokenState(min.next);
      }
    }

    // Handle operator splitting
    if (type === TokenType.OPERATOR) {
      for (let i = text.length - 1; i > 0; i--) {
        const key = text.substring(0, i);
        next = this._tokenTexts.get(key);
        if (next !== undefined) {
          const pre = token.split(1);
          if (result !== null) {
            result.addSplit(token, pre);
            result.add(pre);
            pre.setTextMatch(next.isKeyword());
            pre.setState(next);
          }
          return next.tokenState(pre.next);
        }
      }
    }

    return null;
  }

  // @JAVA_REF ParseState.tokenTypeNext(Token, Parser, ParseContext, PatternResult)
  private tokenTypeNext(token: Token | null, parser: { groupState(group: string): ParseState | null; variable(token: Token, ctx: ParseContext): Variable | null }, ctx: ParseContext, result: PatternResult | null): TokenState | null {
    if (token === null || this._tokenTypes.isEmpty()) {
      return null;
    }
    const type = token.type;

    // Handle NEWLINE
    let next = this._tokenTypes.get(TokenType.NEWLINE);
    if (next !== undefined && Pattern.isEndOfLine(token)) {
      if (result !== null) {
        for (let prev = token.previousAll; prev !== null && prev !== token.previous; prev = prev!.previousAll) {
          if (prev!.type === TokenType.NEWLINE) {
            result.add(prev! as unknown as AstElement);
            prev!.setState(next);
            break;
          }
        }
      }
      return next.tokenState(token);
    }

    // Handle variable lookup
    if (type === TokenType.NAME) {
      const variable = parser.variable(token, ctx);
      if (variable !== null) {
        const tt = variable.type().tokenType();
        next = tt !== null ? this._tokenTypes.get(tt) : undefined;
        if (next !== undefined) {
          if (result !== null) {
            result.add(variable as unknown as AstElement);
            token.setState(next);
          }
          return next.tokenState(token.next);
        }
      }
    }

    // Handle token type match
    if (result !== null || !type.isVariableContent()) {
      next = this._tokenTypes.get(type);
      if (next !== undefined) {
        if (result !== null) {
          result.add(token as unknown as AstElement);
          token.setState(next);
        }
        return next.tokenState(token.next);
      }
    }

    return null;
  }

  // @JAVA_REF ParseState.nodeNext(Token, PatternResult)
  private nodeNext(token: Token | null, result: PatternResult): TokenState | null {
    if (token === null || this._nodeTypes.isEmpty()) {
      return null;
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

    const ctx = createParseContext(this, currentToken, result.context());
    const node = result.parser().parseNode(currentToken, ctx);

    if (node !== null) {
      result.add(node as unknown as AstElement);

      // Handle Variable type
      if ((node as unknown) instanceof Variable) {
        const next = this._nodeTypes.get(Type.VARIABLE);
        if (next !== undefined) {
          return next.tokenState(node.nextToken());
        }
      }

      // Handle normal type matching
      for (const sup of node.type().allSupers()) {
        const next = this._nodeTypes.get(sup);
        if (next !== undefined) {
          return next.tokenState(node.nextToken());
        }
      }

      // Handle type variable matching
      let foundEntry: [Type, ParseState] | undefined;
      for (const [entryKey, entryState] of this._nodeTypes.entries()) {
        if (entryKey.variable() !== null) {
          foundEntry = [entryKey, entryState];
          break;
        }
      }

      if (foundEntry !== undefined) {
        const [keyType, nextState] = foundEntry;
        const variable = keyType.variable()!;
        const resolvedType = result.getTypeArg(variable);
        if (resolvedType !== undefined) {
          if (resolvedType.isAssignableFrom(node.type())) {
            return nextState.tokenState(node.nextToken());
          }
        } else {
          result.putTypeArg(variable, node.type());
          return nextState.tokenState(node.nextToken());
        }
      }

      result.removeLast();
      result.addException(
        ParseException.fromElements(
          'Node ' + node + ' of unexpected type ' + node.type() + ', expected ' + this.expectedTypes(),
          node
        )
      );
    }

    return null;
  }

  // @JAVA_REF ParseState.expectedTypes()
  private expectedTypes(): string {
    const types: string[] = [];
    for (const key of this._nodeTypes.keys()) {
      types.push(String(key));
    }
    return types.join(' or ');
  }

  private isNumeric(type: TokenType): boolean {
    return type === TokenType.NUMBER || type === TokenType.DECIMAL;
  }

  // @JAVA_REF ParseState.merge(ParseState)
  merge(state: ParseState | null): ParseState {
    if (state === null) {
      return this;
    }

    // Merge three maps separately
    const tokenTexts = this._tokenTexts.mergeWith(
      (a, b) => a.merge(b),
      state._tokenTexts
    );
    const tokenTypes = this._tokenTypes.mergeWith(
      (a, b) => a.merge(b),
      state._tokenTypes
    );

    // Merge nodeTypes with type hierarchy handling
    let nodeTypes = this._nodeTypes.mergeWith(
      (a, b) => a.merge(b),
      state._nodeTypes
    );
    for (const subType of nodeTypes.keys()) {
      for (const superType of subType.allSupers()) {
        if (!superType.equals(subType)) {
          const superState = nodeTypes.get(superType);
          if (superState !== undefined) {
            const subState = nodeTypes.get(subType);
            if (subState !== undefined) {
              const mergedState = subState.merge(superState);
              nodeTypes = nodeTypes.set(subType, mergedState);
            }
          }
        }
      }
    }

    return new ParseState(
      tokenTexts,
      tokenTypes,
      nodeTypes,
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

  // @JAVA_REF ParseState.toString()
  toString(): string {
    const textKeys: string[] = [];
    for (const k of this._tokenTexts.keys()) textKeys.push(String(k));
    const typeKeys: string[] = [];
    for (const k of this._tokenTypes.keys()) typeKeys.push(k.name);
    const nodeKeys: string[] = [];
    for (const k of this._nodeTypes.keys()) nodeKeys.push(String(k));
    return '{' + textKeys.join(', ') + '}{' + typeKeys.join(', ') + '}{' + nodeKeys.join(', ') + '}';
  }
}
