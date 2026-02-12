/**
 * NBoolean - boolean constants TRUE, FALSE, UNKNOWN with proper parser support.
 * @JAVA_REF org.modelingvalue.nelumbo.logic.NBoolean
 *
 * In Java, NBoolean instances are created by the parser through the @NelumboConstructor
 * annotation. The first instances created for true/false/unknown become the singletons.
 * This TS version follows the same pattern: the constructor accepts functor + elements
 * from the parser, stores a boolean value as args[0], and lazily assigns singletons.
 */

import { List, Set } from 'immutable';
import type { AstElement } from '../AstElement';
import { Type } from '../Type';
import { Variable } from '../Variable';
import { Node } from '../Node';
import type { Functor } from '../patterns/Functor';
import { Predicate } from './Predicate';
import { InferResult } from '../InferResult';
import type { InferContext } from '../InferContext';
import { TokenType } from '../syntax/TokenType';

/**
 * NBoolean - represents boolean constants TRUE, FALSE, and UNKNOWN.
 * Also handles boolean variables (NBoolean wrapping a Variable).
 */
export class NBoolean extends Predicate {
  private _cachedResult: InferResult | null = null;

  // @JAVA_REF org.modelingvalue.nelumbo.logic.NBoolean(Functor, List, Object[])
  constructor(functor: Functor, elements: List<AstElement>, ...args: unknown[]) {
    // If args[0] is already a boolean/null/Variable, use it directly (from struct/internal)
    // Otherwise parse the functor name (from parser)
    if (args.length > 0 && (typeof args[0] === 'boolean' || args[0] === null || args[0] instanceof Variable)) {
      super(functor, elements, args[0]);
    } else {
      super(functor, elements, NBoolean.parseName(functor?.name()));
    }

    // @JAVA_REF NBoolean constructor singleton assignment
    if (NBoolean._TRUE === null && this.isTrue() && this.variable() === null) {
      NBoolean._TRUE = this;
    } else if (NBoolean._FALSE === null && this.isFalse() && this.variable() === null) {
      NBoolean._FALSE = this;
    } else if (NBoolean._UNKNOWN === null && this.isUnknown()) {
      NBoolean._UNKNOWN = this;
    }
  }

