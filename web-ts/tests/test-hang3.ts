import { KnowledgeBase } from '../src/KnowledgeBase';
import { Tokenizer } from '../src/syntax/Tokenizer';
import { Parser } from '../src/syntax/Parser';
import { InferContext } from '../src/InferContext';
import { InferResult } from '../src/InferResult';
import { Predicate } from '../src/logic/Predicate';
import { CompoundPredicate, _setNBooleanRefs } from '../src/logic/CompoundPredicate';
import { Map, Set } from 'immutable';
import { Variable } from '../src/Variable';

let loopCount = 0;
const MAX_LOOPS = 10;

// Monkey-patch CompoundPredicate.resolve to trace the do-while loop
const origResolve = CompoundPredicate.prototype.resolve;
CompoundPredicate.prototype.resolve = function(this: CompoundPredicate, context: InferContext) {
  const cpId = ++loopCount;
  if (cpId > 5) return origResolve.call(this, context); // only trace first 5

  // Re-implement resolve with tracing
  type BindingKey = Map<Variable, unknown>;
  let now: Map<BindingKey, Predicate>;
  let next: Map<BindingKey, Predicate> = Map<BindingKey, Predicate>().set(this.getBinding() ?? Map(), this as Predicate);
  let facts = Set<Predicate>();
  let falsehoods = Set<Predicate>();
  let cycles = Set<Predicate>();
  let completeFacts = true;
  let completeFalsehoods = true;
  const deep = context;
  const shallow = deep.toShallow();
  const reduce = deep.toReduce();

  let iteration = 0;
  do {
    iteration++;
    now = next;
    next = Map<BindingKey, Predicate>();

    console.log(`  CP[${cpId}] iter=${iteration}: now has ${now.size} entries`);
    if (iteration > MAX_LOOPS) {
      console.log(`  CP[${cpId}] LOOP DETECTED - aborting after ${MAX_LOOPS} iterations`);
      break;
    }

    for (const [binding, predicate] of now.entries()) {
      console.log(`    CP[${cpId}] processing: ${predicate.toString()} binding=${binding.size} entries`);

      // Phase 1: Shallow
      let result = (predicate as any).infer(shallow);
      if (result.hasStackOverflow()) return result;
      console.log(`    CP[${cpId}] shallow: unresolvable=${result.unresolvable()} facts=${result.facts().size} falsehoods=${result.falsehoods().size}`);

      if (!result.unresolvable()) {
        for (const pred of (result as InferResult).allFacts()) {
          let b = pred.getBinding();
          if (b !== null && !b.isEmpty()) {
            b = binding.merge(b);
            const newPred = (predicate as any).setBinding(b).replacePredicate(pred, (Predicate as any)._booleanFromVariable ? undefined : pred);
            // Use the actual TRUE from NBoolean if available
            console.log(`    CP[${cpId}] shallow fact: ${pred.toString()} binding=${[...b.entries()].map(([k,v]) => k+'='+v).join(',')} => next entry`);
            // Can't easily call the actual code with TRUE replacement, so just call original
          }
        }
        for (const pred of (result as InferResult).allFalsehoods()) {
          let b = pred.getBinding();
          if (b !== null && !b.isEmpty()) {
            console.log(`    CP[${cpId}] shallow falsehood: ${pred.toString()}`);
          }
        }
      }

      // Phase 2: Reduce
      result = (predicate as any).infer(reduce);
      if (result.hasStackOverflow()) return result;
      console.log(`    CP[${cpId}] reduce: isFalseCC=${result.isFalseCC()} isTrueCC=${result.isTrueCC()} predicate=${result.predicateOf()?.toString() ?? 'null'}`);

      if (!result.isFalseCC() && !result.isTrueCC()) {
        // Phase 3: Deep
        const resultPredicate = result.predicateOf();
        if (resultPredicate !== null) {
          result = (resultPredicate as any).infer(deep);
          if (result.hasStackOverflow()) return result;
          console.log(`    CP[${cpId}] deep: unresolvable=${result.unresolvable()} facts=${result.facts().size} falsehoods=${result.falsehoods().size}`);

          if (!result.unresolvable()) {
            for (const pred of (result as InferResult).allFacts()) {
              let b = pred.getBinding();
              if (b !== null && !b.isEmpty()) {
                console.log(`    CP[${cpId}] deep fact: ${pred.toString()} binding=${[...b.entries()].map(([k,v]) => k+'='+v).join(',')}`);
              }
            }
          }
        }
      }
    }

    console.log(`  CP[${cpId}] end iter=${iteration}: next has ${next.size} entries`);
  } while (!next.isEmpty());

  console.log(`  CP[${cpId}] DONE after ${iteration} iterations. CALLING ORIGINAL...`);
  // Actually call the original for correct behavior
  return origResolve.call(this, context);
};

const text = `
    import      nelumbo.integers
    Integer a
    a<0            ? [..][(a=0),..]
`;

console.log("Testing a<0 with CP.resolve trace...");
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
