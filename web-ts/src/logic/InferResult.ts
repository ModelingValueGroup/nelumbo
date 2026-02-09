/**
 * InferResult - three-state logic result.
 * Ported from Java: org.modelingvalue.nelumbo.InferResult
 */

import { List, Map, Set } from 'immutable';
import type { Variable } from '../core/Variable';
import type { Predicate } from './Predicate';

/**
 * InferResult - represents the result of logical inference.
 */
export class InferResult {
  private readonly _facts: Set<Predicate>;
  private readonly _falsehoods: Set<Predicate>;
  private readonly _cycles: Set<Predicate>;
  private readonly _completeFacts: boolean;
  private readonly _completeFalsehoods: boolean;
  private readonly _predicate: Predicate | null;
  private readonly _stackOverflow: List<Predicate> | null;
  private readonly _unresolvable: boolean;

  private constructor(
    facts: Set<Predicate>,
    falsehoods: Set<Predicate>,
    cycles: Set<Predicate>,
    completeFacts: boolean,
    completeFalsehoods: boolean,
    predicate: Predicate | null = null,
    stackOverflow: List<Predicate> | null = null,
    unresolvable: boolean = false
  ) {
    this._facts = facts;
    this._falsehoods = falsehoods;
    this._cycles = cycles;
    this._completeFacts = completeFacts;
    this._completeFalsehoods = completeFalsehoods;
    this._predicate = predicate;
    this._stackOverflow = stackOverflow;
    this._unresolvable = unresolvable;
  }

  /**
   * Create a result with specified facts and completion flags.
   */
  static of(
    facts: Set<Predicate>,
    completeFacts: boolean,
    falsehoods: Set<Predicate>,
    completeFalsehoods: boolean,
    cycles: Set<Predicate>
  ): InferResult {
    return new InferResult(facts, falsehoods, cycles, completeFacts, completeFalsehoods);
  }

  /**
   * Create a result with a predicate.
   */
  static ofWithPredicate(
    predicate: Predicate,
    facts: Set<Predicate>,
    completeFacts: boolean,
    falsehoods: Set<Predicate>,
    completeFalsehoods: boolean,
    cycles: Set<Predicate>
  ): InferResult {
    return new InferResult(facts, falsehoods, cycles, completeFacts, completeFalsehoods, predicate);
  }

  /**
   * Create a true result with complete certainty.
   */
  static trueCC(predicate: Predicate): InferResult {
    return new InferResult(Set([predicate]), Set(), Set(), true, true);
  }

  /**
   * Create a false result with complete certainty.
   */
  static falseCC(predicate: Predicate): InferResult {
    return new InferResult(Set(), Set([predicate]), Set(), true, true);
  }

  /**
   * Create a facts result with facts complete, falsehoods incomplete.
   */
  static factsCI(facts: Set<Predicate>): InferResult {
    return new InferResult(facts, Set(), Set(), true, false);
  }

  /**
   * Create a facts result with facts incomplete, falsehoods complete.
   */
  static factsIC(facts: Set<Predicate>): InferResult {
    return new InferResult(facts, Set(), Set(), false, true);
  }

  /**
   * Create a facts result with complete certainty.
   */
  static factsCC(facts: Set<Predicate>): InferResult {
    return new InferResult(facts, Set(), Set(), true, true);
  }

  /**
   * Create a falsehoods result with complete certainty.
   */
  static falsehoodsCC(falsehoods: Set<Predicate>): InferResult {
    return new InferResult(Set(), falsehoods, Set(), true, true);
  }

  /**
   * Create a falsehoods result with facts incomplete.
   */
  static falsehoodsIC(falsehoods: Set<Predicate>): InferResult {
    return new InferResult(Set(), falsehoods, Set(), false, true);
  }

  /**
   * Create a falsehoods result with both incomplete.
   */
  static falsehoodsII(falsehoods: Set<Predicate>): InferResult {
    return new InferResult(Set(), falsehoods, Set(), false, false);
  }

