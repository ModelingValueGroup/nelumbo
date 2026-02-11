/**
 * Token class representing a single lexical token.
 * @JAVA_REF org.modelingvalue.nelumbo.syntax.Token
 */

import { TokenType } from './TokenType';
import type { AstElement } from '../AstElement';
import type { Node } from '../Node';
import type { Variable } from '../Variable';
import { Type } from '../Type';

export class Token implements AstElement {
  // Type and content
  readonly type: TokenType;
  readonly text: string;

  // Position information (0-based)
  readonly line: number;
  readonly position: number;  // column in line
  readonly index: number;     // character position in input
  readonly fileName: string;

  // Multi-line support (calculated)
  readonly numLines: number;
  readonly numChars: number;
  readonly lineEnd: number;
  readonly positionEnd: number;
  readonly indexEnd: number;
  readonly lastLine: number;
  readonly lastPosition: number;

  // Linked list pointers
  private _next: Token | null = null;
  private _previous: Token | null = null;
  private _nextAll: Token | null = null;
  private _previousAll: Token | null = null;

  // Semantic info
  private _isKeyword: boolean = false;
  private _isTextMatch: boolean = false;
  private _colorType: TokenType;
  private _node: Node | null = null;
  private _state: unknown = null;

  constructor(
    type: TokenType,
    text: string,
    line: number,
    position: number,
    index: number,
    fileName: string
  ) {
    this.type = type;
    this.text = text;
    this.line = line;
    this.position = position;
    this.index = index;
    this.fileName = fileName;

    // Calculate derived fields
    this.numChars = text.length;
    this.numLines = Token.countNewlines(text);
    this.indexEnd = index + text.length;

    if (this.numLines === 0) {
      this.lineEnd = line;
      this.positionEnd = position + text.length;
      this.lastLine = line;
      this.lastPosition = position + text.length - 1;
    } else {
      this.lineEnd = line + this.numLines;
      this.positionEnd = Token.lastLineLength(text);
      this.lastLine = line + this.numLines;
      this.lastPosition = this.positionEnd - 1;
    }

    this._colorType = type;
  }

  // Navigation
  get next(): Token | null {
    return this._next;
  }

  get previous(): Token | null {
    return this._previous;
  }

  get nextAll(): Token | null {
    return this._nextAll;
  }

  get previousAll(): Token | null {
    return this._previousAll;
  }

  setNext(next: Token | null): void {
    this._next = next;
  }

  setPrevious(previous: Token | null): void {
    this._previous = previous;
  }

  setNextAll(next: Token | null): void {
    this._nextAll = next;
  }

  setPreviousAll(previous: Token | null): void {
    this._previousAll = previous;
  }

  // Semantic info
  get isKeyword(): boolean {
    return this._isKeyword;
  }

  get isTextMatch(): boolean {
    return this._isTextMatch;
  }

  setTextMatch(isKeyword: boolean): void {
    this._isTextMatch = true;
    this._isKeyword = isKeyword;
  }

  /**
   * Returns the token type to use for syntax highlighting.
   * This considers semantic information from the parsed AST.
   */
  get colorType(): TokenType {
    // Check if this token represents a variable (from the AST)
    if (this.isVariableNode()) {
      return TokenType.VARIABLE;
    }
    // Check if this is a NAME token representing a type
    if (this.type === TokenType.NAME && this.isTypeNode()) {
      return TokenType.TYPE;
    }
    // Check if this is a keyword or literal
    if (this.type === TokenType.NAME && this._isTextMatch && (this._isKeyword || this.isLiteralNode())) {
      return TokenType.KEYWORD;
    }
    // Check if < or > are part of a pattern (meta operators)
    if ((this.text === '<' || this.text === '>') && this.isPatternNode()) {
      return TokenType.META_OPERATOR;
    }
    // Fallback to text match logic
    if (this._isTextMatch) {
      return this._isKeyword ? TokenType.KEYWORD : this.type;
    }
    return this._colorType;
  }

  setColorType(type: TokenType): void {
    this._colorType = type;
  }

  /**
   * Check if this is a skip token (whitespace, comments).
   */
  get skip(): boolean {
    return this.type.isSkip();
  }

  /**
   * Check if a position is contained within this token.
   */
  contains(line: number, col: number): boolean {
    if (line < this.line || line > this.lastLine) {
      return false;
    }
    if (line === this.line && col < this.position) {
      return false;
    }
    if (line === this.lastLine && col > this.lastPosition) {
      return false;
    }
    return true;
  }

  /**
   * Get list of tokens from this token to the end token (inclusive).
   */
  list(end: Token | null): Token[] {
    const result: Token[] = [];
    let current: Token | null = this;
    while (current !== null) {
      result.push(current);
      if (current === end) {
        break;
      }
      current = current._next;
    }
    return result;
  }

