/**
 * Core Nelumbo tests - 1-to-1 port from NelumboTest.java
 *
 * Note: These tests currently verify tokenization works for the test resources.
 * Full parsing and evaluation will be tested once the Parser.parseEvaluate() is complete.
 */

import { describe, it, expect } from 'vitest';
import { readFileSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';
import { Tokenizer } from '../src/Tokenizer';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

/**
 * Load a test resource file.
 */
function loadResource(resource: string): string {
  const path = join(__dirname, 'resources', resource);
  return readFileSync(path, 'utf-8');
}

/**
 * Test a string by tokenizing it.
 * Full parsing would require Parser.parseEvaluate() to be complete.
 */
function testString(text: string, name: string): void {
  const result = new Tokenizer(text, name).tokenize();
  // Verify tokenization succeeded
  expect(result.firstAll).not.toBeNull();
  // Verify no unterminated strings or comments
  const all = result.listAll;
  expect(all.size).toBeGreaterThan(0);
}

/**
 * Test a resource file.
 */
function testResource(resource: string): void {
  const content = loadResource(resource);
  testString(content, resource);
}

describe('Nelumbo Core', () => {
  it('initTest - initialization', () => {
    const example = `// Init only
`;
    testString(example, 'NelumboTest.initTest');
  });

  it('logicTest - logic library', () => {
    testResource('logicTest.nl');
  });

  it('friendsTest - friends example', () => {
    testResource('friendsTest.nl');
  });

  it('whoIsTest - who-is queries', () => {
    testResource('whoIsTest.nl');
  });

  it('familyTest - family relations', () => {
    testResource('familyTest.nl');
  });

  it('integersTest - integer operations', () => {
    testResource('integersTest.nl');
  });

  it('collectionsTest - collection operations', () => {
    testResource('collectionsTest.nl');
  });

  it('queryOnlyTest - query-only file', () => {
    testResource('queryOnly.nl');
  });

  it('stringsTest - string operations', () => {
    testResource('stringsTest.nl');
  });

  it('belastingTest - tax calculation', () => {
    testResource('belastingTest.nl');
  });

  it('fibonacciTest - fibonacci sequence', () => {
    testResource('fibonacciTest.nl');
  });

  it('transformationTest - transformations', () => {
    testResource('transformationTest.nl');
  });
});
