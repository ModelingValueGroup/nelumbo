/**
 * Not - logical NOT predicate.
 * @JAVA_REF org.modelingvalue.nelumbo.logic.Not
 */

import { List } from 'immutable';
import type { AstElement } from '../AstElement';
import { Node } from '../Node';
import type { Functor } from '../patterns/Functor';
import { Predicate } from './Predicate';
import { CompoundPredicate } from './CompoundPredicate';
import { InferResult } from '../InferResult';
import type { InferContext } from '../InferContext';

// Late-bound NBoolean reference (set from NBoolean.ts)
let _NBooleanResultNot: ((which: 'TRUE' | 'FALSE') => InferResult) | null = null;

export function _setNBooleanResultFnNot(fn: (which: 'TRUE' | 'FALSE') => InferResult): void {
  _NBooleanResultNot = fn;
}

/**
 * Not - logical negation.
 * @JAVA_REF org.modelingvalue.nelumbo.logic.Not
 */
export class Not extends CompoundPredicate {
  // @JAVA_REF org.modelingvalue.nelumbo.logic.Not#FUNCTOR
  static FUNCTOR: Functor | null = null;

  constructor(functor: Functor, elements: List<AstElement>, operand: Node) {
    super(functor, elements, operand);
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.Not#of(Predicate)
  static of(predicate: Predicate): Not {
    return new Not(Not.FUNCTOR!, List<AstElement>(), predicate);
  }

  protected static fromDataNot(data: unknown[], declaration?: Node): Not {
    const not = Object.create(Not.prototype) as Not;
    (not as unknown as { _data: unknown[] })._data = data;
    (not as unknown as { _declaration: Node })._declaration = declaration ?? not;
    (not as any)._binding = null;
    (not as any)._hashCodeCached = false;
    (not as any)._hashCode = 0;
    (not as any)._nrOfUnbound = -1;
    return not;
  }

  protected override struct(data: unknown[], declaration?: Node): Not {
    return Not.fromDataNot(data, declaration ?? this.declaration());
  }

  override declaration(): Not {
    return super.declaration() as Not;
  }

  // @JAVA_REF org.modelingvalue.nelumbo.logic.Not#predicate()
  predicate(): Predicate {
    return this.predicateAt(0);
  }

  /**
   * Override infer (not resolve) matching Java's Not.infer(InferContext).
   * @JAVA_REF org.modelingvalue.nelumbo.logic.Not#infer(InferContext)
   */
  override infer(context: InferContext): InferResult {
    const predicate = this.predicate();
    const predResult = predicate.infer(context);
    if (predResult.hasStackOverflow()) {
      return predResult;
    } else if (context.reduce()) {
      if (predResult.isFalseCC()) {
        return _NBooleanResultNot!('TRUE');
      } else if (predResult.isTrueCC()) {
        return _NBooleanResultNot!('FALSE');
      } else {
        return this.set(0, predResult.predicateOf()).unknown();
      }
    } else if (!predResult.unresolvable()) {
      return predResult.flipComplete();
    } else {
      return InferResult.UNRESOLVABLE;
    }
  }

  override set(i: number, ...a: unknown[]): Not {
    return super.set(i, ...a) as Not;
  }
}