  /**
   * Create NBoolean from a Variable (like Java's NBoolean(Variable) constructor).
   * @JAVA_REF org.modelingvalue.nelumbo.logic.NBoolean(Variable)
   */
  static fromVariable(v: Variable): NBoolean {
    return new NBoolean(
      NBoolean._UNKNOWN?.functor() ?? null as unknown as Functor,
      List([v as AstElement]),
      v
    );
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.NBoolean#parse(String)
  private static parseName(name: string | undefined): boolean | null {
    if (!name) return null;
    const lower = name.toLowerCase();
    if (lower === 'true') return true;
    if (lower === 'false') return false;
    return null;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.NBoolean#getBoolean()
  private getBoolean(): boolean | null {
    const val = this.get(0);
    return typeof val === 'boolean' ? val : null;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.NBoolean#variable()
  override variable(): Variable | null {
    const obj = this.get(0);
    if (obj instanceof Variable) {
      return obj;
    }
    const declObj = this.declaration().get(0);
    if (declObj instanceof Variable) {
      return declObj;
    }
    return null;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.NBoolean#args()
  override args(): List<unknown> {
    return List();
  }

  /**
   * Get the boolean value.
   */
  value(): boolean | null {
    return this.getBoolean();
  }

  /**
   * Check if this is true.
   */
  isTrue(): boolean {
    const b = this.getBoolean();
    return b !== null && b;
  }

  /**
   * Check if this is false.
   */
  override isFalse(): boolean {
    const b = this.getBoolean();
    return b !== null && !b;
  }

  /**
   * Check if this is unknown.
   */
  isUnknown(): boolean {
    const b = this.getBoolean();
    return b === null && this.variable() === null;
  }

  override type(): Type {
    return Type.BOOLEAN;
  }

  /**
   * Get cached result for this boolean.
   */
  result(): InferResult {
    return this.infer(null as unknown as InferContext);
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.NBoolean#struct(Object[], Node)
  protected static fromDataNBoolean(data: unknown[], declaration?: Node): NBoolean {
    const nb = Object.create(NBoolean.prototype) as NBoolean;
    (nb as any)._data = data;
    (nb as any)._declaration = declaration ?? nb;
    (nb as any)._binding = null;
    (nb as any)._hashCodeCached = false;
    (nb as any)._hashCode = 0;
    (nb as any)._nrOfUnbound = -1;
    (nb as any)._cachedResult = null;
    return nb;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.NBoolean#struct(Object[], Node)
  protected override struct(data: unknown[], declaration?: Node): NBoolean {
    // Match Java: if data[START] is NBoolean, extract boolean value and use as declaration
    if (data[Node.START] instanceof NBoolean) {
      const b = data[Node.START] as NBoolean;
      data = [...data];
      data[Node.START] = b.getBoolean();
      declaration = b;
    }
    return NBoolean.fromDataNBoolean(data, declaration ?? this.declaration());
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.NBoolean#infer(InferContext)
  override infer(context: InferContext): InferResult {
    if (context !== null && context !== undefined && context.shallow()) {
      return this.unresolvable();
    }
    if (this._cachedResult === null) {
      const v = this.variable();
      if (v !== null) {
        // Boolean variable: can be true or false
        this._cachedResult = InferResult.ofWithPredicate(
          this,
          Set([this.set(0, true) as Predicate]),
          true,
          Set([this.set(0, false) as Predicate]),
          true,
          Set()
        );
      } else {
        this._cachedResult = this.isTrue() ? this.factCC()
          : this.isFalse() ? this.falsehoodCC()
          : this.unknown();
      }
    }
    return this._cachedResult;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.NBoolean#resolve(InferContext)
  override resolve(context: InferContext): InferResult {
    return this.infer(context);
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.NBoolean#set(int, Object...)
  override set(i: number, ...a: unknown[]): NBoolean {
    return super.set(i, ...a) as NBoolean;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.NBoolean#toString(TokenType[])
  override toString(previous?: TokenType[]): string {
    const prev = previous ?? [TokenType.BEGINOFFILE];
    const v = this.variable();
    let str: string;
    if (v !== null) {
      str = v.name();
    } else if (this.isUnknown()) {
      str = 'unknown';
    } else {
      str = String(this.get(0));
    }
    if (prev[0] === TokenType.NAME || prev[0] === TokenType.NUMBER || prev[0] === TokenType.DECIMAL) {
      prev[0] = TokenType.NAME;
      return ' ' + str;
    }
    prev[0] = TokenType.NAME;
    return str;
  }

  override equals(other: unknown): boolean {
    if (this === other) return true;
    if (!(other instanceof NBoolean)) return false;
    return super.equals(other);
  }

  override hashCode(): number {
    return super.hashCode();
  }

  // Mutable singleton references (set from first parser-created instances, like Java)
  private static _TRUE: NBoolean | null = null;
  private static _FALSE: NBoolean | null = null;
  private static _UNKNOWN: NBoolean | null = null;

  /**
   * Static TRUE instance.
   */
  static get TRUE(): NBoolean {
    return NBoolean._TRUE!;
  }

  /**
   * Static FALSE instance.
   */
  static get FALSE(): NBoolean {
    return NBoolean._FALSE!;
  }

  /**
   * Static UNKNOWN instance.
   */
  static get UNKNOWN(): NBoolean {
    return NBoolean._UNKNOWN!;
  }

  /**
   * Get NBoolean from boolean value.
   */
  static of(value: boolean | null): NBoolean {
    if (value === true) return NBoolean.TRUE;
    if (value === false) return NBoolean.FALSE;
    return NBoolean.UNKNOWN;
  }
}

// Register the factory hook on Predicate to avoid circular import
Predicate._booleanFromVariable = (v: Variable) => NBoolean.fromVariable(v);

// Register late-bound NBoolean references for CompoundPredicate, BinaryPredicate, Not
// @JAVA_REF These avoid circular imports: NBoolean.TRUE/FALSE are used in CompoundPredicate.resolve(),
// BinaryPredicate.infer(), and Not.infer() but those classes can't import NBoolean directly.
import { _setNBooleanRefs } from './CompoundPredicate';
import { _setNBooleanResultFn } from './BinaryPredicate';
import { _setNBooleanResultFnNot } from './Not';

_setNBooleanRefs(() => NBoolean.TRUE, () => NBoolean.FALSE);
_setNBooleanResultFn((which: 'TRUE' | 'FALSE' | 'UNKNOWN') => {
  if (which === 'TRUE') return NBoolean.TRUE.result();
  if (which === 'FALSE') return NBoolean.FALSE.result();
  return NBoolean.UNKNOWN.result();
});
_setNBooleanResultFnNot((which: 'TRUE' | 'FALSE') => {
  if (which === 'TRUE') return NBoolean.TRUE.result();
  return NBoolean.FALSE.result();
});
