/**
 * BaseSyntax - defines the essential Nelumbo syntax patterns.
 * Ported from Java: org.modelingvalue.nelumbo.KnowledgeBase.initBase()
 */

import { List, Set } from 'immutable';
import { TokenType } from './syntax/TokenType';
import type { AstElement } from './AstElement';
import { Type, TOP_GROUP, DEFAULT_GROUP } from './Type';
import { Variable } from './Variable';
import { Node } from './Node';
import { Pattern } from './patterns/Pattern';
import { Functor, type NodeConstructor } from './patterns/Functor';
import { KnowledgeBase } from './KnowledgeBase';
import { Query } from './Query';
import { Fact } from './Fact';
import { Rule } from './Rule';
import { NList } from './Transform';
import type { Token } from './syntax/Token';

// Helper to create patterns more easily
const s = Pattern.s.bind(Pattern);
const n = Pattern.n.bind(Pattern);
const t = Pattern.t.bind(Pattern);
const k = Pattern.k.bind(Pattern);
const r = Pattern.r.bind(Pattern);
const o = Pattern.o.bind(Pattern);
const a = Pattern.a.bind(Pattern);

// Token type patterns for building ALTERNATIVES
const TOKEN_TYPES = [TokenType.NAME, TokenType.OPERATOR, TokenType.STRING, TokenType.SEMICOLON, TokenType.SINGLEQUOTE];

// Build the pattern alternatives (any token type or nested pattern)
function buildPatterns(): Pattern[] {
  const patterns: Pattern[] = TOKEN_TYPES.map(tt => t(tt));
  patterns.push(n(Type.PATTERN, Number.MAX_SAFE_INTEGER));
  return patterns;
}

function buildPatternsWithComma(): Pattern[] {
  return [t(TokenType.COMMA), ...buildPatterns()];
}

// Core pattern definitions - lazily initialized to avoid circular dependency issues
let ALTERNATIVES: Pattern;
let SEQ_NO_COMMA: Pattern;
let CONDITION: Pattern;
let PREDICTION: Pattern;
let ROOTS: Pattern;

function initPatterns(): void {
  if (ALTERNATIVES) return; // Already initialized

  const PATTERNS_NO_COMMA = buildPatterns();
  const ALT_NO_COMMA = a(...PATTERNS_NO_COMMA);
  const PATTERNS = buildPatternsWithComma();
  ALTERNATIVES = a(...PATTERNS);
  SEQ_NO_COMMA = s(r(ALT_NO_COMMA, true, null), o(t(TokenType.NEWLINE)));

  // For rules: condition with optional 'if' clause
  CONDITION = s(n(Type.BOOLEAN, 0), o(s(k('if'), n(Type.BOOLEAN, 0))));

  // For query predictions
  const SINGLE = s(n(Type.VARIABLE, 100), t('='), n(Type.OBJECT, 100));
  const BINDING = s(t('('), r(SINGLE, false, t(',')), t(')'));
  const ALTERNATIVE = a(t('..'), BINDING);
  PREDICTION = r(ALTERNATIVE, false, t(','));

  // Root elements (statements separated by newlines)
  ROOTS = r(s(a(n(Type.ROOT.list(), null), n(Type.ROOT, null)), t(TokenType.NEWLINE)), false, null);
}

/**
 * Register essential syntax patterns in a knowledge base.
 */
