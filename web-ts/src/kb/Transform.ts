/**
 * Transform - metaprogramming/rewriting.
 * Ported from Java: org.modelingvalue.nelumbo.Transform
 */

import { List, Map, Set } from 'immutable';
import type { AstElement } from '../core/AstElement';
import { Type } from '../core/Type';
import { Node } from '../core/Node';
import { Pattern } from '../patterns/Pattern';
import { Functor } from '../patterns/Functor';
import { TokenTextPattern } from '../patterns/TokenTextPattern';
import type { KnowledgeBase } from './KnowledgeBase';

/**
 * NList - a list node for transform targets.
 */
export class NList extends Node {
  constructor(functor: Functor | null, elements: List<AstElement>, ...args: unknown[]) {
    super(functor!, elements, ...args);
  }

  elements(): List<Node> {
    const result: Node[] = [];
    for (let i = 0; i < this.length(); i++) {
      const elem = this.get(i);
      if (elem instanceof Node) {
        result.push(elem);
      }
    }
    return List(result);
  }
}

/**
 * Transform - a rewrite/transformation rule.
 */
export class Transform extends Node {
  constructor(functor: Functor, elements: List<AstElement>, ...args: unknown[]) {
    super(functor, elements, ...args);
  }

  protected static fromDataTransform(data: unknown[], declaration?: Node): Transform {
    const transform = Object.create(Transform.prototype) as Transform;
    (transform as unknown as { _data: unknown[] })._data = data;
    (transform as unknown as { _declaration: Node | undefined })._declaration = declaration ?? transform;
    return transform;
  }

  protected override struct(data: unknown[], declaration?: Node): Node {
    return Transform.fromDataTransform(data, declaration ?? this.declaration());
  }

  override set(i: number, ...a: unknown[]): Node {
    return super.set(i, ...a);
  }

  /**
   * Get the source pattern.
   */
  source(): Node {
    return this.get(0) as Node;
  }

  /**
   * Get the target nodes.
   */
  targets(): List<Node> {
    return this.get(1) as List<Node>;
  }

  /**
   * Get flattened list of targets.
   */
  targetsFlattened(): List<Node> {
    let result = List<Node>();
    for (const e of this.targets()) {
      if (e instanceof NList) {
        result = result.concat(e.elements());
      } else {
        result = result.push(e);
      }
    }
    return result;
  }

  /**
   * Get literal functors from targets.
   */
  literals(): Set<Functor> {
    let literals = Set<Functor>();
    for (const target of this.targetsFlattened()) {
      if (target instanceof Functor && target.pattern() instanceof TokenTextPattern) {
        literals = literals.add(target);
      }
    }
    return literals;
  }

  /**
   * Initialize this transform in a knowledge base.
   */
  initInKb(knowledgeBase: KnowledgeBase): Transform {
    knowledgeBase.addTransform(this);
    return this;
  }

  /**
   * Rewrite nodes matching the source pattern.
   */
  rewrite(start: Node, node: Node, knowledgeBase: KnowledgeBase): void {
    const binding = node.getBinding(start);
    if (binding === null) {
      return;
    }

    let functors = Map<Functor, Functor>();
    for (const target of this.targetsFlattened()) {
      if (target instanceof Functor && !Type.VARIABLE.isAssignableFrom(target.resultType()) && !target.pattern().equals(start)) {
        const rewrite = target.setBinding(binding).resetDeclaration() as Functor;
        for (const [key, value] of functors.entries()) {
          if (target.equals(knowledgeBase.literal(key))) {
            knowledgeBase.addLiteral(value, rewrite);
            break;
          }
        }
        functors = functors.set(target, rewrite);
        rewrite.initInKb(knowledgeBase);
      }
    }

    if (start instanceof Pattern) {
      return;
    }

    const fm = functors;
    for (const target of this.targetsFlattened()) {
      if (!(target instanceof Functor)) {
        let rewrite = target.replace(n => {
          const typeOrFunctor = n.typeOrFunctor();
          if (typeOrFunctor instanceof Functor) {
            const r = fm.get(typeOrFunctor);
            if (r !== undefined) {
              return n.setFunctor(r);
            }
          }
          return n;
        });
        rewrite = rewrite.setBinding(binding).setAstElements(node.astElements()).resetDeclaration();
        if (rewrite instanceof Functor) {
          rewrite.initInKb(knowledgeBase);
        }
      }
    }
  }
}
