/**
 * Core Nelumbo tests - 1-to-1 port from NelumboTest.java + NelumboTestBase.java
 *
 * Java: @RepeatedTest(10) - repeated to test with RANDOM_NELUMBO=true
 * TS: No randomization, single run is equivalent.
 */

import { describe, it } from 'vitest';
import { readFileSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';
import { Tokenizer } from '../src/syntax/Tokenizer';
import { Parser } from '../src/syntax/Parser';
import { KnowledgeBase } from '../src/KnowledgeBase';
import { ParseException } from '../src/syntax/ParseException';
import { setExternalResolver } from '../src/ModuleContent';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

/** Java resources base path */
const JAVA_RESOURCES = join(__dirname, '../../src/main/resources/org/modelingvalue/nelumbo');

/**
 * Load a test resource file from the Java resource tree.
 */
function loadResource(resource: string): string {
  const path = join(JAVA_RESOURCES, 'examples', resource);
  return readFileSync(path, 'utf-8');
}

/**
 * Equivalent to Java NelumboTestBase.testString()
 */
function testString(text: string, name: string): void {
  const kb = new KnowledgeBase(KnowledgeBase.BASE);
  kb.run(() => {
    try {
      new Parser(KnowledgeBase.CURRENT, new Tokenizer(text, name).tokenize()).parseEvaluate();
    } catch (e) {
      if (e instanceof ParseException) {
        console.error(e.message);
      }
      throw e;
    }
  });
}

/**
 * Equivalent to Java NelumboTestBase.testResource()
 */
function testResource(resource: string): void {
  const kb = new KnowledgeBase(KnowledgeBase.BASE);
  kb.run(() => {
    try {
      const content = loadResource(resource);
      new Parser(KnowledgeBase.CURRENT, new Tokenizer(content, resource).tokenize()).parseEvaluate();
    } catch (e) {
      if (e instanceof ParseException) {
        console.error(e.message);
      }
      throw e;
    }
  });
}

// External resolver for test resources (e.g. whoIsTest.nl imports friendsTest)
setExternalResolver((name: string) => {
  const prefix = 'org.modelingvalue.nelumbo.examples.';
  if (name.startsWith(prefix)) {
    const resource = name.substring(prefix.length) + '.nl';
    try { return loadResource(resource); } catch { return null; }
  }
  return null;
});

describe('Nelumbo', () => {
  it('initTest', () => {
    testString('// Init only\n', 'NelumboTest.initTest');
  });

  it('logicTest', () => {
    testResource('logicTest.nl');
  });

  it('friendsTest', () => {
    (globalThis as any).__DEBUG_RULES__ = true;
    try { testResource('friendsTest.nl'); } finally { (globalThis as any).__DEBUG_RULES__ = false; }
  });

  it('whoIsTest', () => {
    testResource('whoIsTest.nl');
  });

  it('familyTest', () => {
    testResource('familyTest.nl');
  });

  it('integersTest', () => {
    testResource('integersTest.nl');
  });

  it('collectionsTest', () => {
    testResource('collectionsTest.nl');
  });

  it('queryOnlyTest', () => {
    testResource('queryOnly.nl');
  });

  it('stringsTest', () => {
    testResource('stringsTest.nl');
  });

  it('belastingTest', () => {
    testResource('belastingTest.nl');
  });

  it('fibonacciTest', () => {
    testResource('fibonacciTest.nl');
  });

  it('transformationTest', () => {
    testResource('transformationTest.nl');
  });
});
