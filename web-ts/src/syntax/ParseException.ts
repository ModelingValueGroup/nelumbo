/**
 * ParseException - represents a parsing error.
 * @JAVA_REF org.modelingvalue.nelumbo.syntax.ParseException
 */

import { List } from 'immutable';
import type { Token } from './Token';
import type { AstElement } from '../AstElement';

export class ParseException extends Error {
  readonly line: number;
  readonly position: number;
  readonly index: number;
  readonly length: number;
  readonly fileName: string;

  constructor(
    message: string,
    line: number,
    position: number,
    index: number,
    length: number,
    fileName: string,
    cause?: Error
  ) {
    super(message);
    this.name = 'ParseException';
    this.line = line;
    this.position = position;
    this.index = index;
    this.length = length;
    this.fileName = fileName;
    if (cause) {
      this.cause = cause;
    }
  }

  /**
   * Create from AST elements.
   */
  static fromElements(message: string, ...elements: AstElement[]): ParseException {
    const firstToken = ParseException.getFirstToken(elements);
    const lastToken = ParseException.getLastToken(elements);
    return ParseException.fromTokens(message, firstToken, lastToken);
  }

  /**
   * Create from a list of AST elements.
   */
  static fromElementList(message: string, elements: List<AstElement>): ParseException {
    const arr = elements.toArray();
    return ParseException.fromElements(message, ...arr);
  }

  /**
   * Create from first and last tokens.
   */
  static fromTokens(message: string, firstToken: Token | null, lastToken: Token | null, cause?: Error): ParseException {
    if (firstToken === null) {
      return new ParseException(message, -1, -1, -1, -1, '', cause);
    }

    const length = lastToken
      ? lastToken.position - firstToken.position + lastToken.text.length
      : firstToken.text.length;

    return new ParseException(
      message,
      firstToken.line,
      firstToken.position,
      firstToken.index,
      length,
      firstToken.fileName,
      cause
    );
  }

  /**
   * Create from a single token.
   */
  static fromToken(message: string, token: Token): ParseException {
    return new ParseException(
      message,
      token.line,
      token.position,
      token.index,
      token.text.length,
      token.fileName
    );
  }

  /**
   * Create from file name only.
   */
  static fromFile(message: string, fileName: string, cause?: Error): ParseException {
    return new ParseException(message, 0, 0, 0, 0, fileName, cause);
  }

  private static getFirstToken(elements: AstElement[]): Token | null {
    if (elements.length === 0) return null;
    let firstToken = elements[0].firstToken();
    if (firstToken !== null) {
      // Skip layout tokens
      while (firstToken.type.isLayout() && firstToken.next !== null) {
        firstToken = firstToken.next;
      }
    }
    return firstToken;
  }

  private static getLastToken(elements: AstElement[]): Token | null {
    if (elements.length === 0) return null;
    let lastToken = elements[elements.length - 1].lastToken();
    if (lastToken !== null) {
      // Skip layout tokens
      while (lastToken.type.isLayout() && lastToken.previous !== null) {
        lastToken = lastToken.previous;
      }
    }
    return lastToken;
  }

  get fullMessage(): string {
    return `${this.message}, line=${this.line + 1}, position=${this.position + 1}, file=${this.fileName}`;
  }

  get shortMessage(): string {
    return this.message;
  }
}
