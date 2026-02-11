/**
 * AstElement interface - common interface for Token and Node.
 * @JAVA_REF org.modelingvalue.nelumbo.AstElement
 */

import { List } from 'immutable';
import { Token } from './syntax/Token';

/**
 * Interface for AST elements (Token and Node).
 */
export interface AstElement {
  /**
   * Get the first token of this element.
   */
  firstToken(): Token | null;

  /**
   * Get the last token of this element.
   */
  lastToken(): Token | null;

  /**
   * Deparse this element to a string buffer.
   */
  deparse(sb: string[]): void;
}

/**
 * Utility functions for AstElement.
 */
export const AstElementUtil = {
  /**
   * Get the first token from a list of elements.
   */
  firstToken(elements: List<AstElement>): Token | null {
    for (const element of elements) {
      const first = element.firstToken();
      if (first !== null) {
        return first;
      }
    }
    return null;
  },

  /**
   * Get the last token from a list of elements.
   */
  lastToken(elements: List<AstElement>): Token | null {
    for (const element of elements.reverse()) {
      const last = element.lastToken();
      if (last !== null) {
        return last;
      }
    }
    return null;
  },
};
