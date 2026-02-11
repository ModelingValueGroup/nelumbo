/**
 * ParserResult - full parse result with error collection.
 * @JAVA_REF org.modelingvalue.nelumbo.syntax.ParserResult
 */

import { List } from 'immutable';
import type { Node } from '../Node';
import type { TokenizerResult } from './Tokenizer';
import { ParseException } from './ParseException';

/**
 * ParserResult - result of parsing.
 */
export class ParserResult {
  private readonly _tokenizerResult: TokenizerResult;
  private readonly _throwing: boolean;

  private _exceptions: ParseException[] = [];
  private _root: Node | null = null;

  constructor(tokenizerResult: TokenizerResult, throwing: boolean) {
    this._tokenizerResult = tokenizerResult;
    this._throwing = throwing;
  }

  /**
   * Get the tokenizer result.
   */
  tokenizerResult(): TokenizerResult {
    return this._tokenizerResult;
  }

  /**
   * Get all root nodes.
   */
  roots(): List<Node> {
    // Check if root is a list node
    if (this._root !== null && this.isListNode(this._root)) {
      return (this._root as unknown as { elementsFlattened(): List<Node> }).elementsFlattened();
    }
    return this._root !== null ? List([this._root]) : List();
  }

  private isListNode(node: Node): boolean {
    return 'elementsFlattened' in node && typeof (node as unknown as { elementsFlattened: unknown }).elementsFlattened === 'function';
  }

  /**
   * Get the single root node.
   */
  root(): Node | null {
    return this._root;
  }

  /**
   * Set the root node.
   */
  setRoot(root: Node): void {
    this._root = root;
  }

  /**
   * Add an exception.
   */
  addException(exception: ParseException): void {
    if (this._throwing) {
      throw exception;
    }
    this._exceptions.push(exception);
  }

  /**
   * Get all exceptions.
   */
  exceptions(): List<ParseException> {
    return List(this._exceptions);
  }

  /**
   * Throw the first exception if any.
   */
  throwException(): void {
    if (this._exceptions.length > 0) {
      throw this._exceptions[0];
    }
  }

  /**
   * Evaluate all root nodes.
   */
  evaluate(): void {
    if (this._exceptions.length === 0) {
      for (const root of this.roots()) {
        if (this.isEvaluatable(root)) {
          // root.evaluate(knowledgeBase, this);
        }
      }
    }
  }

  private isEvaluatable(node: Node): boolean {
    return 'evaluate' in node && typeof (node as unknown as { evaluate: unknown }).evaluate === 'function';
  }

  /**
   * Print results to console.
   */
  print(): void {
    for (const exc of this._exceptions) {
      console.log(exc.fullMessage);
    }
    for (const root of this.roots()) {
      console.log(root.toString());
    }
  }

  /**
   * Check assertions (debug only).
   */
  checkAssertions(): void {
    // Assertions disabled in production
  }
}
