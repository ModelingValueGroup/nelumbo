import { describe, it } from 'vitest';
import { readFileSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';
import { Tokenizer } from '../src/syntax/Tokenizer';
import { Parser } from '../src/syntax/Parser';
import { KnowledgeBase } from '../src/KnowledgeBase';
import { Predicate } from '../src/logic/Predicate';
import { setExternalResolver } from '../src/ModuleContent';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const JAVA_RESOURCES = join(__dirname, '../../src/main/resources/org/modelingvalue/nelumbo');

setExternalResolver((name: string) => {
  const prefix = 'org.modelingvalue.nelumbo.examples.';
  if (name.startsWith(prefix)) {
    const resource = name.substring(prefix.length) + '.nl';
    try { return readFileSync(join(JAVA_RESOURCES, 'examples', resource), 'utf-8'); } catch { return null; }
  }
  return null;
});

describe('Debug Integers', () => {
  it('0=a then 1=-a with resolve limit', () => {
    const kb = new KnowledgeBase(KnowledgeBase.BASE);

    // Patch resolve to limit iterations
    let resolveCount = 0;
    const maxResolves = 500;
    const origResolve = Predicate.prototype.resolve;
    Predicate.prototype.resolve = function(this: Predicate, ...args: any[]) {
      resolveCount++;
      if (resolveCount <= 30) {
        console.log(`  resolve[${resolveCount}]: ${this} functor=${this.functor()} class=${this.constructor.name}`);
      } else if (resolveCount % 100 === 0) {
        console.log(`  resolve[${resolveCount}]: ${this}`);
      }
      if (resolveCount >= maxResolves) {
        throw new Error(`Too many resolve calls (${maxResolves}). Last: ${this}`);
      }
      return origResolve.apply(this, args as any);
    };

    try {
      kb.run(() => {
        new Parser(KnowledgeBase.CURRENT, new Tokenizer(`import nelumbo.integers
Integer a
0=a ? [(a=0)][..]
1=-a ? [(a=-1)][..]
`, 'debug').tokenize()).parseEvaluate();
      });
      console.log(`OK, total resolves: ${resolveCount}`);
    } catch (e: any) {
      console.log(`FAILED after ${resolveCount} resolves: ${e.message}`);
    } finally {
      Predicate.prototype.resolve = origResolve;
    }
  });
});
