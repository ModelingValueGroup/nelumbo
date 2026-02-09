/**
 * Pattern abstract base class for syntax pattern matching.
 * Ported from Java: org.modelingvalue.nelumbo.patterns.Pattern
 */

import { List, Map } from 'immutable';
import { TokenType } from '../TokenType';
import type { Token } from '../Token';
import type { AstElement } from '../core/AstElement';
import { Type } from '../core/Type';
import { Variable } from '../core/Variable';
import { Node } from '../core/Node';
import type { ParseState } from '../syntax/ParseState';
import type { Functor } from './Functor';

/**
 * Abstract base class for syntax patterns.
 */
export abstract class Pattern extends Node {
  protected constructor(type: Type, elements: List<AstElement>, ...args: unknown[]) {
    super(type, elements, ...args);
  }

  /**
   * Build a parse state for this pattern.
   */
  abstract parseState(next: ParseState, functor: Functor): ParseState;

  /**
   * Get the name of this pattern (for functor naming).
   */
  name(): string {
    return '';
  }

  /**
   * Set precedence for inner patterns.
   */
  setPrecedence(_precedence: number): Pattern {
    return this;
  }

  /**
   * Apply a type transformation function.
   */
  setTypes(_typeFunction: (type: Type) => Type): Pattern {
    return this;
  }

  /**
   * Get the argument types this pattern produces.
   */
  abstract argTypes(types: List<Type>): List<Type>;

  /**
   * Convert arguments to string representation.
   */
  abstract string(args: List<unknown>, ai: number, sb: string[], previous: TokenType[], alt: boolean): number;

  /**
   * Extract arguments from AST elements.
   */
  abstract extractArgs(
    elements: List<AstElement>,
    i: number,
    args: unknown[],
    alt: boolean,
    functor: Functor,
    typeArgs: Map<Variable, Type>
  ): number;

  /**
   * Get the declaration pattern for a token.
   */
  abstract tokenDeclaration(token: Token): Pattern | null;

  /**
   * Check if a token is at end of line.
   */
  static isEndOfLine(token: Token): boolean {
    return token.type === TokenType.ENDOFFILE ||
           (token.previous !== null && token.line > token.previous.line);
  }

  /**
   * Add text with proper spacing based on token types.
   */
  protected addText(sb: string[], previous: TokenType[], text: string): void {
    const type = TokenType.of(text);
    if (previous[0] === TokenType.NAME ||
        previous[0] === TokenType.NUMBER ||
        previous[0] === TokenType.DECIMAL) {
      if (type === TokenType.NAME ||
          type === TokenType.NUMBER ||
          type === TokenType.DECIMAL) {
        sb.push(' ');
      }
    }
    sb.push(text);
    previous[0] = type;
  }

  // Static factory methods

  /**
   * Create an alternation pattern.
   */
  static a(...options: Pattern[]): Pattern {
    return Pattern.aWithElements(List(), ...options);
  }

  /**
   * Create a node type pattern.
   */
  static n(nodeType: Type, precedence: number | null): Pattern {
    return Pattern.nWithElements(List(), nodeType, precedence);
  }

  /**
   * Create an optional pattern.
   */
  static o(optional: Pattern): Pattern {
    return Pattern.oWithElements(List(), optional);
  }

  /**
   * Create a repetition pattern.
   */
  static r(repeated: Pattern, mandatory: boolean, separator: Pattern | null): Pattern {
    return Pattern.rWithElements(List(), repeated, mandatory, separator);
  }

  /**
   * Create a sequence pattern.
   */
  static s(...elements: Pattern[]): Pattern {
    return Pattern.sWithElements(List(), ...elements);
  }

  /**
   * Create a token text pattern.
   */
  static t(tokenText: string): Pattern;
  static t(tokenType: TokenType): Pattern;
  static t(variable: Variable): Pattern;
  static t(value: string | TokenType | Variable): Pattern {
    if (typeof value === 'string') {
      return Pattern.tTextWithElements(List(), value, false);
    } else if (value instanceof Variable) {
      return Pattern.tVarWithElements(List(), value);
    } else {
      return Pattern.tTypeWithElements(List(), value);
    }
  }

  /**
   * Create a keyword token text pattern.
   */
  static k(tokenText: string): Pattern {
    return Pattern.kWithElements(List(), tokenText);
  }

  /**
   * Create a variable pattern.
   */
  static v(variable: Variable): Pattern {
    return Pattern.vWithElements(List(), variable);
  }

  // Factory methods with AST elements

  static aWithElements(ast: List<AstElement>, ...options: Pattern[]): Pattern {
    // Import dynamically to avoid circular deps
    const { AlternationPattern } = require('./AlternationPattern');
    return new AlternationPattern(Type.PATTERN, ast, List(options));
  }

  static nWithElements(ast: List<AstElement>, nodeType: Type, precedence: number | null): Pattern {
    const { NodeTypePattern } = require('./NodeTypePattern');
    return new NodeTypePattern(Type.PATTERN, ast, nodeType, precedence);
  }

  static oWithElements(ast: List<AstElement>, optional: Pattern): Pattern {
    const { OptionalPattern } = require('./OptionalPattern');
    return new OptionalPattern(Type.PATTERN, ast, optional);
  }

  static rWithElements(ast: List<AstElement>, repeated: Pattern, mandatory: boolean, separator: Pattern | null): Pattern {
    const { RepetitionPattern } = require('./RepetitionPattern');
    return new RepetitionPattern(Type.PATTERN, ast, repeated, mandatory, separator);
  }

  static sWithElements(ast: List<AstElement>, ...elements: Pattern[]): Pattern {
    const { SequencePattern } = require('./SequencePattern');
    // Flatten nested sequence patterns
    const flattened = List(elements).flatMap(e => {
      if (e instanceof SequencePattern) {
        return (e as unknown as { patternElements(): List<Pattern> }).patternElements();
      }
      return List([e]);
    });
    return new SequencePattern(Type.PATTERN, ast, flattened);
  }

  static tTextWithElements(ast: List<AstElement>, tokenText: string, isKeyword: boolean): Pattern {
    const { TokenTextPattern } = require('./TokenTextPattern');
    return new TokenTextPattern(Type.PATTERN, ast, tokenText, isKeyword);
  }

  static kWithElements(ast: List<AstElement>, tokenText: string): Pattern {
    const { TokenTextPattern } = require('./TokenTextPattern');
    return new TokenTextPattern(Type.PATTERN, ast, tokenText, true);
  }

  static tVarWithElements(ast: List<AstElement>, variable: Variable): Pattern {
    const { TokenTextPattern } = require('./TokenTextPattern');
    return new TokenTextPattern(Type.PATTERN, ast, variable);
  }

  static tTypeWithElements(ast: List<AstElement>, tokenType: TokenType): Pattern {
    const { TokenTypePattern } = require('./TokenTypePattern');
    return new TokenTypePattern(Type.PATTERN, ast, tokenType);
  }

  static vWithElements(ast: List<AstElement>, variable: Variable): Pattern {
    const type = variable.type();
    const tt = type.tokenType();
    if (tt !== null) {
      const { TokenTextPattern } = require('./TokenTextPattern');
      return new TokenTextPattern(Type.PATTERN, ast, variable);
    }
    const { NodeTypePattern } = require('./NodeTypePattern');
    return new NodeTypePattern(Type.PATTERN, ast, Type.fromVariable(variable), null);
  }
}

// Re-export SequencePattern for type checking
export { SequencePattern } from './SequencePattern';
