/**
 * InferContext - execution context for logical inference.
 * Ported from Java: org.modelingvalue.nelumbo.InferContext
 */

import { List, Map } from 'immutable';
import type { KnowledgeBase } from '../kb/KnowledgeBase';
import type { Predicate } from './Predicate';
import type { InferResult } from './InferResult';

/**
 * InferContext - context for logical inference.
 */
export class InferContext {
  private readonly _knowledgeBase: KnowledgeBase;
  private readonly _stack: List<Predicate>;
  private readonly _bindings: Map<unknown, unknown>;
  private readonly _negated: boolean;
  private readonly _cycleCheck: boolean;
  private readonly _trace: boolean;
  private readonly _cycleResults: Map<Predicate, InferResult>;
  private readonly _reduce: boolean;
  private readonly _shallow: boolean;

  private constructor(
    knowledgeBase: KnowledgeBase,
    stack: List<Predicate>,
    bindings: Map<unknown, unknown>,
    negated: boolean,
    cycleCheck: boolean,
    trace: boolean,
    cycleResults: Map<Predicate, InferResult> = Map(),
    reduce: boolean = false,
    shallow: boolean = false
  ) {
    this._knowledgeBase = knowledgeBase;
    this._stack = stack;
    this._bindings = bindings;
    this._negated = negated;
    this._cycleCheck = cycleCheck;
    this._trace = trace;
    this._cycleResults = cycleResults;
    this._reduce = reduce;
    this._shallow = shallow;
  }

  /**
   * Create a new context.
   */
  static of(
    knowledgeBase: KnowledgeBase,
    stack: List<Predicate>,
    bindings: Map<unknown, unknown>,
    negated: boolean,
    cycleCheck: boolean,
    trace: boolean
  ): InferContext {
    return new InferContext(knowledgeBase, stack, bindings, negated, cycleCheck, trace);
  }

  /**
   * Get the knowledge base.
   */
  knowledgeBase(): KnowledgeBase {
    return this._knowledgeBase;
  }

  /**
   * Get the inference stack.
   */
  stack(): List<Predicate> {
    return this._stack;
  }

  /**
   * Get the bindings.
   */
  bindings(): Map<unknown, unknown> {
    return this._bindings;
  }

  /**
   * Check if negated.
   */
  negated(): boolean {
    return this._negated;
  }

  /**
   * Check if cycle checking is enabled.
   */
  cycleCheck(): boolean {
    return this._cycleCheck;
  }

  /**
   * Check if tracing is enabled.
   */
  trace(): boolean {
    return this._trace;
  }

  /**
   * Check if reducing.
   */
  reduce(): boolean {
    return this._reduce;
  }

  /**
   * Check if shallow.
   */
  shallow(): boolean {
    return this._shallow;
  }

  /**
   * Get prefix for trace output.
   */
  prefix(): string {
    return '  '.repeat(this._stack.size);
  }

  /**
   * Push a predicate onto the stack.
   */
  push(predicate: Predicate): InferContext {
    return new InferContext(
      this._knowledgeBase,
      this._stack.push(predicate),
      this._bindings,
      this._negated,
      this._cycleCheck,
      this._trace,
      this._cycleResults,
      this._reduce,
      this._shallow
    );
  }

  /**
   * Push a predicate onto the stack (alias for push).
   */
  pushOnStack(predicate: Predicate): InferContext {
    return this.push(predicate);
  }

  /**
   * Enter negation.
   */
  negate(): InferContext {
    return new InferContext(
      this._knowledgeBase,
      this._stack,
      this._bindings,
      !this._negated,
      this._cycleCheck,
      this._trace,
      this._cycleResults,
      this._reduce,
      this._shallow
    );
  }

  /**
   * Get cycle result for a predicate.
   */
  getCycleResult(predicate: Predicate): InferResult | null {
    return this._cycleResults.get(predicate) ?? null;
  }

  /**
   * Put a cycle result for a predicate.
   */
  putCycleResult(predicate: Predicate, result: InferResult): InferContext {
    return new InferContext(
      this._knowledgeBase,
      this._stack,
      this._bindings,
      this._negated,
      this._cycleCheck,
      this._trace,
      this._cycleResults.set(predicate, result),
      this._reduce,
      this._shallow
    );
  }

  /**
   * Create a shallow context.
   */
  withShallow(shallow: boolean): InferContext {
    return new InferContext(
      this._knowledgeBase,
      this._stack,
      this._bindings,
      this._negated,
      this._cycleCheck,
      this._trace,
      this._cycleResults,
      this._reduce,
      shallow
    );
  }

  /**
   * Create a reduce context.
   */
  withReduce(reduce: boolean): InferContext {
    return new InferContext(
      this._knowledgeBase,
      this._stack,
      this._bindings,
      this._negated,
      this._cycleCheck,
      this._trace,
      this._cycleResults,
      reduce,
      this._shallow
    );
  }
}
