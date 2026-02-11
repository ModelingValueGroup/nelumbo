/**
 * Fact - asserted facts.
 * @JAVA_REF org.modelingvalue.nelumbo.Fact
 */

import { List } from 'immutable';
import type { AstElement } from './AstElement';
import { Node } from './Node';
import type { Functor } from './patterns/Functor';
import { Predicate } from './logic/Predicate';
import { ParseException } from './syntax/ParseException';
import type { KnowledgeBase, ParseExceptionHandler } from './KnowledgeBase';
import type { Evaluatable } from './Evaluatable';

/**
 * Fact - an asserted fact in the knowledge base.
 */
export class Fact extends Node implements Evaluatable {
  constructor(functor: Functor, elements: List<AstElement>, ...args: unknown[]) {
    super(functor, elements, ...args);
  }

  protected static fromDataFact(data: unknown[], declaration?: Node): Fact {
    const fact = Object.create(Fact.prototype) as Fact;
    (fact as unknown as { _data: unknown[] })._data = data;
    (fact as unknown as { _declaration: Node | undefined })._declaration = declaration ?? fact;
    return fact;
  }

  protected override struct(data: unknown[], declaration?: Node): Node {
    return Fact.fromDataFact(data, declaration ?? this.declaration());
  }

  override set(i: number, ...a: unknown[]): Node {
    return super.set(i, ...a);
  }

  /**
   * Get the predicate this fact asserts.
   */
  predicate(): Predicate {
    return Predicate.predicate(this.get(0) as Node);
  }

  /**
   * Evaluate this fact in a knowledge base.
   */
  evaluate(knowledgeBase: KnowledgeBase, handler: ParseExceptionHandler): void {
    const predicate = this.predicate();
    if (!predicate.isFact()) {
      handler.addException(ParseException.fromElements('The type of ' + predicate + ' is not FactType.', predicate));
      return;
    }
    if (!predicate.isFullyBound()) {
      handler.addException(ParseException.fromElements('Fact ' + predicate + ' has variables.', predicate));
      return;
    }
    knowledgeBase.addFact(predicate);
  }

  /**
   * Initialize this fact in a knowledge base.
   */
  // @JAVA_REF Fact.init(KnowledgeBase)
  initInKb(knowledgeBase: KnowledgeBase): Fact {
    let predicate = this.predicate();
    const nodeFunctor = predicate.functor();
    const literalFunctor = knowledgeBase.literal(nodeFunctor!);
    if (literalFunctor !== null) {
      predicate = predicate.setFunctor(literalFunctor);
    }
    if (predicate.isFact() && predicate.isFullyBound()) {
      knowledgeBase.addFact(predicate);
    }
    return this;
  }
}
