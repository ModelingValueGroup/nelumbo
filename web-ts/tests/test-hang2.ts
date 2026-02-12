import { KnowledgeBase } from '../src/KnowledgeBase';
import { Tokenizer } from '../src/syntax/Tokenizer';
import { Parser } from '../src/syntax/Parser';
import { InferContext } from '../src/InferContext';
import { InferResult } from '../src/InferResult';
import { Predicate } from '../src/logic/Predicate';
import { CompoundPredicate } from '../src/logic/CompoundPredicate';
import { Rule } from '../src/Rule';

let callCount = 0;
const MAX = 50;

// Trace inferInternal to see what enters and exits
const origInferInternal = (Predicate.prototype as any).inferInternal;
(Predicate.prototype as any).inferInternal = function(this: Predicate, nrOfUnbound: number, context: InferContext) {
  callCount++;
  const id = callCount;
  if (id <= MAX) {
    console.log(`inferInternal[${id}]: ${this.toString()} nrOfUnbound=${nrOfUnbound} stack=${context.stack().size} shallow=${context.shallow()} reduce=${context.reduce()}`);
  }
  if (callCount > 200) {
    throw new Error(`Too many inferInternal calls`);
  }
  const result = origInferInternal.call(this, nrOfUnbound, context);
  if (id <= MAX) {
    console.log(`inferInternal[${id}]:  => ${result.toString()}`);
  }
  return result;
};

// Trace CompoundPredicate.resolve to see the do-while loop
const origCPResolve = CompoundPredicate.prototype.resolve;
let cpCount = 0;
CompoundPredicate.prototype.resolve = function(this: CompoundPredicate, context: InferContext) {
  cpCount++;
  const id = cpCount;
  if (id <= 20) {
    console.log(`CP.resolve[${id}]: ${this.toString()} stack=${context.stack().size}`);
  }
  if (cpCount > 50) {
    throw new Error(`Too many CP.resolve calls`);
  }
  const result = origCPResolve.call(this, context);
  if (id <= 20) {
    console.log(`CP.resolve[${id}]:  => ${result.toString()}`);
  }
  return result;
};

// Trace biimply
const origBiimply = Rule.prototype.biimply;
let biCount = 0;
Rule.prototype.biimply = function(this: Rule, predicate: Predicate, context: InferContext, result: InferResult) {
  biCount++;
  const id = biCount;
  if (id <= 30) {
    console.log(`biimply[${id}]: rule=${this.consequence()} <=> ${this.condition()}, pred=${predicate}, result=${result}`);
  }
  if (biCount > 100) {
    throw new Error(`Too many biimply calls`);
  }
  const res = origBiimply.call(this, predicate, context, result);
  if (id <= 30) {
    console.log(`biimply[${id}]:  => ${res.toString()}`);
  }
  return res;
};

const text = `
    import      nelumbo.integers
    Integer a
    a<0            ? [..][(a=0),..]
`;

console.log("Testing a<0 with detailed trace...");
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
