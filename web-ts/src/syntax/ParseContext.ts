/**
 * ParseContext - immutable parsing context with precedence and group.
 * @JAVA_REF org.modelingvalue.nelumbo.syntax.ParseContext
 */

import type { Token } from './Token';
import type { ParseState } from './ParseState';

/**
 * ParseContext - holds parsing context information.
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
 * Create a new ParseContext.
 */
export function createParseContext(
  state: ParseState | null,
  token: Token | null,
  group: string,
  precedence: number,
  outer: ParseContext | null
): ParseContext {
  return {
    state: () => state,
    token: () => token,
    precedence: () => precedence,
    group: () => group,
    outer: () => outer,

    toString(): string {
      const stateStr = state ? state.toString() + ' ' : '';
      const outerStr = outer ? ' ' + outer.toString() : '';
      return stateStr + precedence + ' ' + group + outerStr;
    },
  };
}
