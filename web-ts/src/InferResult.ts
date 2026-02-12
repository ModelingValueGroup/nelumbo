/**
 * InferResult - three-state logic result.
 * @JAVA_REF org.modelingvalue.nelumbo.InferResult
 */

import { List, Map, Set } from 'immutable';
import { Variable } from './Variable';
import { Type } from './Type';
import type { Predicate } from './logic/Predicate';

/**
 * InferResult - represents the result of logical inference.
 * @JAVA_REF org.modelingvalue.nelumbo.InferResult
 */
export class InferResult {
  private readonly _facts: Set<Predicate>;
  private readonly _falsehoods: Set<Predicate>;
  private readonly _allFacts: List<Predicate> | null;
  private readonly _allFalsehoods: List<Predicate> | null;
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
    unresolvable: boolean = false,
    allFacts: List<Predicate> | null = null,
    allFalsehoods: List<Predicate> | null = null
  ) {
    this._facts = facts;
    this._falsehoods = falsehoods;
    this._cycles = cycles;
    this._completeFacts = completeFacts;
    this._completeFalsehoods = completeFalsehoods;
    this._predicate = predicate;
    this._stackOverflow = stackOverflow;
    this._unresolvable = unresolvable;
    this._allFacts = allFacts;
    this._allFalsehoods = allFalsehoods;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#of(Set, boolean, Set, boolean, Set)
  static of(
    facts: Set<Predicate>,
    completeFacts: boolean,
    falsehoods: Set<Predicate>,
    completeFalsehoods: boolean,
    cycles: Set<Predicate>
  ): InferResult {
    return new InferResult(facts, falsehoods, cycles, completeFacts, completeFalsehoods);
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#of(Predicate, Set, boolean, Set, boolean, Set)
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

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#of(Collection, boolean, Collection, boolean, Set)
  static ofFromCollections(
    facts: List<Predicate>,
    completeFacts: boolean,
    falsehoods: List<Predicate>,
    completeFalsehoods: boolean,
    cycles: Set<Predicate>
  ): InferResult {
    return new InferResult(
      Set<Predicate>(), Set<Predicate>(), cycles,
      completeFacts, completeFalsehoods,
      null, null, false,
      facts, falsehoods
    );
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#factsCI(Set)
  static factsCI(facts: Set<Predicate>): InferResult {
    return new InferResult(facts, Set(), Set(), true, false);
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#factsIC(Set)
  static factsIC(facts: Set<Predicate>): InferResult {
    return new InferResult(facts, Set(), Set(), false, true);
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#factsCC(Set)
  static factsCC(facts: Set<Predicate>): InferResult {
    return new InferResult(facts, Set(), Set(), true, true);
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#falsehoodsCC(Set)
  static falsehoodsCC(falsehoods: Set<Predicate>): InferResult {
    return new InferResult(Set(), falsehoods, Set(), true, true);
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#falsehoodsIC(Set)
  static falsehoodsIC(falsehoods: Set<Predicate>): InferResult {
    return new InferResult(Set(), falsehoods, Set(), false, true);
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#falsehoodCI(Predicate)
  static falsehoodCI(falsehood: Predicate): InferResult {
    return new InferResult(Set(), Set(), Set(), true, false, falsehood);
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#falsehoodsII(Set)
  static falsehoodsII(falsehoods: Set<Predicate>): InferResult {
    return new InferResult(Set(), falsehoods, Set(), false, false);
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#unknown(Predicate)
  static unknown(predicate: Predicate): InferResult {
    return new InferResult(Set(), Set(), Set(), false, false, predicate);
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#unresolvable(Predicate)
  static unresolvable(predicate: Predicate): InferResult {
    return new InferResult(Set(), Set(), Set(), false, false, predicate, null, true);
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#cycle(Set, Set, Predicate)
  static cycle(facts: Set<Predicate>, falsehoods: Set<Predicate>, predicate: Predicate): InferResult {
    return new InferResult(facts, falsehoods, predicate.singleton(), false, false);
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#overflow(List)
  static overflow(overflow: List<Predicate>): InferResult {
    return new InferResult(Set(), Set(), Set(), false, false, null, overflow);
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult.UNRESOLVABLE
  static readonly UNRESOLVABLE = new InferResult(Set(), Set(), Set(), true, true, null, null, true);

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#facts()
  facts(): Set<Predicate> {
    return this._facts;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#falsehoods()
  falsehoods(): Set<Predicate> {
    return this._falsehoods;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#allFacts()
  allFacts(): Iterable<Predicate> & { isEmpty(): boolean; equals(other: unknown): boolean; map<T>(fn: (v: Predicate) => T): { toArray(): T[] } } {
    if (this._allFacts !== null) {
      return this._allFacts as any;
    }
    return this._facts as any;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#allFalsehoods()
  allFalsehoods(): Iterable<Predicate> & { isEmpty(): boolean; equals(other: unknown): boolean; map<T>(fn: (v: Predicate) => T): { toArray(): T[] } } {
    if (this._allFalsehoods !== null) {
      return this._allFalsehoods as any;
    }
    return this._falsehoods as any;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#cycles()
  cycles(): Set<Predicate> {
    return this._cycles;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#completeFacts()
  completeFacts(): boolean {
    return this._completeFacts;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#completeFalsehoods()
  completeFalsehoods(): boolean {
    return this._completeFalsehoods;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#isComplete()
  isComplete(): boolean {
    return this._completeFacts || this._completeFalsehoods;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#predicate()
  predicateOf(): Predicate | null {
    return this._predicate;
  }

  /**
   * Get or set the predicate.
   * @JAVA_REF org.modelingvalue.nelumbo.InferResult#predicate() and InferResult#predicate(Predicate)
   */
  predicate(pred?: Predicate): InferResult | Predicate | null {
    if (pred !== undefined) {
      return InferResult.ofWithPredicate(pred, this._facts, this._completeFacts, this._falsehoods, this._completeFalsehoods, this._cycles);
    }
    return this._predicate;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#unresolvable()
  isUnresolvable(): boolean {
    return this._unresolvable;
  }

  /**
   * Java-compatible unresolvable() default method.
   * @JAVA_REF org.modelingvalue.nelumbo.InferResult#unresolvable()
   */
  unresolvable(): boolean {
    return this._unresolvable;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#hasCycleWith(Predicate)
  hasCycleWith(predicate: Predicate): boolean {
    return this._cycles.contains(predicate);
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#stackOverflow()
  stackOverflow(): List<Predicate> | null {
    return this._stackOverflow;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#hasStackOverflow()
  hasStackOverflow(): boolean {
    return this._stackOverflow !== null;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#isTrueCC()
  isTrueCC(): boolean {
    return this.allFalsehoods().isEmpty() && !this.allFacts().isEmpty() && this._completeFalsehoods && this._completeFacts;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#isFalseCC()
  isFalseCC(): boolean {
    return this.allFacts().isEmpty() && !this.allFalsehoods().isEmpty() && this._completeFacts && this._completeFalsehoods;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#trueBindings()
  trueBindings(): Set<Map<Variable, unknown>> {
    const pred = this._predicate;
    if (pred === null) return Set();
    const allF = this.allFacts();
    let result = Set<Map<Variable, unknown>>();
    for (const p of allF) {
      const binding = p.getBinding(pred);
      if (binding === null) {
        result = result.add(Map<Variable, unknown>());
      } else {
        const filtered = binding.filter((val) => !(val instanceof Variable) && !(val instanceof Type));
        result = result.add(filtered);
      }
    }
    return result;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#falseBindings()
  falseBindings(): Set<Map<Variable, unknown>> {
    const pred = this._predicate;
    if (pred === null) return Set();
    const allFH = this.allFalsehoods();
    let result = Set<Map<Variable, unknown>>();
    for (const p of allFH) {
      const binding = p.getBinding(pred);
      if (binding === null) {
        result = result.add(Map<Variable, unknown>());
      } else {
        const filtered = binding.filter((val) => !(val instanceof Variable) && !(val instanceof Type));
        result = result.add(filtered);
      }
    }
    return result;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#cast(Predicate)
  cast(to: Predicate): InferResult {
    const castFacts = this._facts.map(p => p.equals(to) ? to : to.castFrom(p)).toSet();
    const castFalsehoods = this._falsehoods.map(p => p.equals(to) ? to : to.castFrom(p)).toSet();
    return InferResult.of(castFacts, this._completeFacts, castFalsehoods, this._completeFalsehoods, this._cycles);
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#add(InferResult)
  add(other: InferResult): InferResult {
    const facts = List<Predicate>([...this.allFacts(), ...other.allFacts()]);
    const falsehoods = List<Predicate>([...this.allFalsehoods(), ...other.allFalsehoods()]);
    const completeFacts = this._completeFacts && other._completeFacts;
    const completeFalsehoods = this._completeFalsehoods && other._completeFalsehoods;
    const cycles = this._cycles.union(other._cycles);
    return InferResult.ofFromCollections(facts, completeFacts, falsehoods, completeFalsehoods, cycles);
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#flipComplete()
  flipComplete(): InferResult {
    const allF = this.allFacts();
    const allFH = this.allFalsehoods();
    if (this._allFacts !== null || this._allFalsehoods !== null) {
      return InferResult.ofFromCollections(
        List<Predicate>([...allF]),
        this._completeFalsehoods,
        List<Predicate>([...allFH]),
        this._completeFacts,
        this._cycles
      );
    }
    return InferResult.of(this._facts, this._completeFalsehoods, this._falsehoods, this._completeFacts, this._cycles);
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult#complete()
  complete(): InferResult {
    if (this._allFacts !== null || this._allFalsehoods !== null) {
      return InferResult.ofFromCollections(
        List<Predicate>([...this.allFacts()]),
        true,
        List<Predicate>([...this.allFalsehoods()]),
        true,
        this._cycles
      );
    }
    return InferResult.of(this._facts, true, this._falsehoods, true, this._cycles);
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult.InferResultImpl#equals(Object)
  equals(other: InferResult): boolean {
    if (this === other) return true;
    if (other === null || other === undefined) return false;
    const thisAllFacts = this.allFacts();
    const otherAllFacts = other.allFacts();
    const thisAllFalsehoods = this.allFalsehoods();
    const otherAllFalsehoods = other.allFalsehoods();
    return thisAllFacts.equals(otherAllFacts) &&
           this._completeFacts === other._completeFacts &&
           thisAllFalsehoods.equals(otherAllFalsehoods) &&
           this._completeFalsehoods === other._completeFalsehoods &&
           this._cycles.equals(other._cycles);
  }

  // @JAVA_REF org.modelingvalue.nelumbo.InferResult.InferResultImpl#toString()
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
