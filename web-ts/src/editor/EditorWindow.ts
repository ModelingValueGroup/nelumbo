/**
 * EditorWindow - Individual editor tab/window management.
 * Handles a single editor instance with syntax highlighting and parsing.
 */

import type { TokenizerResult } from '../syntax/Tokenizer';
import { ParserResult } from '../syntax/ParserResult';
import { Tokenizer } from '../syntax/Tokenizer';
import { Parser } from '../syntax/Parser';
import { KnowledgeBase } from '../KnowledgeBase';
import { Query } from '../Query';
import { isEvaluatable } from '../Evaluatable';
import { ParseException } from '../syntax/ParseException';
import { SyntaxHighlighter } from './SyntaxHighlighter';
import { MessagePane } from './MessagePane';
import { EditorTheme } from './EditorTheme';

/**
 * EditorWindow state.
 */
export interface EditorWindowState {
  id: string;
  title: string;
  content: string;
  isExample: boolean;
  examplePath?: string;
  caretPosition: number;
  selectionStart: number;
  selectionEnd: number;
}

/**
 * EditorWindow change event handler.
 */
export type EditorChangeHandler = (window: EditorWindow) => void;

/**
 * EditorWindow - manages a single editor instance.
 */
export class EditorWindow {
  private id: string;
  private title: string;
  private isExample: boolean;
  private examplePath?: string;
  private container: HTMLElement;
  private editorElement: HTMLElement;
  private highlightOverlay: HTMLElement;
  private messagesPane: MessagePane;
  private syntaxHighlighter: SyntaxHighlighter;
  private lastTokenizerResult: TokenizerResult | null = null;
  private lastParserResult: ParserResult | null = null;
  private debounceTimer: number | null = null;
  private changeHandlers: EditorChangeHandler[] = [];

  constructor(
    id: string,
    container: HTMLElement,
    messagesContainer: HTMLElement,
    title: string = 'Untitled',
    isExample: boolean = false,
    examplePath?: string
  ) {
    this.id = id;
    this.title = title;
    this.isExample = isExample;
    this.examplePath = examplePath;
    this.container = container;
    this.syntaxHighlighter = new SyntaxHighlighter();

    // Create editor elements
    this.editorElement = this.createEditorElement();
    this.highlightOverlay = this.createHighlightOverlay();

    // Setup container
    this.container.style.position = 'relative';
    this.container.appendChild(this.highlightOverlay);
    this.container.appendChild(this.editorElement);

    // Create message pane
    this.messagesPane = new MessagePane(messagesContainer);
    this.messagesPane.syncScrollWith(this.editorElement);

    // Setup event handlers
    this.setupEventHandlers();
  }

  private createEditorElement(): HTMLElement {
    const editor = document.createElement('textarea');
    editor.className = 'nelumbo-editor';
    editor.spellcheck = false;
    editor.autocapitalize = 'off';
    editor.autocomplete = 'off';

    // Styling
    Object.assign(editor.style, {
      position: 'absolute',
      top: '0',
      left: '0',
      width: '100%',
      height: '100%',
      margin: '0',
      padding: '16px',
      border: 'none',
      outline: 'none',
      fontFamily: "'JetBrains Mono', 'Fira Code', 'Consolas', monospace",
      fontSize: '14px',
      lineHeight: '1.5',
      tabSize: '4',
      background: 'transparent',
      color: 'transparent',
      caretColor: EditorTheme.isDarkTheme() ? '#fff' : '#000',
      resize: 'none',
      overflow: 'auto',
      whiteSpace: 'pre-wrap',
      wordBreak: 'break-word',
      zIndex: '2',
    });

    if (this.isExample) {
      editor.readOnly = true;
    }

    return editor as HTMLElement;
  }

  private createHighlightOverlay(): HTMLElement {
    const overlay = document.createElement('div');
    overlay.className = 'nelumbo-highlight-overlay';

    Object.assign(overlay.style, {
      position: 'absolute',
      top: '0',
      left: '0',
      width: '100%',
      height: '100%',
      padding: '16px',
      fontFamily: "'JetBrains Mono', 'Fira Code', 'Consolas', monospace",
      fontSize: '14px',
      lineHeight: '1.5',
      tabSize: '4',
      background: EditorTheme.isDarkTheme() ? '#1e1e2e' : '#ffffff',
      color: EditorTheme.isDarkTheme() ? '#f8f8f2' : '#000000',
      overflow: 'hidden',
      whiteSpace: 'pre-wrap',
      wordBreak: 'break-word',
      pointerEvents: 'none',
      zIndex: '1',
    });

    return overlay;
  }

