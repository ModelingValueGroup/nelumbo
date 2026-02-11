/**
 * ParseContext - immutable parsing context with precedence and group.
 * @JAVA_REF org.modelingvalue.nelumbo.syntax.ParseContext
 */

import type { Token } from './Token';
import type { ParseState } from './ParseState';

/**
 * ParseContext - holds parsing context information.
 * @JAVA_REF org.modelingvalue.nelumbo.syntax.ParseContext
 */
export interface ParseContext {
  /**
   * Current parse state.
   */
  state(): ParseState | null;

  /**
   * Current token.
   */
  token(): Token | null;

  /**
   * Current precedence level.
   */
  precedence(): number;

  /**
   * Current parsing group.
   */
  group(): string;

  /**
   * Outer context (for nested parsing).
   */
  outer(): ParseContext | null;

  /**
   * String representation.
   */
  toString(): string;
}

/**
 * Create a new ParseContext for inner parsing (derives precedence/group from state).
 * @JAVA_REF ParseContext.of(ParseState, Token, ParseContext)
 */
export function createParseContext(
  state: ParseState,
  token: Token | null,
  outer: ParseContext
): ParseContext;

/**
 * Create a new ParseContext for top-level parsing.
 * @JAVA_REF ParseContext.of(String, int)
 */
export function createParseContext(
  group: string,
  precedence: number
): ParseContext;

export function createParseContext(
  arg1: ParseState | string,
  arg2: Token | null | number,
  arg3?: ParseContext
): ParseContext {
  if (typeof arg1 === 'string') {
    // Top-level: (group, precedence)
    const group = arg1;
    const precedence = arg2 as number;
    return {
      state: () => null,
      token: () => null,
      precedence: () => precedence,
      group: () => group,
      outer: () => null,

      toString(): string {
        return '(' + precedence + ' ' + group + ')';
      },
    };
  } else {
    // Inner: (state, token, outer)
    const state = arg1;
    const token = arg2 as Token | null;
    const outer = arg3 as ParseContext;
    const innerPrec = state.innerPrecedence();
    const precedence = innerPrec ?? Number.MIN_SAFE_INTEGER;
    const group = state.group()!;
    return {
      state: () => state,
      token: () => token,
      precedence: () => precedence,
      group: () => group,
      outer: () => outer,

      toString(): string {
        return '(' + state + ' ' + precedence + ' ' + group + ' ' + outer + ')';
      },
    };
  }
}
