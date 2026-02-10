/**
 * Test utilities for Nelumbo TypeScript tests.
 * Ported from Java: org.modelingvalue.nelumbo.test.NelumboTestBase
 */

import { List } from 'immutable';
import type { Token } from '../src/syntax/Token';
import { Tokenizer } from '../src/syntax/Tokenizer';
import type { TokenizerResult } from '../src/syntax/Tokenizer';

/**
 * Tokenize a string and return all tokens (including whitespace).
 */
export function tokenizeAll(input: string, name: string = 'test'): List<Token> {
  const result = new Tokenizer(input, name).tokenize();
  return result.listAll;
}

/**
 * Tokenize a string and return only meaningful tokens.
 */
export function tokenize(input: string, name: string = 'test'): List<Token> {
  const result = new Tokenizer(input, name).tokenize();
  return result.list;
}

/**
 * Get a full tokenizer result.
 */
export function getTokenizerResult(input: string, name: string = 'test'): TokenizerResult {
  return new Tokenizer(input, name).tokenize();
}

/**
 * Load a test resource file.
 * Note: In browser environment, this would need to be adjusted.
 */
export async function loadResource(path: string): Promise<string> {
  // For Node.js/Vitest environment
  const fs = await import('fs');
  const fullPath = new URL(`./resources/${path}`, import.meta.url).pathname;
  return fs.readFileSync(fullPath, 'utf-8');
}

/**
 * Test a string by tokenizing it and verifying success.
 */
export function testString(text: string, name: string): void {
  const result = new Tokenizer(text, name).tokenize();
  if (result.firstAll === null) {
    throw new Error('Tokenization failed');
  }
}
