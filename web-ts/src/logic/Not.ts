/**
 * Not - logical NOT predicate.
 * Ported from Java: org.modelingvalue.nelumbo.logic.Not
 */

import { List, Set } from 'immutable';
import type { AstElement } from '../AstElement';
import { Node } from '../Node';
import type { Functor } from '../patterns/Functor';
import { Predicate } from './Predicate';
import { InferResult } from '../InferResult';
import type { InferContext } from '../InferContext';

/**
 * Not - logical negation.
 */
export class Not extends Predicate {
  constructor(functor: Functor, elements: List<AstElement>, operand: Node) {
    super(functor, elements, operand);
  }

  protected static fromDataNot(data: unknown[], declaration?: Node): Not {
    const not = Object.create(Not.prototype) as Not;
    (not as unknown as { _data: unknown[] })._data = data;
    (not as unknown as { _declaration: Node })._declaration = declaration ?? not;
    return not;
  }

  protected override struct(data: unknown[], declaration?: Node): Not {
    return Not.fromDataNot(data, declaration ?? this.declaration());
  }

  /**
   * Get the operand predicate.
   */
  operand(): Predicate {
    return Predicate.predicate(this.get(0) as Node);
  }

  override resolve(context: InferContext): InferResult {
    const operand = this.operand();

    // Resolve operand in negated context
    const result = operand.resolve(context.negate());

    if (result.hasStackOverflow()) {
      return result;
    }

    // Flip the result
    const flipped = result.not();

    // In reduce mode, return complete flipped result
    if (context.reduce()) {
      return this.createResult(flipped, true);
    }

    // Otherwise flip completeness flags
    return this.createResult(flipped.flipComplete(), false);
  }

  private createResult(flipped: InferResult, complete: boolean): InferResult {
    if (flipped.isTrueCC() || (complete && !flipped.facts().isEmpty())) {
      return InferResult.factsCC(Set([this]));
    }
    if (flipped.isFalseCC() || (complete && !flipped.falsehoods().isEmpty())) {
      return InferResult.falsehoodsCC(Set([this]));
    }

    return InferResult.of(
      flipped.facts().isEmpty() ? Set() : Set([this]),
      flipped.completeFacts(),
      flipped.falsehoods().isEmpty() ? Set() : Set([this]),
      flipped.completeFalsehoods(),
      flipped.cycles()
    );
  }

  override toString(): string {
    return `!${this.operand()}`;
  }
}
