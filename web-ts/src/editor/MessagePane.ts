/**
 * MessagePane - Error and result display pane for the Nelumbo editor.
 */

import type { ParserResult } from '../syntax/ParserResult';
import { SyntaxHighlighter } from './SyntaxHighlighter';
import { Tokenizer } from '../syntax/Tokenizer';

/**
 * Message types for display.
 */
export type MessageType = 'error' | 'warning' | 'info' | 'success' | 'result';

/**
 * A message to display.
 */
export interface Message {
  type: MessageType;
  line: number;
  column: number;
  text: string;
  detail?: string;
}

/**
 * Highlight info for correlating editor and message positions.
 */
export interface HighlightInfo {
  editorStart: number;
  editorEnd: number;
  messageStart: number;
  messageEnd: number;
  type: 'error' | 'result';
}

/**
 * MessagePane - manages error and result display.
 */
export class MessagePane {
  private container: HTMLElement;
  private messages: Message[] = [];
  private highlights: HighlightInfo[] = [];
  private syntaxHighlighter: SyntaxHighlighter;

  constructor(container: HTMLElement) {
    this.container = container;
    this.syntaxHighlighter = new SyntaxHighlighter();
    this.setupStyles();
  }

  private setupStyles(): void {
    this.container.style.fontFamily = "'JetBrains Mono', 'Fira Code', 'Consolas', monospace";
    this.container.style.fontSize = '14px'; // Match editor font size
    this.container.style.lineHeight = '1.5';
    this.container.style.padding = '16px';
    this.container.style.overflow = 'auto';
    this.container.style.whiteSpace = 'pre-wrap';
    this.container.style.wordBreak = 'break-word';
  }

  /**
   * Clear all messages.
   */
  clear(): void {
    this.messages = [];
    this.highlights = [];
    this.container.innerHTML = '';
  }

  /**
   * Add a message.
   */
  addMessage(message: Message): void {
    this.messages.push(message);
  }

  /**
   * Add an error message.
   */
  addError(line: number, column: number, text: string, detail?: string): void {
    this.addMessage({ type: 'error', line, column, text, detail });
  }

  /**
   * Add a result message.
   */
  addResult(line: number, column: number, text: string): void {
    this.addMessage({ type: 'result', line, column, text });
  }

  /**
   * Display messages from a parser result.
   */
  showParserResult(parserResult: ParserResult): void {
    this.clear();

    const exceptions = parserResult.exceptions();
    if (!exceptions.isEmpty()) {
      // Show parse errors
      exceptions.forEach(exc => {
        this.addError(
          exc.line,
          exc.position,
          exc.shortMessage,
          exc.fullMessage
        );
      });
    }

    this.render();
  }

  /**
   * Render messages to the container.
   */
  render(): void {
    this.container.innerHTML = '';

    if (this.messages.length === 0) {
      return;
    }

    // Sort messages by line (0-based line numbers from ParseException)
    const sorted = [...this.messages].sort((a, b) => a.line - b.line);

    // Build lines aligned with editor lines (0-based)
    const entries: { text: string; msg: Message | null }[] = [];
    let lastLine = 0;
    const MAX_LINES = 10000;

    for (const msg of sorted) {
      if (msg.line < 0 || msg.line > MAX_LINES) {
        continue;
      }

      // Add empty lines to align with editor
      while (lastLine < msg.line) {
        entries.push({ text: '', msg: null });
        lastLine++;
      }

      // Add message line
      const prefix = this.getMessagePrefix(msg.type);
      const text = `${prefix}${msg.text}`;
      entries.push({ text, msg });
      lastLine = msg.line + 1;
    }

    // Create HTML
    const html = entries.map((entry) => {
      if (!entry.text) return '<br>';

      const className = entry.msg ? `message-${entry.msg.type}` : '';

      if (entry.msg && entry.msg.type === 'result') {
        // Syntax-highlight result text (Nelumbo expressions)
        const tokenizer = new Tokenizer(entry.text, 'messages.nl');
        const result = tokenizer.tokenize();
        this.syntaxHighlighter.setTokenizerResult(result);
        const highlighted = this.syntaxHighlighter.highlightCode(entry.text);
        return `<div class="${className}">${highlighted}</div>`;
      } else {
        // Plain text for error and other messages
        const escaped = escapeHtml(entry.text);
        return `<div class="${className}">${escaped}</div>`;
      }
    }).join('');

    this.container.innerHTML = html;

    // Apply message type styles
    this.applyMessageStyles();
  }

  private getMessagePrefix(type: MessageType): string {
    switch (type) {
      case 'error':
        return ''; // Errors are highlighted differently
      case 'warning':
        return '⚠ ';
      case 'info':
        return 'ℹ ';
      case 'success':
        return '';
      case 'result':
        return '';
      default:
        return '';
    }
  }

  private applyMessageStyles(): void {
    // Error messages - red background
    this.container.querySelectorAll('.message-error').forEach(el => {
      (el as HTMLElement).style.backgroundColor = '#ffeeee';
      (el as HTMLElement).style.color = '#cc0000';
      (el as HTMLElement).style.padding = '2px 4px';
      (el as HTMLElement).style.borderRadius = '2px';
    });

    // Success/result messages - green background
    this.container.querySelectorAll('.message-success, .message-result').forEach(el => {
      (el as HTMLElement).style.backgroundColor = '#eeffee';
      (el as HTMLElement).style.color = '#006600';
      (el as HTMLElement).style.padding = '2px 4px';
      (el as HTMLElement).style.borderRadius = '2px';
    });

    // Warning messages
    this.container.querySelectorAll('.message-warning').forEach(el => {
      (el as HTMLElement).style.backgroundColor = '#fff8e0';
      (el as HTMLElement).style.color = '#996600';
    });
  }

  /**
   * Get highlights for correlating editor and message positions.
   */
  getHighlights(): HighlightInfo[] {
    return this.highlights;
  }

  /**
   * Set synchronized scroll with editor.
   */
  syncScrollWith(editorElement: HTMLElement): void {
    editorElement.addEventListener('scroll', () => {
      this.container.scrollTop = editorElement.scrollTop;
    });
  }
}

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