  private setupEventHandlers(): void {
    const textarea = this.editorElement as HTMLTextAreaElement;

    // Text input handler - update overlay immediately, debounce parsing
    textarea.addEventListener('input', () => {
      // Show text immediately (without highlighting) for responsive feel
      this.updateOverlayText(textarea.value);
      this.scheduleRefresh();
    });

    // Scroll sync
    textarea.addEventListener('scroll', () => {
      this.highlightOverlay.scrollTop = textarea.scrollTop;
      this.highlightOverlay.scrollLeft = textarea.scrollLeft;
    });

    // Font size adjustment
    textarea.addEventListener('keydown', (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && (e.key === '+' || e.key === '=')) {
        e.preventDefault();
        this.increaseFontSize();
      } else if ((e.ctrlKey || e.metaKey) && e.key === '-') {
        e.preventDefault();
        this.decreaseFontSize();
      }
    });

    // Handle example window edit attempts
    if (this.isExample) {
      textarea.addEventListener('keydown', (e: KeyboardEvent) => {
        if (!e.ctrlKey && !e.metaKey && !e.altKey) {
          const isPrintable = e.key.length === 1;
          const isDelete = e.key === 'Backspace' || e.key === 'Delete';
          if (isPrintable || isDelete) {
            this.promptConvertToEditable();
            e.preventDefault();
          }
        }
      });
    }
  }

  private scheduleRefresh(): void {
    if (this.debounceTimer !== null) {
      window.clearTimeout(this.debounceTimer);
    }
    this.debounceTimer = window.setTimeout(() => {
      this.refresh();
    }, 150);
  }

  /**
   * Refresh the editor (re-tokenize and parse).
   */
  refresh(): void {
    const code = this.getContent();
    const fileName = this.getFileName();

    // Tokenize
    const tokenizer = new Tokenizer(code, fileName);
    this.lastTokenizerResult = tokenizer.tokenize();

    // Parse with a fresh knowledge base (derived from BASE)
    const kb = new KnowledgeBase(KnowledgeBase.BASE);
    const parser = new Parser(kb, this.lastTokenizerResult);
    this.lastParserResult = parser.parseMultipleNonThrowing();

    // Update syntax highlighting (after parsing, so tokens have node references)
    this.syntaxHighlighter.setTokenizerResult(this.lastTokenizerResult);
    this.updateHighlighting(code);

    // Show results/errors in message pane
    this.showResults(this.lastParserResult, kb);

    // Notify change handlers
    this.changeHandlers.forEach(handler => handler(this));
  }

  private updateHighlighting(code: string): void {
    const highlighted = this.syntaxHighlighter.highlightCode(code);
    this.highlightOverlay.innerHTML = highlighted + '<br>'; // Extra br for scrolling
  }

  /**
   * Update overlay with plain text (for immediate feedback while typing).
   */
  private updateOverlayText(text: string): void {
    // Escape HTML and preserve whitespace
    const escaped = text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');
    this.highlightOverlay.innerHTML = escaped + '<br>';
  }

  /**
   * Show parser results and errors in the message pane.
   */
  private showResults(result: ParserResult, kb: KnowledgeBase): void {
    this.messagesPane.clear();

    const exceptions = result.exceptions();
    if (!exceptions.isEmpty()) {
      // Show parse errors
      exceptions.forEach(exc => {
        this.messagesPane.addError(exc.line, exc.position, exc.shortMessage, exc.fullMessage);
      });
    } else {
      // Evaluate and show results
      const throwing = new ParserResult(result.tokenizerResult(), true);

      for (const root of result.roots()) {
        if (isEvaluatable(root)) {
          try {
            root.evaluate(kb, throwing);
          } catch (e) {
            if (e instanceof ParseException) {
              // Highlight error in editor
              this.messagesPane.addError(e.line, e.position, e.shortMessage, e.fullMessage);
            }
          }

          // If it's a Query, show the result
          if (root instanceof Query && root.inferResult() !== null) {
            const resultStr = root.inferResult()!.toString();
            const lastToken = root.lastToken();
            if (lastToken) {
              this.messagesPane.addResult(lastToken.line, lastToken.position, resultStr);
            }
          }
        }
      }
    }

    this.messagesPane.render();
  }

  private increaseFontSize(): void {
    const currentSize = parseFloat(this.editorElement.style.fontSize) || 14;
    const newSize = Math.min(currentSize * 1.2, 48);
    this.setFontSize(newSize);
  }

  private decreaseFontSize(): void {
    const currentSize = parseFloat(this.editorElement.style.fontSize) || 14;
    const newSize = Math.max(currentSize / 1.2, 8);
    this.setFontSize(newSize);
  }

  private setFontSize(size: number): void {
    const sizeStr = `${size}px`;
    this.editorElement.style.fontSize = sizeStr;
    this.highlightOverlay.style.fontSize = sizeStr;
  }

  private promptConvertToEditable(): void {
    if (confirm('This is a read-only example. Would you like to create an editable copy?')) {
      this.convertToEditable();
    }
  }

  /**
   * Convert this example window to an editable window.
   */
  convertToEditable(): void {
    this.isExample = false;
    (this.editorElement as HTMLTextAreaElement).readOnly = false;
    this.title = `Copy of ${this.title}`;
    this.changeHandlers.forEach(handler => handler(this));
  }

  // Public API

  /**
   * Get the window ID.
   */
  getId(): string {
    return this.id;
  }

  /**
   * Get the window title.
   */
  getTitle(): string {
    return this.title;
  }

  /**
   * Set the window title.
   */
  setTitle(title: string): void {
    this.title = title;
  }

  /**
   * Get the file name for this editor.
   */
  getFileName(): string {
    if (this.isExample && this.examplePath) {
      const parts = this.examplePath.split('/');
      return parts[parts.length - 1];
    }
    return `editor.nelumbo_${this.id}.nl`;
  }

  /**
   * Check if this is an example window.
   */
  getIsExample(): boolean {
    return this.isExample;
  }

  /**
   * Get the editor content.
   */
  getContent(): string {
    return (this.editorElement as HTMLTextAreaElement).value;
  }

  /**
   * Set the editor content.
   */
  setContent(content: string): void {
    (this.editorElement as HTMLTextAreaElement).value = content;
    this.refresh();
  }

  /**
   * Get the last tokenizer result.
   */
  getTokenizerResult(): TokenizerResult | null {
    return this.lastTokenizerResult;
  }

  /**
   * Get the last parser result.
   */
  getParserResult(): ParserResult | null {
    return this.lastParserResult;
  }

  /**
   * Focus the editor.
   */
  focus(): void {
    this.editorElement.focus();
  }

  /**
   * Add a change handler.
   */
  onChange(handler: EditorChangeHandler): void {
    this.changeHandlers.push(handler);
  }

  /**
   * Remove a change handler.
   */
  offChange(handler: EditorChangeHandler): void {
    const index = this.changeHandlers.indexOf(handler);
    if (index >= 0) {
      this.changeHandlers.splice(index, 1);
    }
  }

  /**
   * Save state to localStorage.
   */
  saveState(): void {
    const state: EditorWindowState = {
      id: this.id,
      title: this.title,
      content: this.getContent(),
      isExample: this.isExample,
      examplePath: this.examplePath,
      caretPosition: (this.editorElement as HTMLTextAreaElement).selectionStart,
      selectionStart: (this.editorElement as HTMLTextAreaElement).selectionStart,
      selectionEnd: (this.editorElement as HTMLTextAreaElement).selectionEnd,
    };

    try {
      localStorage.setItem(`nelumbo.window.${this.id}`, JSON.stringify(state));
    } catch {
      // Ignore storage errors
    }
  }

  /**
   * Load state from localStorage.
   */
  loadState(): boolean {
    try {
      const stored = localStorage.getItem(`nelumbo.window.${this.id}`);
      if (stored) {
        const state: EditorWindowState = JSON.parse(stored);
        this.title = state.title;
        this.isExample = state.isExample;
        this.examplePath = state.examplePath;
        this.setContent(state.content);

        // Restore selection
        const textarea = this.editorElement as HTMLTextAreaElement;
        textarea.setSelectionRange(state.selectionStart, state.selectionEnd);

        return true;
      }
    } catch {
      // Ignore errors
    }
    return false;
  }

  /**
   * Clear saved state.
   */
  clearState(): void {
    try {
      localStorage.removeItem(`nelumbo.window.${this.id}`);
    } catch {
      // Ignore errors
    }
  }

  /**
   * Dispose of this window.
   */
  dispose(): void {
    if (this.debounceTimer !== null) {
      window.clearTimeout(this.debounceTimer);
    }
    this.changeHandlers = [];
    this.container.innerHTML = '';
  }
}
