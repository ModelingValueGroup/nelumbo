import { describe, it, expect } from 'vitest';
import { List } from 'immutable';
import { Tokenizer } from '../src/syntax/Tokenizer';
import { Parser } from '../src/syntax/Parser';
import { KnowledgeBase } from '../src/KnowledgeBase';
import { Functor } from '../src/patterns/Functor';
import { Type } from '../src/Type';
import { SequencePattern } from '../src/patterns/SequencePattern';
import { Token } from '../src/syntax/Token';

describe('debug extractArgs', () => {
  it('should trace extractArgs', () => {
    const code = `x :: Object\n`;
    const kb = new KnowledgeBase(KnowledgeBase.BASE);
    const tokenizer = new Tokenizer(code, 'test.nl');

    const origExtract = SequencePattern.prototype.extractArgs;
    (SequencePattern.prototype as any).extractArgs = function(this: SequencePattern, elements: any, i: number, args: any, alt: boolean, functor: any, typeArgs: any) {
      if (functor?.resultType?.()?.equals?.(Type.FUNCTOR)) {
        const result: unknown[] = [];
        const subs = this.patternElements();
        console.log(`SequencePattern.extractArgs: ${subs.size} subs, starting at i=${i}, elements.size=${elements.size}`);
        for (let si = 0; si < subs.size; si++) {
          const element = subs.get(si)!;
          const inner: unknown[] = [];
          const ii = element.extractArgs(elements, i, inner, false, functor, typeArgs);
          console.log(`  sub[${si}] ${element.constructor.name}: i=${i} -> ${ii}, inner=[${inner.map((x: any) => typeof x === 'object' ? x?.constructor?.name : String(x)).join(', ')}]`);
          if (ii >= 0) {
            result.push(...inner);
            i = ii;
          } else {
            const elem = elements.get(i);
            if (elem instanceof Token) {
              console.log(`  FAILED at sub[${si}], token: type=${elem.type.name}, text="${elem.text}", isTextMatch=${elem.isTextMatch}`);
            } else {
              console.log(`  FAILED at sub[${si}], element:`, elem?.toString?.());
            }
            SequencePattern.prototype.extractArgs = origExtract;
            return -1;
          }
        }
        SequencePattern.prototype.extractArgs = origExtract;
        args.push(result.length > 1 ? List(result) : result[0]);
        return i;
      }
      return origExtract.call(this, elements, i, args, alt, functor, typeArgs);
    };

    const parser = new Parser(kb, tokenizer.tokenize());
    const result = parser.parseMultipleNonThrowing();
    SequencePattern.prototype.extractArgs = origExtract;
    console.log('Exceptions:', result.exceptions().size);
    console.log('Roots:', result.roots().size);
  });
});
