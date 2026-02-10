/**
 * SyntaxHighlighter - Token-based syntax coloring for the Nelumbo editor.
 */

import type { Token } from '../syntax/Token';
import type { TokenizerResult } from '../syntax/Tokenizer';
import { EditorTheme } from './EditorTheme';

/**
 * Highlighted span information.
 */
export interface HighlightSpan {
  start: number;
  end: number;
  className: string;
  style: string;
}

/**
 * SyntaxHighlighter - applies syntax highlighting to tokenized code.
 */
export class SyntaxHighlighter {
  private tokenizerResult: TokenizerResult | null = null;

  /**
   * Update the tokenizer result.
   */
  setTokenizerResult(result: TokenizerResult): void {
    this.tokenizerResult = result;
  }

  /**
   * Get highlight spans for all tokens.
   */
  getHighlightSpans(): HighlightSpan[] {
    const spans: HighlightSpan[] = [];

    if (!this.tokenizerResult) {
      return spans;
    }

    let token: Token | null = this.tokenizerResult.firstAll;
    while (token) {
      const colorType = token.colorType;
      const scheme = EditorTheme.getTokenColor(colorType);

      if (scheme) {
        spans.push({
          start: token.index,
          end: token.index + token.text.length,
          className: `token-${colorType.name.toLowerCase()}`,
          style: EditorTheme.schemeToCSS(scheme),
        });
      }

      token = token.nextAll;
    }

    return spans;
  }

  /**
   * Apply highlighting to plain text, returning HTML.
   */
  highlightCode(code: string): string {
    if (!this.tokenizerResult) {
      return escapeHtml(code);
    }

    const spans = this.getHighlightSpans();
    if (spans.length === 0) {
      return escapeHtml(code);
    }

    // Sort spans by start position
    spans.sort((a, b) => a.start - b.start);

    const result: string[] = [];
    let lastIndex = 0;

    for (const span of spans) {
      // Add unhighlighted text before this span
      if (span.start > lastIndex) {
        result.push(escapeHtml(code.substring(lastIndex, span.start)));
      }

      // Add highlighted span
      const text = code.substring(span.start, span.end);
      result.push(`<span class="${span.className}" style="${span.style}">${escapeHtml(text)}</span>`);

      lastIndex = span.end;
    }

    // Add remaining text
    if (lastIndex < code.length) {
      result.push(escapeHtml(code.substring(lastIndex)));
    }

    return result.join('');
  }

  /**
   * Get token at a specific character index.
   */
  getTokenAtIndex(index: number): Token | null {
    if (!this.tokenizerResult) {
      return null;
    }

    let token: Token | null = this.tokenizerResult.firstAll;
    while (token) {
      if (token.index <= index && index < token.index + token.text.length) {
        return token;
      }
      token = token.nextAll;
    }

    return null;
  }

  /**
   * Get token at a specific line and column.
   */
  getTokenAt(line: number, column: number): Token | null {
    if (!this.tokenizerResult) {
      return null;
    }

    let token: Token | null = this.tokenizerResult.firstAll;
    while (token) {
      if (token.contains(line, column)) {
        return token;
      }
      token = token.nextAll;
    }

    return null;
  }

  /**
   * Generate CSS classes for all token types.
   */
  static generateCSS(): string {
    const css: string[] = [];
    const colors = EditorTheme.getAllTokenColors();

    colors.forEach((scheme, typeName) => {
      const className = `.token-${typeName.toLowerCase()}`;
      const styles = EditorTheme.schemeToCSS(scheme);
      if (styles) {
        css.push(`${className} { ${styles}; }`);
      }
    });

    return css.join('\n');
  }
}

/**
 * Escape HTML special characters.
 */
function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