export function registerBaseSyntax(kb: KnowledgeBase): void {
  initPatterns();

  kb.run(() => {
    try {
      // 1. Equals operator: a = b
      kb.register(Functor.of(
        s(n(Type.OBJECT, 30), t('='), n(Type.OBJECT, 30)),
        Type.BOOLEAN,
        false,
        null,
        null
      ));

      // 2. Document structure: BEGINOFFILE roots ENDOFFILE
      const docConstructor: NodeConstructor = (elements, args, _functor) => {
        let roots = List<Node>();
        for (const arg of args) {
          if (arg instanceof Node) {
            roots = roots.push(arg);
          }
        }
        return new NList(Type.ROOT.list(), elements, roots);
      };
      kb.register(Functor.of(
        s(t(TokenType.BEGINOFFILE), ROOTS, t(TokenType.ENDOFFILE)),
        Type.ROOT.list(TOP_GROUP),
        false,
        docConstructor,
        null
      ));

      // 3. Type declaration: Name :: Type, Type, ... #group
      const typeDeclConstructor: NodeConstructor = (elements, args, _functor) => {
        const currentKb = KnowledgeBase.CURRENT;
        let supers = Set<Type>();
        const supersList = args[2];
        if (List.isList(supersList)) {
          for (const sup of supersList as List<Type>) {
            supers = supers.add(sup);
          }
        }

        const name = args[0] as string;
        const groupArg = args[3];
        let group = DEFAULT_GROUP;
        if (groupArg && typeof groupArg === 'object' && 'isPresent' in groupArg) {
          const opt = groupArg as { isPresent(): boolean; get(): string };
          if (opt.isPresent()) {
            group = opt.get();
          }
        }

        const type = Type.namedWithGroup(name, group, ...supers.toArray());
        const functor = currentKb.addType(type);
        // Set the parsed elements so nextToken() works correctly
        return functor.setAstElements(elements) as unknown as Node;
      };
      kb.register(Functor.of(
        s(
          t(TokenType.NAME),
          o(s(t('<'), n(Type.TYPE, Number.MAX_SAFE_INTEGER), t('>'))),
          t('::'),
          r(n(Type.TYPE, Number.MAX_SAFE_INTEGER), true, t(',')),
          o(s(t('#'), t(TokenType.NAME)))
        ),
        Type.FUNCTOR,
        false,
        typeDeclConstructor,
        null
      ));

      // 4. Variable declaration: Type varName, varName, ...
      const varDeclConstructor: NodeConstructor = (elements, _args, _functor) => {
        const currentKb = KnowledgeBase.CURRENT;
        const typeElem = elements.first();
        if (!(typeElem instanceof Type)) {
          return new NList(elements, Type.ROOT);
        }
        const type = typeElem as Type;
        let roots = new NList(List([type as AstElement]), Type.ROOT);

        for (let i = 1; i < elements.size; i++) {
          const e = elements.get(i);
          if (!e) continue;

          // Skip commas
          if ('text' in e && (e as Token).text === ',') {
            roots = roots.setAstElements(roots.astElements().push(e)) as NList;
            continue;
          }

          if ('text' in e) {
            const token = e as Token;
            const variable = new Variable(List([e as AstElement]), type, token.text);
            const varFunctor = currentKb.addVariable(variable);
            roots = new NList(List(), Type.ROOT, roots, varFunctor);
          }
        }
        return roots;
      };
      kb.register(Functor.of(
        s(n(Type.TYPE, Number.MAX_SAFE_INTEGER), r(t(TokenType.NAME), true, t(','))),
        Type.ROOT.list(),
        false,
        varDeclConstructor,
        0
      ));

      // 5. Rule: predicate <=> condition, condition, ...
      const ruleConstructor: NodeConstructor = (elements, args, functor) => {
        const currentKb = KnowledgeBase.CURRENT;
        // Simplified rule creation - combines conditions with AND
        const consequence = args[0] as Node;
        let condition = args[1] as Node;

        // If there are multiple conditions, they're in a list - use first for now
        if (List.isList(args[1])) {
          const condList = args[1] as List<Node>;
          if (condList.size > 0) {
            condition = condList.first()!;
          }
        }

        const rule = new Rule(functor, elements, consequence as any, condition as any);
        currentKb.addRule(rule);
        return new NList(elements, Type.ROOT, rule);
      };
      kb.register(Functor.of(
        s(n(Type.BOOLEAN, 0), t('<=>'), r(CONDITION, true, t(','))),
        Type.ROOT.list(),
        false,
        ruleConstructor,
        null
      ));

      // 6. Query: predicate ? [expected][falsehoods]
      const queryConstructor: NodeConstructor = (elements, args, functor) => {
        return new Query(functor, elements, ...args);
      };
      kb.register(Functor.of(
        s(n(Type.BOOLEAN, 0), t('?'), o(s(t('['), PREDICTION, t(']'), t('['), PREDICTION, t(']')))),
        Type.QUERY,
        false,
        queryConstructor,
        null
      ));

      // 7. Fact: standalone boolean predicate
      const factConstructor: NodeConstructor = (elements, args, functor) => {
        return new Fact(functor, elements, ...args);
      };
      kb.register(Functor.of(
        n(Type.BOOLEAN, 0),
        Type.FACT,
        false,
        factConstructor,
        null
      ));

      // 8. Parentheses grouping: (expression)
      const parenConstructor: NodeConstructor = (elements, args, _functor) => {
        const node = args[0] as Node;
        if (node && node.setAstElements) {
          return node.setAstElements(elements);
        }
        return new Node(Type.OBJECT, elements, ...args);
      };
      kb.register(Functor.of(
        s(t('('), n(Type.OBJECT, 0), t(')')),
        Type.OBJECT,
        false,
        parenConstructor,
        null
      ));

      // 9. Import: import name.name, ...
      const importConstructor: NodeConstructor = (elements, _args, _functor) => {
        // Simplified import handling - just creates a root node
        return new NList(elements, Type.ROOT);
      };
      kb.register(Functor.of(
        s(k('import'), r(r(t(TokenType.NAME), true, t('.')), true, t(','))),
        Type.ROOT.list(),
        false,
        importConstructor,
        null
      ));

      // 10. Pattern declaration: Type ::= pattern, pattern, ...
      const patternDeclConstructor: NodeConstructor = (elements, args, _functor) => {
        // Check if first arg is 'private'
        const privateArg = args[0];
        const isPrivate = privateArg && typeof privateArg === 'object' &&
          'isPresent' in privateArg && (privateArg as { isPresent(): boolean }).isPresent();
        const typeIdx = isPrivate ? 1 : 0;

        // Find the type in elements
        let type: Type | null = null;
        for (let i = 0; i <= typeIdx && i < elements.size; i++) {
          const e = elements.get(i);
          if (e instanceof Type) {
            type = e;
            break;
          }
        }

        if (!type) {
          return new NList(elements, Type.ROOT);
        }

        // For now, just return the elements as a root
        // Full implementation would parse patterns and create functors
        return new NList(elements, Type.ROOT);
      };
      kb.register(Functor.of(
        s(
          o(k('private')),
          n(Type.TYPE, Number.MAX_SAFE_INTEGER),
          t('::='),
          r(SEQ_NO_COMMA, true, t(','))
        ),
        Type.ROOT.list(),
        false,
        patternDeclConstructor,
        null
      ));

    } catch (e) {
      console.error('Error registering base syntax:', e);
      throw e;
    }
  });
}