  /**
   * Get list of all tokens (including skipped) from this to end.
   */
  listAll(end: Token | null): Token[] {
    const result: Token[] = [];
    let current: Token | null = this;
    while (current !== null) {
      result.push(current);
      if (current === end) {
        break;
      }
      current = current._nextAll;
    }
    return result;
  }

  // Utility functions
  private static countNewlines(text: string): number {
    let count = 0;
    for (let i = 0; i < text.length; i++) {
      const c = text[i];
      if (c === '\n') {
        count++;
      } else if (c === '\r') {
        count++;
        // Skip \n if it follows \r (Windows line ending)
        if (i + 1 < text.length && text[i + 1] === '\n') {
          i++;
        }
      }
    }
    return count;
  }

  private static lastLineLength(text: string): number {
    const lastNewline = Math.max(text.lastIndexOf('\n'), text.lastIndexOf('\r'));
    if (lastNewline === -1) {
      return text.length;
    }
    return text.length - lastNewline - 1;
  }

  toString(): string {
    return `Token(${this.type.name}, "${this.text}", ${this.line}:${this.position})`;
  }

  // AstElement implementation
  firstToken(): Token {
    return this;
  }

  lastToken(): Token {
    return this;
  }

  deparse(sb: string[]): void {
    sb.push(this.text);
  }

  // Node reference (set during parsing)
  get node(): Node | null {
    return this._node;
  }

  setNode(node: Node): void {
    this._node = node;
  }

  /**
   * Get the variable associated with this token's node.
   */
  variable(): Variable | null {
    return this._node?.variable() ?? null;
  }

  /**
   * Check if this token represents a variable node.
   */
  isVariableNode(): boolean {
    return this.variable() !== null;
  }

  /**
   * Check if this token represents a type node.
   */
  isTypeNode(): boolean {
    return this._node instanceof Type;
  }

  /**
   * Check if this token represents a pattern node.
   */
  isPatternNode(): boolean {
    return this._node !== null && Type.PATTERN.isAssignableFrom(this._node.type());
  }

  /**
   * Check if this token represents a literal node.
   */
  isLiteralNode(): boolean {
    return this._node !== null && Type.LITERAL.isAssignableFrom(this._node.type());
  }

  // Parse state (for parser)
  // @JAVA_REF Token.getState()
  get state(): unknown {
    return this._state;
  }

  // @JAVA_REF Token.setState(ParseState)
  setState(state: unknown): void {
    this._state = state;
  }

  /**
   * Get completions available at this token's parse state.
   * @JAVA_REF Token.completions()
   */
  completions(): string[] {
    if (this._state !== null && typeof this._state === 'object' && 'tokenTexts' in this._state) {
      const state = this._state as { tokenTexts(): { keys(): Iterable<string> } };
      return [...state.tokenTexts().keys()].sort();
    }
    return [];
  }

  /**
   * Split this token at the given position.
   * Returns a new token with the first part.
   */
  split(position: number): Token {
    const firstText = this.text.substring(0, position);
    const secondText = this.text.substring(position);

    // Create new token for first part
    const t1 = new Token(
      TokenType.of(firstText),
      firstText,
      this.line,
      this.position,
      this.index,
      this.fileName
    );

    // Create new token for second part
    const t2 = new Token(
      TokenType.of(secondText),
      secondText,
      this.line,
      this.position + position,
      this.index + position,
      this.fileName
    );

    // Link: t1 → t2 → original.next (preserving the chain)
    t1._next = t2;
    t1._nextAll = t2;
    t2._next = this._next;
    t2._nextAll = this._nextAll;

    return t1;
  }

  /**
   * Prepend text to this token.
   */
  prepend(text: string): Token {
    const merge = new Token(
      TokenType.of(text + this.text),
      text + this.text,
      this.line,
      this.position - text.length,
      this.index - text.length,
      this.fileName
    );
    // Preserve the token chain links (matching Java: merge.next = next; merge.nextAll = nextAll;)
    merge.setNext(this._next);
    merge.setNextAll(this._nextAll);
    return merge;
  }

  /**
   * Connect a split token.
   */
  connect(t1: Token): void {
    // Insert t1 → t2 in place of this token in the chain
    // t1 is the first part, t2 = t1.next is the second part (set during split)
    const t2 = t1._next!;
    t2._previous = t1;
    t2._previousAll = t1;
    if (this._previous !== null) {
      this._previous.setNext(t1);
    }
    if (this._previousAll !== null) {
      this._previousAll.setNextAll(t1);
    }
    if (this._next !== null) {
      this._next.setPrevious(t2);
    }
    if (this._nextAll !== null) {
      this._nextAll.setPreviousAll(t2);
    }
  }

  /**
   * Merge with another token.
   */
  merge(other: Token): void {
    // Replace this token with the merged token
    if (this._previous !== null) {
      this._previous.setNext(other);
      other.setPrevious(this._previous);
    }
    if (other.next !== null) {
      other.next.setPrevious(other);
    }
  }

  /**
   * Get the next token (possibly skipping to a split).
   */
  nextToken(): Token | null {
    return this._next;
  }
}
