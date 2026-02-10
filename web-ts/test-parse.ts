import { Tokenizer } from './src/Tokenizer';
import { Parser } from './src/syntax/Parser';
import { KnowledgeBase } from './src/kb/KnowledgeBase';
import { Query } from './src/kb/Query';

const code = `
Person :: Object
Person a
a=a ?
`;

console.log('Tokenizing...');
const tokenizer = new Tokenizer(code, 'test.nl');
const tokenizerResult = tokenizer.tokenize();
console.log('Tokens:', tokenizerResult.list.toArray().map(t => `${t.type.name}:${t.text}`));

console.log('\nParsing...');
const kb = new KnowledgeBase(KnowledgeBase.BASE);
const parser = new Parser(kb, tokenizerResult);
const result = parser.parseMultipleNonThrowing();

console.log('Parse exceptions:', result.exceptions().toArray().map(e => e.shortMessage));
console.log('Roots:', result.roots().toArray().map(r => r.constructor.name + ': ' + r.toString()));

for (const root of result.roots()) {
  if (root instanceof Query) {
    console.log('Query inferResult:', root.inferResult()?.toString());
  }
}
