/**
 * Query - logic queries with expected results.
 * @JAVA_REF org.modelingvalue.nelumbo.Query
 */

import { List, Map, Set } from 'immutable';
import type { AstElement } from './AstElement';
import { Node } from './Node';
import { Variable } from './Variable';
import type { Functor } from './patterns/Functor';
import { Predicate } from './logic/Predicate';
import { InferResult } from './InferResult';
import { ParseException } from './syntax/ParseException';
import type { KnowledgeBase, ParseExceptionHandler } from './KnowledgeBase';
import type { Evaluatable } from './Evaluatable';
import { Optional, some, none } from './patterns/OptionalPattern';

/**
 * Query - a query against the knowledge base with optional expected results.
 */
export class Query extends Node implements Evaluatable {
  private _inferResult: InferResult | null = null;

  constructor(functor: Functor, elements: List<AstElement>, ...args: unknown[]) {
    super(functor, elements, ...Query.processArgs(args));
  }

  private static processArgs(args: unknown[]): unknown[] {
    const nodePred = Predicate.predicate(args[0] as Node);
    const predBinding = nodePred.getBinding();
    const predicate = predBinding !== null
      ? nodePred.setVariables(Predicate.literals(predBinding))
      : nodePred;
    const expected = args[1] as Optional<List<List<unknown>>>;

    if (expected.isEmpty()) {
      return [predicate];
    }

    const expectedList = expected.get()!;
    const facts = expectedList.get(0) as List<unknown>;
    const falsehoods = expectedList.get(1) as List<unknown>;

    return [
      predicate,
      Query.bindings(facts.filter((x): x is List<List<unknown>> => List.isList(x)).toList()),
      !facts.contains('..'),
      Query.bindings(falsehoods.filter((x): x is List<List<unknown>> => List.isList(x)).toList()),
      !falsehoods.contains('..')
    ];
  }

  private static bindings(listListList: List<List<List<unknown>>>): Set<Map<Variable, unknown>> {
    let set = Set<Map<Variable, unknown>>();
    for (const listList of listListList) {
      let map = Map<Variable, unknown>();
      for (const list of listList) {
        map = map.set((list.get(0) as Variable).literal(), list.get(1));
      }
      set = set.add(map);
    }
    return set;
  }

  protected static fromDataQuery(data: unknown[], declaration?: Node): Query {
    const query = Object.create(Query.prototype) as Query;
    (query as unknown as { _data: unknown[] })._data = data;
    (query as unknown as { _declaration: Node | undefined })._declaration = declaration ?? query;
    return query;
  }

  protected override struct(data: unknown[], declaration?: Node): Node {
    return Query.fromDataQuery(data, declaration ?? this.declaration());
  }

  override args(): List<unknown> {
    const args: unknown[] = [this.get(0)];
    if (this.hasExpected()) {
      let trueList = List<unknown>();
      for (const binding of this.expectedFacts()) {
        let list = List<List<unknown>>();
        for (const [key, value] of binding.entries()) {
          list = list.push(List([key, value]));
        }
        trueList = trueList.push(list);
      }
      if (!this.completeFacts()) {
        trueList = trueList.push('..');
      }
      let falseList = List<unknown>();
      for (const binding of this.expectedFalsehoods()) {
        let list = List<List<unknown>>();
        for (const [key, value] of binding.entries()) {
          list = list.push(List([key, value]));
        }
        falseList = falseList.push(list);
      }
      if (!this.completeFalsehoods()) {
        falseList = falseList.push('..');
      }
      args.push(some(List([trueList, falseList])));
    } else {
      args.push(none<List<List<unknown>>>());
    }
    return List(args);
  }

  override set(i: number, ...a: unknown[]): Node {
    return super.set(i, ...a);
  }

  /**
   * Get the predicate being queried.
   */
  predicate(): Predicate {
    return Predicate.predicate(this.get(0) as Node);
  }

  /**
   * Get expected fact bindings.
   */
  expectedFacts(): Set<Map<Variable, unknown>> {
    return this.get(1) as Set<Map<Variable, unknown>>;
  }

  /**
   * Check if expected facts are complete.
   */
  completeFacts(): boolean {
    return this.get(2) as boolean;
  }

  /**
   * Get expected falsehood bindings.
   */
  expectedFalsehoods(): Set<Map<Variable, unknown>> {
    return this.get(3) as Set<Map<Variable, unknown>>;
  }

  /**
   * Check if expected falsehoods are complete.
   */
  completeFalsehoods(): boolean {
    return this.get(4) as boolean;
  }

  /**
   * Check if this query has expected results.
   */
  hasExpected(): boolean {
    return this.length() > 1;
  }

  /**
   * Evaluate this query against a knowledge base.
   */
  evaluate(knowledgeBase: KnowledgeBase, handler: ParseExceptionHandler): void {
    const predicate = this.predicate();
    let found: InferResult;
    try {
      // @JAVA_REF org.modelingvalue.nelumbo.logic.Predicate#infer() no-arg:
      // Java does: Predicate p = setBinding(getBinding()); return p.resolve(context);
      const binding = predicate.getBinding();
      const bound = binding !== null ? predicate.setBinding(binding) as Predicate : predicate;
      const result = bound.resolve(knowledgeBase.createInferContext());
      const predicateResult = result.predicate(predicate);
      found = predicateResult as InferResult;
    } catch (e) {
      if (e instanceof Error) {
        handler.addException(ParseException.fromElements(e.message, predicate));
      }
      return;
    }
    this._inferResult = found;

    if (this.hasExpected()) {
      const trueBindings = this.expectedFacts();
      const truePredicates = trueBindings.map(b => predicate.setBinding(b)).toSet();
      const completeFacts = this.completeFacts();
      const falseBindings = this.expectedFalsehoods();
      const falsePredicates = falseBindings.map(b => predicate.setBinding(b)).toSet();
      const completeFalsehoods = this.completeFalsehoods();

      const expected = InferResult.ofWithPredicate(predicate, truePredicates, completeFacts, falsePredicates, completeFalsehoods, Set());

      if (!found.equals(expected) && found.toString() !== expected.toString()) {
        const astElements = this.astElements();
        handler.addException(
          ParseException.fromElements(
            'Expected result ' + expected + ', found ' + found,
            ...astElements.slice(2).toArray()
          )
        );
      }
    }
  }

  /**
   * Get the inference result.
   */
  inferResult(): InferResult | null {
    return this._inferResult;
  }
}
