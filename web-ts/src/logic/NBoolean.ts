/**
 * NBoolean - static TRUE, FALSE, UNKNOWN boolean instances.
 * @JAVA_REF org.modelingvalue.nelumbo.logic.NBoolean
 */

import { List, Set } from 'immutable';
import type { AstElement } from '../AstElement';
import { Type } from '../Type';
import type { Functor } from '../patterns/Functor';
import { Predicate } from './Predicate';
import { InferResult } from '../InferResult';
import type { InferContext } from '../InferContext';

/**
 * NBoolean - represents boolean constants TRUE, FALSE, and UNKNOWN.
 */
export class NBoolean extends Predicate {
  private readonly _value: boolean | null;
  private _cachedResult: InferResult | null = null;

  private constructor(value: boolean | null) {
    super(null as unknown as Functor, List<AstElement>());
    this._value = value;
  }

  /**
   * Get the boolean value.
   */
  value(): boolean | null {
    return this._value;
  }

  /**
   * Check if this is true.
   */
  isTrue(): boolean {
    return this._value === true;
  }

  /**
   * Check if this is false.
   */
  override isFalse(): boolean {
    return this._value === false;
  }

  /**
   * Check if this is unknown.
   */
  isUnknown(): boolean {
    return this._value === null;
  }

  override type(): Type {
    return Type.BOOLEAN;
  }

  /**
   * Get cached result for this boolean.
   */
  result(): InferResult {
    if (this._cachedResult === null) {
      if (this._value === true) {
        this._cachedResult = InferResult.factsCC(Set([this]));
      } else if (this._value === false) {
        this._cachedResult = InferResult.falsehoodsCC(Set([this]));
      } else {
        this._cachedResult = InferResult.unknown(this);
      }
    }
    return this._cachedResult;
  }

  override infer(_context: InferContext): InferResult {
    return this.result();
  }

  override resolve(_context: InferContext): InferResult {
    return this.result();
  }

  override toString(): string {
    if (this._value === true) return 'true';
    if (this._value === false) return 'false';
    return 'unknown';
  }

  override equals(other: unknown): boolean {
    if (this === other) return true;
    if (!(other instanceof NBoolean)) return false;
    return this._value === other._value;
  }

  override hashCode(): number {
    if (this._value === true) return 1;
    if (this._value === false) return 0;
    return -1;
  }

  /**
   * Static TRUE instance.
   */
  static readonly TRUE = new NBoolean(true);

  /**
   * Static FALSE instance.
   */
  static readonly FALSE = new NBoolean(false);

  /**
   * Static UNKNOWN instance.
   */
  static readonly UNKNOWN = new NBoolean(null);

  /**
   * Get NBoolean from boolean value.
   */
  static of(value: boolean | null): NBoolean {
    if (value === true) return NBoolean.TRUE;
    if (value === false) return NBoolean.FALSE;
    return NBoolean.UNKNOWN;
  }

  /**
   * Get NBoolean singleton from functor name.
   * Maps "true"→TRUE, "false"→FALSE, "unknown"→UNKNOWN.
   */
  static fromFunctor(functor: Functor, _elements: List<AstElement>): NBoolean {
    const name = functor.name();
    if (name === 'true') return NBoolean.TRUE;
    if (name === 'false') return NBoolean.FALSE;
    return NBoolean.UNKNOWN;
  }
}