  /**
   * Create an unknown result.
   */
  static unknown(predicate: Predicate): InferResult {
    return new InferResult(Set(), Set(), Set(), false, false, predicate);
  }

  /**
   * Create an unresolvable result.
   */
  static unresolvable(predicate: Predicate): InferResult {
    return new InferResult(Set(), Set(), Set(), false, false, predicate, null, true);
  }

  /**
   * Create a cycle result.
   */
  static cycle(facts: Set<Predicate>, falsehoods: Set<Predicate>, predicate: Predicate): InferResult {
    return new InferResult(facts, falsehoods, predicate.singleton(), false, false);
  }

  /**
   * Create an overflow result.
   */
  static overflow(overflow: List<Predicate>): InferResult {
    return new InferResult(Set(), Set(), Set(), false, false, null, overflow);
  }

  /**
   * Unresolvable singleton.
   */
  static readonly UNRESOLVABLE = new InferResult(Set(), Set(), Set(), true, true, null, null, true);

  /**
   * Get the facts.
   */
  facts(): Set<Predicate> {
    return this._facts;
  }

  /**
   * Get the falsehoods.
   */
  falsehoods(): Set<Predicate> {
    return this._falsehoods;
  }

  /**
   * Get the cycles.
   */
  cycles(): Set<Predicate> {
    return this._cycles;
  }

  /**
   * Check if facts are complete.
   */
  completeFacts(): boolean {
    return this._completeFacts;
  }

  /**
   * Check if falsehoods are complete.
   */
  completeFalsehoods(): boolean {
    return this._completeFalsehoods;
  }

  /**
   * Check if result is complete.
   */
  isComplete(): boolean {
    return this._completeFacts || this._completeFalsehoods;
  }

  /**
   * Get the predicate.
   */
  predicate(pred?: Predicate): InferResult | Predicate | null {
    if (pred !== undefined) {
      return InferResult.ofWithPredicate(pred, this._facts, this._completeFacts, this._falsehoods, this._completeFalsehoods, this._cycles);
    }
    return this._predicate;
  }

  /**
   * Check if unresolvable.
   */
  isUnresolvable(): boolean {
    return this._unresolvable;
  }

  /**
   * Check if has cycle with a predicate.
   */
  hasCycleWith(predicate: Predicate): boolean {
    return this._cycles.contains(predicate);
  }

  /**
   * Get stack overflow.
   */
  stackOverflow(): List<Predicate> | null {
    return this._stackOverflow;
  }

  /**
   * Check if has stack overflow.
   */
  hasStackOverflow(): boolean {
    return this._stackOverflow !== null;
  }

  /**
   * Check if true with complete certainty.
   */
  isTrueCC(): boolean {
    return this._falsehoods.isEmpty() && !this._facts.isEmpty() && this._completeFalsehoods && this._completeFacts;
  }

  /**
   * Check if false with complete certainty.
   */
  isFalseCC(): boolean {
    return this._facts.isEmpty() && !this._falsehoods.isEmpty() && this._completeFacts && this._completeFalsehoods;
  }

  /**
   * Get true bindings.
   */
  trueBindings(): Set<Map<Variable, unknown>> {
    const pred = this._predicate;
    if (pred === null) return Set();
    return this._facts.map(p => {
      const binding = p.getBinding(pred);
      if (binding === null) return Map<Variable, unknown>();
      return binding.filter((val) => !(val instanceof Object && 'type' in val));
    }).toSet();
  }

  /**
   * Get false bindings.
   */
  falseBindings(): Set<Map<Variable, unknown>> {
    const pred = this._predicate;
    if (pred === null) return Set();
    return this._falsehoods.map(p => {
      const binding = p.getBinding(pred);
      if (binding === null) return Map<Variable, unknown>();
      return binding.filter((val) => !(val instanceof Object && 'type' in val));
    }).toSet();
  }

