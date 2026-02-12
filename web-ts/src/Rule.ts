/**
 * Rule - bidirectional implication with biimply.
 * @JAVA_REF org.modelingvalue.nelumbo.Rule
 */

import { List, Set } from 'immutable';
import type { AstElement } from './AstElement';
import { Node } from './Node';
import { Variable } from './Variable';
import type { Functor } from './patterns/Functor';
import { Predicate } from './logic/Predicate';
import { InferResult } from './InferResult';
import type { InferContext } from './InferContext';
import type { KnowledgeBase, ParseExceptionHandler } from './KnowledgeBase';
import type { Evaluatable } from './Evaluatable';

/**
 * InconsistencyException - thrown when rule evaluation finds a contradiction.
 */
export class InconsistencyException extends Error {
  readonly ruleResult: InferResult;
  readonly previousResult: InferResult;

  constructor(ruleResult: InferResult, previousResult: InferResult) {
    super(`Inconsistency found: rule result ${ruleResult} conflicts with ${previousResult}`);
    this.name = 'InconsistencyException';
    this.ruleResult = ruleResult;
    this.previousResult = previousResult;
  }
}

/**
 * Rule - a bidirectional implication rule.
 */
export class Rule extends Node implements Evaluatable {
  constructor(functor: Functor, elements: List<AstElement>, consequence: Predicate, condition: Predicate) {
    super(functor, elements, consequence, condition);
  }

  protected static fromDataRule(data: unknown[], declaration?: Node): Rule {
    const rule = Object.create(Rule.prototype) as Rule;
    (rule as unknown as { _data: unknown[] })._data = data;
    (rule as unknown as { _declaration: Node })._declaration = declaration ?? rule;
    return rule;
  }

  protected struct(data: unknown[], declaration?: Node): Rule {
    return Rule.fromDataRule(data, declaration ?? this.declaration());
  }

  override args(): List<unknown> {
    return super.args().push(List());
  }

  /**
   * Get the consequence functor.
   */
  consequenceFunctor(): Functor | null {
    return this.consequence().functor();
  }

  /**
   * Get the consequence predicate.
   */
  consequence(): Predicate {
    return Predicate.predicate(this.get(0) as Node);
  }

  /**
   * Get the condition predicate.
   */
  condition(): Predicate {
    return Predicate.predicate(this.get(1) as Node);
  }

  /**
   * Bidirectional implication - core rule inference logic.
   */
  biimply(predicate: Predicate, context: InferContext, result: InferResult): InferResult {
    const consequence = this.consequence();
    const bindingOrNull = predicate.getBinding(consequence);
    if ((globalThis as any).__DEBUG_RULES__ && String(predicate).includes('friend')) {
      const cond = this.condition();
      const condType = cond.type();
      // Explore the Or structure to find friends nodes
      const findFriendsTypes = (node: any, depth: number = 0): string => {
        const prefix = '  '.repeat(depth);
        let result = `${prefix}${node.constructor.name}: type=${node.type()}, str=${String(node).substring(0, 60)}\n`;
        for (let i = 0; i < node.length(); i++) {
          const child = node.get(i);
          if (child instanceof Node && !(child instanceof Variable)) {
            result += findFriendsTypes(child, depth + 1);
          }
        }
        return result;
      };
      console.log(`DEBUG biimply UNBOUND RULE TYPES:\ncondition type=${condType}\n${findFriendsTypes(cond)}`);
    }
    if (bindingOrNull === null) {
      return result;
    }

    const condition = this.condition();
    const binding = this.getBinding()!.merge(bindingOrNull);
    const boundConsequence = consequence.setBinding(binding) as Predicate;
    const boundCondition = condition.setBinding(binding) as Predicate;
    if ((globalThis as any).__DEBUG_RULES__) {
      console.log(`DEBUG biimply: binding=${binding} boundCond=${boundCondition} boundCons=${boundConsequence}`);
    }

    if (context.trace()) {
      console.log(context.prefix() + boundConsequence + ' <=> ' + boundCondition);
    }

    const condResult = boundCondition.resolve(context);
    if ((globalThis as any).__DEBUG_RULES__) {
      console.log(`DEBUG biimply: condResult=${condResult} facts=${condResult.facts()} falsehoods=${condResult.falsehoods()}`);
    }

    // @JAVA_REF org.modelingvalue.nelumbo.Rule#biimply - stack overflow check
    if (condResult.hasStackOverflow()) {
      return condResult;
    }

    let facts = Set<Predicate>();
    let falsehoods = Set<Predicate>();
    let completeFacts = true;
    let completeFalsehoods = true;

    // @JAVA_REF org.modelingvalue.nelumbo.Rule#biimply - process facts/falsehoods
    // Java: condFact.getBinding() always returns non-null Map for inference results
    for (const condFact of condResult.facts()) {
      const fact = predicate.castFrom(boundConsequence.setBinding(condFact.getBinding()!) as Predicate);
      if (fact.isFullyBound()) {
        facts = facts.add(fact);
      } else {
        completeFacts = false;
      }
    }

    for (const condFalsehood of condResult.falsehoods()) {
      const falsehood = predicate.castFrom(boundConsequence.setBinding(condFalsehood.getBinding()!) as Predicate);
      if (falsehood.isFullyBound()) {
        falsehoods = falsehoods.add(falsehood);
      } else {
        completeFalsehoods = false;
      }
    }

    if ((facts.isEmpty() && falsehoods.isEmpty()) || !predicate.isFullyBound()) {
      if (!condResult.completeFacts()) {
        completeFacts = false;
      }
      if (!condResult.completeFalsehoods()) {
        completeFalsehoods = false;
      }
    }

    const ruleResult = InferResult.of(facts, completeFacts, falsehoods, completeFalsehoods, condResult.cycles());

    if (context.trace()) {
      console.log(context.prefix() + predicate + ' ' + ruleResult);
    }

    // Check for inconsistencies
    for (const fact of result.facts()) {
      if (falsehoods.contains(fact) || (completeFacts && this.biimply(fact, context, fact.unknown()).isFalseCC())) {
        throw new InconsistencyException(ruleResult, result);
      }
      facts = facts.add(fact);
    }

    for (const falsehood of result.falsehoods()) {
      if (facts.contains(falsehood) || (completeFalsehoods && this.biimply(falsehood, context, falsehood.unknown()).isTrueCC())) {
        throw new InconsistencyException(ruleResult, result);
      }
      falsehoods = falsehoods.add(falsehood);
    }

    const finalCompleteFacts = completeFacts || result.completeFacts();
    const finalCompleteFalsehoods = completeFalsehoods || result.completeFalsehoods();

    return InferResult.of(facts, finalCompleteFacts, falsehoods, finalCompleteFalsehoods, result.cycles().union(ruleResult.cycles()));
  }

  override set(i: number, ...a: unknown[]): Rule {
    return super.set(i, ...a) as Rule;
  }

  /**
   * Evaluate this rule in a knowledge base.
   */
  evaluate(knowledgeBase: KnowledgeBase, _handler: ParseExceptionHandler): void {
    knowledgeBase.addRule(this);
  }

  /**
   * Initialize this rule in a knowledge base.
   */
  initInKb(knowledgeBase: KnowledgeBase): Rule {
    this.evaluate(knowledgeBase, knowledgeBase);
    return this;
  }

  override toString(): string {
    return this.consequence() + ' <=> ' + this.condition();
  }
}
