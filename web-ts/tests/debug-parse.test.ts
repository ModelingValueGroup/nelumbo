/**
 * Debug test for multi-line parsing
 */

import { describe, it, expect } from 'vitest';
import { Tokenizer } from '../src/syntax/Tokenizer';
import { Parser } from '../src/syntax/Parser';
import { KnowledgeBase } from '../src/KnowledgeBase';

describe('Debug multi-line parse', () => {
  it('should parse two type declarations', () => {
    const code = `x :: Object
y :: x
`;
    const tokenizer = new Tokenizer(code, 'test.nl');
    const tokenizerResult = tokenizer.tokenize();

    console.log('=== TOKENS ===');
    const tokens = tokenizerResult.list.toArray();
    for (const t of tokens) {
      console.log(`  ${t.type.name}:${JSON.stringify(t.text)} @${t.line}:${t.position}`);
    }

    console.log('\n=== PARSING ===');
    const kb = new KnowledgeBase(KnowledgeBase.BASE);
    const parser = new Parser(kb, tokenizerResult);

    try {
      const result = parser.parseMultipleNonThrowing();

      console.log('\n=== EXCEPTIONS ===');
      const exceptions = result.exceptions();
      console.log('Count:', exceptions.size);
      exceptions.toArray().forEach((e, i) => {
        console.log(`  ${i}: ${e.shortMessage}`);
      });

      console.log('\n=== ROOTS ===');
      const roots = result.roots();
      console.log('Count:', roots.size);
      roots.toArray().forEach((r, i) => {
        console.log(`  ${i}: type=${r.type().name()} class=${r.constructor.name}`);
      });

      expect(exceptions.size).toBe(0);
      expect(roots.size).toBeGreaterThan(0);
    } catch (e) {
      console.log('ERROR:', e);
      throw e;
    }
  });
});