  /**
   * Cast this result to a specific predicate.
   */
  cast(to: Predicate): InferResult {
    const castFacts = this._facts.map(p => p.equals(to) ? to : to.castFrom(p)).toSet();
    const castFalsehoods = this._falsehoods.map(p => p.equals(to) ? to : to.castFrom(p)).toSet();
    return InferResult.of(castFacts, this._completeFacts, castFalsehoods, this._completeFalsehoods, this._cycles);
  }

  /**
   * Add another result.
   */
  add(other: InferResult): InferResult {
    return InferResult.of(
      this._facts.union(other._facts),
      this._completeFacts && other._completeFacts,
      this._falsehoods.union(other._falsehoods),
      this._completeFalsehoods && other._completeFalsehoods,
      this._cycles.union(other._cycles)
    );
  }

  /**
   * Flip completion flags.
   */
  flipComplete(): InferResult {
    return InferResult.of(this._facts, this._completeFalsehoods, this._falsehoods, this._completeFacts, this._cycles);
  }

  /**
   * Make complete.
   */
  complete(): InferResult {
    return InferResult.of(this._facts, true, this._falsehoods, true, this._cycles);
  }

  /**
   * Combine with another result using AND logic.
   */
  and(other: InferResult): InferResult {
    if (this.isFalseCC() || other.isFalseCC()) {
      return InferResult.of(Set(), true, this._falsehoods.union(other._falsehoods), true, Set());
    }
    return InferResult.of(
      this._facts.union(other._facts),
      this._completeFacts && other._completeFacts,
      this._falsehoods.union(other._falsehoods),
      this._completeFalsehoods && other._completeFalsehoods,
      this._cycles.union(other._cycles)
    );
  }

  /**
   * Combine with another result using OR logic.
   */
  or(other: InferResult): InferResult {
    if (this.isTrueCC()) {
      return this;
    }
    if (other.isTrueCC()) {
      return other;
    }
    return InferResult.of(
      this._facts.union(other._facts),
      this._completeFacts && other._completeFacts,
      this._falsehoods.intersect(other._falsehoods),
      this._completeFalsehoods && other._completeFalsehoods,
      this._cycles.union(other._cycles)
    );
  }

  /**
   * Negate this result.
   */
  not(): InferResult {
    return InferResult.of(
      this._falsehoods,
      this._completeFalsehoods,
      this._facts,
      this._completeFacts,
      this._cycles
    );
  }

  /**
   * Check equality.
   */
  equals(other: InferResult): boolean {
    if (this === other) return true;
    if (other === null) return false;
    return this._facts.equals(other._facts) &&
           this._completeFacts === other._completeFacts &&
           this._falsehoods.equals(other._falsehoods) &&
           this._completeFalsehoods === other._completeFalsehoods &&
           this._cycles.equals(other._cycles);
  }

  /**
   * String representation.
   */
  toString(): string {
    if (this._stackOverflow !== null) {
      return this._stackOverflow.toArray().toString();
    }
    let cycleString = '';
    if (!this._cycles.isEmpty()) {
      cycleString = '{' + this._cycles.toArray().join(',') + '}';
    }
    const trueStr = this.bindingsToString(this.trueBindings(), this._completeFacts);
    const falseStr = this.bindingsToString(this.falseBindings(), this._completeFalsehoods);
    return trueStr + falseStr + cycleString;
  }

  private bindingsToString(bindings: Set<Map<Variable, unknown>>, complete: boolean): string {
    const strs = bindings.map(m => {
      const entries: string[] = [];
      m.forEach((v, k) => {
        entries.push(k + '=' + v);
      });
      return '(' + entries.join(',') + ')';
    }).toArray().sort();
    const result = '[' + strs.join(',') + ']';
    return complete ? result : result.slice(0, -1) + (bindings.isEmpty() ? '..]' : ',..]');
  }
}
