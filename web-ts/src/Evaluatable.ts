/**
 * Evaluatable interface - for nodes that can be evaluated.
 * Ported from Java: org.modelingvalue.nelumbo.Evaluatable
 */

import type { KnowledgeBase } from './KnowledgeBase';
import type { ParserResult } from './syntax/ParserResult';

/**
 * Interface for evaluatable nodes.
 */
export interface Evaluatable {
  /**
   * Evaluate this node in the context of a knowledge base.
   */
  evaluate(knowledgeBase: KnowledgeBase, result: ParserResult): void;
}

/**
 * Check if a node is evaluatable.
 */
export function isEvaluatable(node: unknown): node is Evaluatable {
  return node !== null &&
         typeof node === 'object' &&
         'evaluate' in node &&
         typeof (node as { evaluate: unknown }).evaluate === 'function';
}
