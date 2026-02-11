/**
 * Syntax/Parser tests - 1-to-1 port from SyntaxTest.java
 */

import { describe, it, expect } from 'vitest';
import { Tokenizer } from '../src/syntax/Tokenizer';
import { Parser } from '../src/syntax/Parser';
import { KnowledgeBase } from '../src/KnowledgeBase';
import { ParseException } from '../src/syntax/ParseException';

describe('Syntax', () => {
  it('exampleTest', () => {
    KnowledgeBase.BASE.run(() => {
      const example = `
import     nelumbo.logic

ESet     ::  Object
Int      ::  Object
EList    ::  Object
Option   ::  Object
Altern   ::  Object
Test     ::  Object                        #TEST

Int      ::= <ESet>.size,
             <NUMBER>

ESet     ::= <ESet> + <Object>             #40,
             <ESet> - <Object>             #40,
             { <(> <Object> <,> , <)*> }   @org.modelingvalue.nelumbo.Node

EList    ::= [ <(> <Object> <,> , <)*> ]   @org.modelingvalue.nelumbo.Node
EList    ::= [[ <(> <Object> <,> , <)+> ]] @org.modelingvalue.nelumbo.Node
Option   ::= ?[ <(> XX <)?> ]?             @org.modelingvalue.nelumbo.Node
Altern   ::= +[ <(> XX <|> YY <)> ]+       @org.modelingvalue.nelumbo.Node


Object   ::= <EList>.get(<Int>)

ESet   s, t, u
Int    i, j, k
Object n

10=10 ? [()][]

s=t+n ?
s=t+n ? [..][..]

s.size=i   <=> i=10

/* comment
multi
line */

`;
      try {
        const tr = new Tokenizer(example, 'SyntaxTest.exampleTest').tokenize();
        new Parser(KnowledgeBase.CURRENT, tr).parseEvaluate();
      } catch (e) {
        if (e instanceof ParseException) {
          console.error(e.message);
        }
        throw e;
      }
    });
  });

  // Note: tokenSplitTest requires loading the integers module (integers.nl) which depends on
  // nelumbo.logic import and Java class constructors (@org.modelingvalue.nelumbo.integers.*)
  // that are not yet available in the TypeScript implementation.
  it.skip('tokenSplitTest', () => {
    KnowledgeBase.BASE.run(() => {
      try {
        // Java: Parser.parse(org.modelingvalue.nelumbo.integers.NInteger.class, "integers.nl")
        // TODO: Load integers module when import mechanism is available

        const nl = '-4=-(2+2) ?';

        const tr = new Tokenizer(nl, 'SyntaxTest.tokenSplitTest').tokenize();
        let all = tr.listAll;
        expect(all.size).toBe(11);
        expect(all.map(t => t.text).join(',')).toBe(',-4,=-,(,2,+,2,), ,?,');

        const result = new Parser(KnowledgeBase.CURRENT, tr).parseEvaluate().roots();
        all = tr.listAll;
        expect(all.size).toBe(12);
        expect(all.map(t => t.text).join(',')).toBe(',-4,=,-,(,2,+,2,), ,?,');
        expect(result.size).toBe(1);

        expect(tr.list.map(t => t.text).join(',')).toBe(',-4,=,-,(,2,+,2,),?,');
        expect(result.first()!.tokens().map(t => t.type.name).join(','))
          .toBe('NUMBER,OPERATOR,OPERATOR,LEFT,NUMBER,OPERATOR,NUMBER,RIGHT,OPERATOR');
      } catch (e) {
        if (e instanceof ParseException) {
          console.error(e.message);
        }
        throw e;
      }
    });
  });
});
