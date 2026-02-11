/**
 * Parser - precedence climbing parser.
 * @JAVA_REF org.modelingvalue.nelumbo.syntax.Parser
 */

import { List } from 'immutable';
import type { Token } from './Token';
import type { Node } from '../Node';
import { TOP_GROUP } from '../Type';
import { Variable } from '../Variable';
import { Tokenizer, type TokenizerResult } from './Tokenizer';
import type { KnowledgeBase } from '../KnowledgeBase';
import type { ParseContext } from './ParseContext';
import { createParseContext } from './ParseContext';
import type { ParseState } from './ParseState';
import { PatternResult } from './PatternResult';
import { ParserResult } from './ParserResult';
import { ParseException } from './ParseException';

/**
 * Parser - parses tokenized input using precedence climbing.
 */
export class Parser {
  private readonly _knowledgeBase: KnowledgeBase;
  private readonly _tokenizerResult: TokenizerResult;

  private _result: ParserResult | null = null;

  constructor(knowledgeBase: KnowledgeBase, tokenizerResult: TokenizerResult) {
    this._knowledgeBase = knowledgeBase;
    this._tokenizerResult = tokenizerResult;
  }

  /**
   * Parse without throwing on errors.
   */
  parseNonThrowing(): ParserResult {
    try {
      return this.parse(new ParserResult(this._tokenizerResult, false), false);
    } catch (e) {
      if (e instanceof ParseException) {
        throw new Error('Unexpected ParseException in non-throwing mode: ' + e.message);
      }
      throw e;
    }
  }

  /**
   * Parse and throw on errors.
   */
  parseThrowing(): ParserResult {
    return this.parse(new ParserResult(this._tokenizerResult, true), false);
  }

  /**
   * Parse and evaluate.
   */
  parseEvaluate(): ParserResult {
    const result = this.parse(new ParserResult(this._tokenizerResult, true), false);
    result.evaluate();
    return result;
  }

  /**
   * Parse multiple roots without throwing.
   */
  parseMultipleNonThrowing(): ParserResult {
    try {
      return this.parse(new ParserResult(this._tokenizerResult, false), true);
    } catch (e) {
      if (e instanceof ParseException) {
        throw new Error('Unexpected ParseException in non-throwing mode: ' + e.message);
      }
      throw e;
    }
  }

  private parse(result: ParserResult, multiple: boolean): ParserResult {
    return this._knowledgeBase.run(() => {
      this._result = result;
      this._knowledgeBase.setExceptionHandler(this);
      Parser._parseNodeCount = 0;

      try {
        let token = this._tokenizerResult.first;
        // @JAVA_REF Parser.parse() — use two-arg ParseContext.of(group, precedence)
        const ctx = createParseContext(TOP_GROUP, Number.MIN_SAFE_INTEGER);
        const node = this.parseNode(token, ctx);

        if (node !== null) {
          result.setRoot(node);
          token = node.nextToken();
          if (token !== null) {
            this.addException(
              ParseException.fromToken('Unexpected token ' + token + ' after end of input', token)
            );
          }
        } else if (this.exceptions().isEmpty()) {
          this.addException(
            ParseException.fromToken('No syntax pattern found for ' + token, token!)
          );
        }

        result.checkAssertions();
        return result;
      } finally {
        this._knowledgeBase.endParsing(multiple);
        this._result = null;
      }
    });
  }

  /**
   * Parse a single node.
   */
  private static _parseNodeDepth = 0;
  private static _parseNodeCount = 0;
  // @JAVA_REF Parser.parseNode(Token, ParseContext)
  parseNode(token: Token | null, ctx: ParseContext): Node | null {
    Parser._parseNodeDepth++;
    Parser._parseNodeCount++;

    // Safety limits to prevent browser hangs
    if (Parser._parseNodeDepth > 1000) {
      Parser._parseNodeDepth--;
      throw new Error('Parser.parseNode: Maximum recursion depth exceeded');
    }
    if (Parser._parseNodeCount > 50000) {
      throw new Error('Parser.parseNode: Maximum call count exceeded');
    }

    try {
      // Pre-parse phase
      let result = this.preParse(token, ctx, null);
      if (result === null) {
        return null;
      }

      // Post-parse phase
      let left = result.postParse(ctx);
      if (left !== null && ctx.precedence() < Number.MAX_SAFE_INTEGER) {
        token = left.nextToken();
        if (token !== null) {
          result = this.preParse(token, ctx, left);

          while (result !== null) {
            if (ctx.precedence() >= (result.leftPrecedence() ?? Number.MAX_SAFE_INTEGER)) {
              return left;
            }

            left = result.postParse(ctx);
            if (left === null) {
              break;
            }

            token = left.nextToken();
            if (token === null) {
              return left;
            }

            result = this.preParse(token, ctx, left);
          }
        }
      }

      return left;
    } finally {
      Parser._parseNodeDepth--;
    }
  }

  /**
   * Get group state from knowledge base.
   */
  groupState(group: string): ParseState | null {
    return this._knowledgeBase.groupState(group);
  }

  /**
   * Look up a variable.
   */
  variable(token: Token, ctx: ParseContext): Variable | null {
    return this._knowledgeBase.variable(token, ctx.group(), this);
  }

  /**
   * Pre-parse phase.
   */
  preParse(token: Token | null, ctx: ParseContext, left: Node | null): PatternResult | null {
    return this._knowledgeBase.preParse(token, ctx, left, this);
  }

  /**
   * Get the knowledge base.
   */
  knowledgeBase(): KnowledgeBase {
    return this._knowledgeBase;
  }

  /**
   * Add an exception.
   */
  addException(exception: ParseException): void {
    if (this._result !== null) {
      this._result.addException(exception);
    }
  }

  /**
   * Get all exceptions.
   */
  exceptions(): List<ParseException> {
    return this._result?.exceptions() ?? List();
  }
}

/**
 * Static parse methods.
 */
export function parseString(knowledgeBase: KnowledgeBase, input: string): List<Node> {
  const tokenizer = new Tokenizer(input + '\n', input);
  const parser = new Parser(knowledgeBase, tokenizer.tokenize());
  return parser.parseEvaluate().roots();
}

export function parseTokenizerResult(knowledgeBase: KnowledgeBase, tokenizerResult: TokenizerResult): ParserResult {
  return new Parser(knowledgeBase, tokenizerResult).parseNonThrowing();
}
