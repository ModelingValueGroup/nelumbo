/**
 * InferContext - execution context for logical inference.
 * @JAVA_REF org.modelingvalue.nelumbo.InferContext
 */

import { List, Map } from 'immutable';
import type { KnowledgeBase } from './KnowledgeBase';
import type { Predicate } from './logic/Predicate';
import type { InferResult } from './InferResult';

/**
 * InferContext - context for logical inference.
 * @JAVA_REF org.modelingvalue.nelumbo.InferContext
 */
export class InferContext {
  private readonly _knowledgeBase: KnowledgeBase;
  private readonly _stack: List<Predicate>;
  private readonly _cycleResults: Map<Predicate, InferResult>;
  private readonly _shallow: boolean;
  private readonly _reduce: boolean;
  private readonly _trace: boolean;

  private constructor(
    knowledgeBase: KnowledgeBase,
    stack: List<Predicate>,
    cycleResults: Map<Predicate, InferResult>,
    shallow: boolean,
    reduce: boolean,
    trace: boolean
  ) {
    this._knowledgeBase = knowledgeBase;
    this._stack = stack;
    this._cycleResults = cycleResults;
    this._shallow = shallow;
    this._reduce = reduce;
    this._trace = trace;
  }

  /**
   * Create a new context.
   * @JAVA_REF org.modelingvalue.nelumbo.InferContext#of
   */
  static of(
    knowledgeBase: KnowledgeBase,
    stack: List<Predicate>,
    cyclic: Map<Predicate, InferResult>,
    shallow: boolean,
    reduce: boolean,
    trace: boolean
  ): InferContext {
    return new InferContext(knowledgeBase, stack, cyclic, shallow, reduce, trace);
  }

  /**
   * Get the knowledge base.
   * @JAVA_REF org.modelingvalue.nelumbo.InferContext#knowledgebase()
   */
  knowledgeBase(): KnowledgeBase {
    return this._knowledgeBase;
  }

  /**
   * Get the inference stack.
   * @JAVA_REF org.modelingvalue.nelumbo.InferContext#stack()
   */
  stack(): List<Predicate> {
    return this._stack;
  }

  /**
   * Check if shallow.
   * @JAVA_REF org.modelingvalue.nelumbo.InferContext#shallow()
   */
  shallow(): boolean {
    return this._shallow;
  }

  /**
   * Check if reducing.
   * @JAVA_REF org.modelingvalue.nelumbo.InferContext#reduce()
   */
  reduce(): boolean {
    return this._reduce;
  }

  /**
   * Check if deep (neither shallow nor reduce).
   * @JAVA_REF org.modelingvalue.nelumbo.InferContext#deep()
   */
  deep(): boolean {
    return !this._shallow && !this._reduce;
  }

  /**
   * Check if tracing is enabled.
   * @JAVA_REF org.modelingvalue.nelumbo.InferContext#trace()
   */
  trace(): boolean {
    return this._trace;
  }

  /**
   * Get prefix for trace output.
   * @JAVA_REF org.modelingvalue.nelumbo.InferContext#prefix()
   */
  prefix(): string {
    return '  '.repeat(this._stack.size);
  }

  /**
   * Push a predicate onto the stack. Resets shallow and reduce to false.
   * @JAVA_REF org.modelingvalue.nelumbo.InferContext#pushOnStack(Predicate)
   */
  pushOnStack(predicate: Predicate): InferContext {
    return InferContext.of(
      this._knowledgeBase,
      this._stack.push(predicate),
      this._cycleResults,
      false,
      false,
      this._trace
    );
  }

  /**
   * Put a cycle result for a predicate. Resets shallow and reduce to false.
   * @JAVA_REF org.modelingvalue.nelumbo.InferContext#putCycleResult(Predicate, InferResult)
   */
  putCycleResult(predicate: Predicate, result: InferResult): InferContext {
    return InferContext.of(
      this._knowledgeBase,
      this._stack,
      this._cycleResults.set(predicate, result),
      false,
      false,
      this._trace
    );
  }

  /**
   * Switch to shallow mode.
   * @JAVA_REF org.modelingvalue.nelumbo.InferContext#toShallow()
   */
  toShallow(): InferContext {
    return InferContext.of(
      this._knowledgeBase,
      this._stack,
      this._cycleResults,
      true,
      false,
      this._trace
    );
  }

  /**
   * Switch to reduce mode.
   * @JAVA_REF org.modelingvalue.nelumbo.InferContext#toReduce()
   */
  toReduce(): InferContext {
    return InferContext.of(
      this._knowledgeBase,
      this._stack,
      this._cycleResults,
      false,
      true,
      this._trace
    );
  }

  /**
   * Switch to deep mode (neither shallow nor reduce).
   * @JAVA_REF org.modelingvalue.nelumbo.InferContext#toDeep()
   */
  toDeep(): InferContext {
    return InferContext.of(
      this._knowledgeBase,
      this._stack,
      this._cycleResults,
      false,
      false,
      this._trace
    );
  }

  /**
   * Get cycle result for a predicate.
   * @JAVA_REF org.modelingvalue.nelumbo.InferContext#getCycleResult(Predicate)
   */
  getCycleResult(predicate: Predicate): InferResult | null {
    const result = this._cycleResults.get(predicate) ?? null;
    return result !== null ? result.cast(predicate) : null;
  }
}
