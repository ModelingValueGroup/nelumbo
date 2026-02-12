import { KnowledgeBase } from '../src/KnowledgeBase';
import { Tokenizer } from '../src/syntax/Tokenizer';
import { Parser } from '../src/syntax/Parser';
import { InferContext } from '../src/InferContext';
import { InferResult } from '../src/InferResult';
import { Predicate } from '../src/logic/Predicate';

// Trace the fixpoint loop
const origFixpoint = (Predicate.prototype as any).fixpoint;
let fpCount = 0;
(Predicate.prototype as any).fixpoint = function(this: Predicate, context: InferContext) {
  fpCount++;
  console.log(`fixpoint[${fpCount}]: ${this.toString()} stack=${context.stack().size}`);
  if (fpCount > 30) {
    throw new Error(`Too many fixpoint calls`);
  }
  const result = origFixpoint.call(this, context);
  console.log(`fixpoint[${fpCount}]:  => ${result.toString()} hasCycle=${result.hasCycleWith(this)}`);
  return result;
};

// Also trace inferRules
const origInferRules = (Predicate.prototype as any).inferRules;
let irCount = 0;
(Predicate.prototype as any).inferRules = function(this: Predicate, context: InferContext) {
  irCount++;
  console.log(`  inferRules[${irCount}]: ${this.toString()}`);
  if (irCount > 30) {
    throw new Error(`Too many inferRules calls`);
  }
  const result = origInferRules.call(this, context);
  console.log(`  inferRules[${irCount}]:  => ${result.toString()} cycles=[${[...result.cycles()].map((c: any) => c.toString()).join(',')}]`);
  return result;
};

const text = `
    import      nelumbo.integers
    Integer a
    a<0            ? [..][(a=0),..]
`;

console.log("Testing a<0 with fixpoint/inferRules trace...");
try {
  const kb = new KnowledgeBase(KnowledgeBase.BASE);
  kb.run(() => {
    const tokens = new Tokenizer(text, "test").tokenize();
    const parser = new Parser(KnowledgeBase.CURRENT, tokens);
    parser.parseEvaluate();
  });
  console.log("OK");
} catch (e) {
  console.log(`ERROR: ${(e as Error).message}`);
}
