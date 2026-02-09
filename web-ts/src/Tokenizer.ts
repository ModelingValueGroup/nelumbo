/**
 * Tokenizer (Lexer) for Nelumbo.
 * Ported from Java: org.modelingvalue.nelumbo.syntax.Tokenizer
 */

import { List } from 'immutable';
import { Token } from './Token';
import { TokenType } from './TokenType';

export interface TokenizerResult {
  /** First token including skipped tokens */
  firstAll: Token | null;
  /** First non-skipped token */
  first: Token | null;
  /** Last token including skipped tokens */
  lastAll: Token | null;
  /** Last non-skipped token */
  last: Token | null;
  /** List of non-skipped tokens */
  list: List<Token>;
  /** List of all tokens */
  listAll: List<Token>;
  /** Source file name */
  fileName: string;
  /** Original input string */
  input: string;
}

export class Tokenizer {
  private readonly input: string;
  private readonly fileName: string;

  constructor(input: string, fileName: string) {
    this.input = input;
    this.fileName = fileName;
  }

  /**
   * Tokenize the input and return the result.
   */
  tokenize(): TokenizerResult {
    const tokens: Token[] = [];
    const allTokens: Token[] = [];

    let line = 0;
    let position = 0;
    let index = 0;

    // Track the last token that can continue on next line
    let lastContinuingToken: Token | null = null;

    // Add BEGINOFFILE synthetic token
    const bofToken = new Token(
      TokenType.BEGINOFFILE,
      '',
      line,
      position,
      index,
      this.fileName
    );
    allTokens.push(bofToken);

    // Main tokenization loop
    while (index < this.input.length) {
      const remaining = this.input.substring(index);
      const matched = this.matchToken(remaining);

      if (!matched) {
        // Should not happen with ERROR pattern as fallback
        throw new Error(`Failed to match token at index ${index}`);
      }

      const [type, text] = matched;

      // Create the token
      const token = new Token(type, text, line, position, index, this.fileName);
      allTokens.push(token);

      // Update position tracking
      const newlines = this.countNewlines(text);
      if (newlines > 0) {
        line += newlines;
        position = this.lastLineLength(text);
      } else {
        position += text.length;
      }
      index += text.length;

      // Handle skip tokens and newlines
      if (type === TokenType.NEWLINE) {
        // Check if previous significant token continues on next line
        if (lastContinuingToken !== null) {
          // Skip this newline (treat as layout)
          continue;
        }
      }

      if (!type.isSkip() && type !== TokenType.NEWLINE) {
        tokens.push(token);
        if (type.isContinuesOnNextLine()) {
          lastContinuingToken = token;
        } else {
          lastContinuingToken = null;
        }
      }
    }

    // Add ENDOFFILE synthetic token
    const eofToken = new Token(
      TokenType.ENDOFFILE,
      '',
      line,
      position,
      index,
      this.fileName
    );
    allTokens.push(eofToken);
    tokens.push(eofToken);

    // Link tokens
    this.linkTokens(tokens);
    this.linkAllTokens(allTokens);

    return {
      firstAll: allTokens[0] || null,
      first: tokens[0] || null,
      lastAll: allTokens[allTokens.length - 1] || null,
      last: tokens[tokens.length - 1] || null,
      list: List(tokens),
      listAll: List(allTokens),
      fileName: this.fileName,
      input: this.input,
    };
  }

  /**
   * Match a token at the start of the input.
   * Returns [TokenType, matchedText] or null if no match.
   */
  private matchToken(input: string): [TokenType, string] | null {
    for (const type of TokenType.getMatchedTypes()) {
      if (type.pattern) {
        const match = input.match(type.pattern);
        if (match && match.index === 0) {
          return [type, match[0]];
        }
      }
    }
    return null;
  }

  /**
   * Link non-skipped tokens in a doubly-linked list.
   */
  private linkTokens(tokens: Token[]): void {
    for (let i = 0; i < tokens.length; i++) {
      const token = tokens[i];
      if (i > 0) {
        token.setPrevious(tokens[i - 1]);
      }
      if (i < tokens.length - 1) {
        token.setNext(tokens[i + 1]);
      }
    }
  }

  /**
   * Link all tokens (including skipped) in a doubly-linked list.
   */
  private linkAllTokens(tokens: Token[]): void {
    for (let i = 0; i < tokens.length; i++) {
      const token = tokens[i];
      if (i > 0) {
        token.setPreviousAll(tokens[i - 1]);
      }
      if (i < tokens.length - 1) {
        token.setNextAll(tokens[i + 1]);
      }
    }
  }

  private countNewlines(text: string): number {
    let count = 0;
    for (let i = 0; i < text.length; i++) {
      const c = text[i];
      if (c === '\n') {
        count++;
      } else if (c === '\r') {
        count++;
        if (i + 1 < text.length && text[i + 1] === '\n') {
          i++;
        }
      }
    }
    return count;
  }

  private lastLineLength(text: string): number {
    const lastNewline = Math.max(text.lastIndexOf('\n'), text.lastIndexOf('\r'));
    if (lastNewline === -1) {
      return text.length;
    }
    return text.length - lastNewline - 1;
  }
}
