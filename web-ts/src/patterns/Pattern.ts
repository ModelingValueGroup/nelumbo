/**
 * Pattern abstract base class for syntax pattern matching.
 * @JAVA_REF org.modelingvalue.nelumbo.patterns.Pattern
 */

import { List, Map } from 'immutable';
import { TokenType } from '../syntax/TokenType';
import type { Token } from '../syntax/Token';
import type { AstElement } from '../AstElement';
import { Type } from '../Type';
import { Variable } from '../Variable';
import { Node } from '../Node';
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
  // These are implemented using a registry pattern to avoid circular imports

  private static _registry: {
    AlternationPattern?: new (type: Type, ast: List<AstElement>, options: List<Pattern>) => Pattern;
    NodeTypePattern?: new (type: Type, ast: List<AstElement>, nodeType: Type, precedence: number | null) => Pattern;
    OptionalPattern?: new (type: Type, ast: List<AstElement>, optional: Pattern) => Pattern;
    RepetitionPattern?: new (type: Type, ast: List<AstElement>, repeated: Pattern, mandatory: boolean, separator: Pattern | null) => Pattern;
    SequencePattern?: new (type: Type, ast: List<AstElement>, elements: List<Pattern>) => Pattern;
    TokenTextPattern?: new (type: Type, ast: List<AstElement>, text: string | Variable, isKeyword?: boolean) => Pattern;
    TokenTypePattern?: new (type: Type, ast: List<AstElement>, tokenType: TokenType) => Pattern;
  } = {};

  static registerPatternClass(name: string, clazz: unknown): void {
    (Pattern._registry as Record<string, unknown>)[name] = clazz;
  }

  static aWithElements(ast: List<AstElement>, ...options: Pattern[]): Pattern {
    const Clazz = Pattern._registry.AlternationPattern;
    if (!Clazz) throw new Error('AlternationPattern not registered');
    return new Clazz(Type.PATTERN, ast, List(options));
  }

  static nWithElements(ast: List<AstElement>, nodeType: Type, precedence: number | null): Pattern {
    const Clazz = Pattern._registry.NodeTypePattern;
    if (!Clazz) throw new Error('NodeTypePattern not registered');
    return new Clazz(Type.PATTERN, ast, nodeType, precedence);
  }

  static oWithElements(ast: List<AstElement>, optional: Pattern): Pattern {
    const Clazz = Pattern._registry.OptionalPattern;
    if (!Clazz) throw new Error('OptionalPattern not registered');
    return new Clazz(Type.PATTERN, ast, optional);
  }

  static rWithElements(ast: List<AstElement>, repeated: Pattern, mandatory: boolean, separator: Pattern | null): Pattern {
    const Clazz = Pattern._registry.RepetitionPattern;
    if (!Clazz) throw new Error('RepetitionPattern not registered');
    return new Clazz(Type.PATTERN, ast, repeated, mandatory, separator);
  }

  static sWithElements(ast: List<AstElement>, ...elements: Pattern[]): Pattern {
    const Clazz = Pattern._registry.SequencePattern;
    if (!Clazz) throw new Error('SequencePattern not registered');
    // Flatten nested sequence patterns
    const flattened = List(elements).flatMap(e => {
      if ('patternElements' in e && typeof (e as { patternElements: unknown }).patternElements === 'function') {
        return (e as unknown as { patternElements(): List<Pattern> }).patternElements();
      }
      return List([e]);
    });
    return new Clazz(Type.PATTERN, ast, flattened);
  }

  static tTextWithElements(ast: List<AstElement>, tokenText: string, isKeyword: boolean): Pattern {
    const Clazz = Pattern._registry.TokenTextPattern;
    if (!Clazz) throw new Error('TokenTextPattern not registered');
    return new Clazz(Type.PATTERN, ast, tokenText, isKeyword);
  }

  static kWithElements(ast: List<AstElement>, tokenText: string): Pattern {
    const Clazz = Pattern._registry.TokenTextPattern;
    if (!Clazz) throw new Error('TokenTextPattern not registered');
    return new Clazz(Type.PATTERN, ast, tokenText, true);
  }

  static tVarWithElements(ast: List<AstElement>, variable: Variable): Pattern {
    const Clazz = Pattern._registry.TokenTextPattern;
    if (!Clazz) throw new Error('TokenTextPattern not registered');
    return new Clazz(Type.PATTERN, ast, variable);
  }

  static tTypeWithElements(ast: List<AstElement>, tokenType: TokenType): Pattern {
    const Clazz = Pattern._registry.TokenTypePattern;
    if (!Clazz) throw new Error('TokenTypePattern not registered');
    return new Clazz(Type.PATTERN, ast, tokenType);
  }

  static vWithElements(ast: List<AstElement>, variable: Variable): Pattern {
    const type = variable.type();
    const tt = type.tokenType();
    if (tt !== null) {
      const Clazz = Pattern._registry.TokenTextPattern;
      if (!Clazz) throw new Error('TokenTextPattern not registered');
      return new Clazz(Type.PATTERN, ast, variable);
    }
    const Clazz = Pattern._registry.NodeTypePattern;
    if (!Clazz) throw new Error('NodeTypePattern not registered');
    return new Clazz(Type.PATTERN, ast, Type.fromVariable(variable), null);
  }
}
