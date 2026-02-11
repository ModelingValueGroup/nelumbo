/**
 * BaseSyntax - defines the essential Nelumbo syntax patterns.
 * @JAVA_REF org.modelingvalue.nelumbo.KnowledgeBase#initBase()
 */

import { List, Map, Set } from 'immutable';
import { TokenType } from './syntax/TokenType';
import type { AstElement } from './AstElement';
import { Type, TOP_GROUP, DEFAULT_GROUP } from './Type';
import { Variable } from './Variable';
import { Node } from './Node';
import { Pattern } from './patterns/Pattern';
import { SequencePattern } from './patterns/SequencePattern';
import { Functor, type NodeConstructor } from './patterns/Functor';
import { KnowledgeBase } from './KnowledgeBase';
import { Query } from './Query';
import { Fact } from './Fact';
import { Rule } from './Rule';
import { NList, Transform } from './Transform';
import { Token } from './syntax/Token';
import { Import } from './Import';
import { Optional } from './patterns/OptionalPattern';
import { findConstructor } from './ConstructorRegistry';

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
let SEQUENCE: Pattern;
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
  SEQUENCE = r(ALTERNATIVES, true, null);
  SEQ_NO_COMMA = s(r(ALT_NO_COMMA, true, null), o(s(t('#'), t(TokenType.NUMBER))), o(s(t('@'), r(t(TokenType.NAME), true, t('.')))));

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
 * Convert raw AST elements into a Pattern.
 * @JAVA_REF org.modelingvalue.nelumbo.KnowledgeBase#pattern()
 */
function buildPattern(elements: List<AstElement>): Pattern {
  let patterns = List<Pattern>();
  let elems = elements;
  for (let i = 0; i < elems.size; i++) {
    const e = elems.get(i)!;
    if (e instanceof Token) {
      const tok = e as Token;
      let text = tok.text;
      if (tok.type === TokenType.STRING) {
        text = text.substring(1, text.length - 1);
      }
      const tokenPattern = tok.type === TokenType.STRING && TokenType.of(text) === TokenType.NAME
        ? Pattern.kWithElements(List([tok as AstElement]), text)
        : Pattern.tTextWithElements(List([tok as AstElement]), text, false);
      patterns = patterns.push(tokenPattern);
      elems = elems.set(i, tokenPattern as unknown as AstElement);
    } else if (e instanceof Variable) {
      const v = e as Variable;
      const variablePattern = Pattern.vWithElements(List([v as AstElement]), v);
      patterns = patterns.push(variablePattern);
      elems = elems.set(i, variablePattern as unknown as AstElement);
    } else {
      const pattern = e as unknown as Pattern;
      if (pattern instanceof SequencePattern) {
        const sp = pattern as SequencePattern;
        patterns = patterns.concat(sp.patternElements());
        elems = elems.delete(i);
        elems = elems.splice(i, 0, ...sp.astElements().toArray()).toList();
        i = i - 1 + sp.astElements().size;
      } else {
        patterns = patterns.push(pattern);
      }
    }
  }
  return patterns.size > 1 ? Pattern.sWithElements(elems, ...patterns.toArray()) : patterns.first()!;
}

/**
 * Register essential syntax patterns in a knowledge base.
 */
export function registerBaseSyntax(kb: KnowledgeBase): void {
  initPatterns();

  kb.run(() => {
    try {
      // 1. Equals operator: a = b
      KnowledgeBase.equalsFunctor = Functor.of(
        s(n(Type.OBJECT, 30), t('='), n(Type.OBJECT, 30)),
        Type.BOOLEAN,
        false,
        null,
        null
      ).initInKb(kb);

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
      Functor.of(
        s(t(TokenType.BEGINOFFILE), ROOTS, t(TokenType.ENDOFFILE)),
        Type.ROOT.list(TOP_GROUP),
        false,
        docConstructor,
        null
      ).initInKb(kb);

      // 3. Alternation meta-syntax: <(> seq <|> seq ... <)>
      const alternationConstructor: NodeConstructor = (elements, _args, _functor) => {
        let options = List<AstElement>();
        let list = List<AstElement>();
        for (let i = 3; i < elements.size - 2; i++) {
          const e = elements.get(i)!;
          if (e instanceof Token && (e as Token).text.startsWith('<')) {
            if (!list.isEmpty()) {
              options = options.push(buildPattern(list) as unknown as AstElement);
              list = List();
            }
            i += 2;
          } else {
            list = list.push(e);
          }
        }
        return Pattern.aWithElements(elements, ...options.toArray() as Pattern[]) as unknown as Node;
      };
      Functor.of(
        s(t('<'), t('('), t('>'), r(SEQUENCE, true, s(t('<'), t('|'), t('>'))), t('<'), t(')'), t('>')),
        Type.PATTERN,
        false,
        alternationConstructor,
        null
      ).initInKb(kb);

      // 4. Repetition meta-syntax: <(> seq <,> seq <)+/*>
      const repetitionConstructor: NodeConstructor = (elements, args, _functor) => {
        let repeated: Pattern | null = null;
        let separator: Pattern | null = null;
        let list = List<AstElement>();
        for (let i = 3; i < elements.size - 3; i++) {
          const e = elements.get(i)!;
          if (e instanceof Token && (e as Token).text.startsWith('<')) {
            if (!list.isEmpty()) {
              if (repeated === null) {
                repeated = buildPattern(list);
                list = List();
              } else {
                separator = buildPattern(list);
              }
            }
            i += 2;
          } else {
            list = list.push(e);
          }
        }
        const mandatory = args[2] === '+';
        return Pattern.rWithElements(elements, repeated!, mandatory, separator) as unknown as Node;
      };
      Functor.of(
        s(t('<'), t('('), t('>'), SEQUENCE, o(s(t('<'), t(','), t('>'), SEQUENCE)), t('<'), t(')'), a(t('*'), t('+')), t('>')),
        Type.PATTERN,
        false,
        repetitionConstructor,
        null
      ).initInKb(kb);

      // 5. Optional meta-syntax: <(> seq <)?>
      const optionalConstructor: NodeConstructor = (elements, _args, _functor) => {
        return Pattern.oWithElements(elements, buildPattern(elements)) as unknown as Node;
      };
      Functor.of(
        s(t('<'), t('('), t('>'), SEQUENCE, t('<'), t(')'), t('?'), t('>')),
        Type.PATTERN,
        false,
        optionalConstructor,
        null
      ).initInKb(kb);

      // 6. Parenthesized group: ( SEQUENCE )
      const patternGroupConstructor: NodeConstructor = (elements, _args, _functor) => {
        return Pattern.sWithElements(elements, buildPattern(elements)) as unknown as Node;
      };
      Functor.of(
        s(t(TokenType.LEFT), SEQUENCE, t(TokenType.RIGHT)),
        Type.PATTERN,
        false,
        patternGroupConstructor,
        null
      ).initInKb(kb);

      // 7. Type reference: <Type> or <Type#N>
      const typeRefConstructor: NodeConstructor = (elements, args, _functor) => {
        const type = args[0] as Type;
        let precedence: number | null = null;
        const opt = args[1] as Optional<string>;
        if (opt.isPresent()) {
          precedence = parseInt(opt.get()!, 10);
        }
        const tt = type.tokenType();
        return tt !== null
          ? Pattern.tTypeWithElements(elements, tt) as unknown as Node
          : Pattern.nWithElements(elements, type, precedence) as unknown as Node;
      };
      Functor.of(
        s(t('<'), n(Type.TYPE, Number.MAX_SAFE_INTEGER), o(s(t('#'), t(TokenType.NUMBER))), t('>')),
        Type.PATTERN,
        false,
        typeRefConstructor,
        null
      ).initInKb(kb);

      // 8. Pattern declaration: Type ::= pattern, pattern, ...
      const patternDeclConstructor: NodeConstructor = (elements, args, functor) => {
        // Re-extract args if first element is a token (private keyword present)
        if (elements.first() instanceof Token) {
          functor.functorArgs(elements, Map());
        }
        const local = (args[0] as Optional<string>).isPresent();
        const type = elements.get(local ? 1 : 0) as Type;
        const start = local ? 3 : 2;
        let roots = new NList(elements.slice(0, start).toList(), Type.ROOT);
        let pttrn = List<AstElement>();
        let ast = List<AstElement>();
        let precedence: number | null = null;
        let ctor: NodeConstructor | null = null;
        for (let i = start; i <= elements.size; i++) {
          const e = i < elements.size ? elements.get(i)! : null;
          if (e === null || e instanceof Token) {
            const tok = e as Token | null;
            if (tok === null || tok.text === ',') {
              let pattern = buildPattern(pttrn);
              if (precedence !== null) {
                pattern = pattern.setPrecedence(precedence);
              }
              roots = KnowledgeBase.CURRENT.createFunctor(type, roots, ast, ctor, pattern, local, precedence);
              if (tok !== null) {
                roots = roots.setAstElements(roots.astElements().push(tok as AstElement)) as NList;
              }
              ast = List();
              pttrn = List();
              precedence = null;
              ctor = null;
            } else if (tok.text === '#') {
              ast = ast.push(tok as AstElement);
              const nextTok = tok.next!;
              ast = ast.push(nextTok as AstElement);
              i++;
              precedence = parseInt(nextTok.text, 10);
            } else if (tok.text === '@') {
              ast = ast.push(tok as AstElement);
              let className = '';
              let nextTok = tok.next!;
              do {
                ast = ast.push(nextTok as AstElement);
                className += nextTok.text;
                i++;
                nextTok = nextTok.next!;
              } while (nextTok !== null && (nextTok.text === '.' || nextTok.type === TokenType.NAME));
              ctor = findConstructor(className);
            } else {
              pttrn = pttrn.push(e!);
            }
          } else {
            pttrn = pttrn.push(e!);
          }
        }
        return roots;
      };
      Functor.of(
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
      ).initInKb(kb);

      // 9. Type declaration: Name :: Type, Type, ... #group
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
        const groupArg = args[3] as Optional<string>;
        const group = groupArg.orElse(DEFAULT_GROUP);

        const argType = args[1] as Optional<Type>;
        let type: Type;
        if (argType.isPresent()) {
          const arg = argType.get()!;
          const v = arg.variable();
          if (v === null || !Type.TYPE.equals(v.type())) {
            currentKb.addException(new (Error as any)('Type argument ' + arg + ' must be a Variable of type <Type>'));
          }
          type = Type.withElements(elements, name, supers, group, arg);
        } else {
          type = Type.withElements(elements, name, supers, group);
        }
        const resultFunctor = currentKb.addType(type);
        return resultFunctor.setAstElements(elements) as unknown as Node;
      };
      Functor.of(
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
      ).initInKb(kb);

      // 10. Import: import name.name, ...
      const importConstructor: NodeConstructor = (elements, _args, _functor) => {
        const currentKb = KnowledgeBase.CURRENT;
        let roots = new NList(elements, Type.ROOT);
        // Extract import names from elements (tokens: import, name, ., name, ',', name, ., name, ...)
        let nameParts: string[] = [];
        for (let i = 0; i < elements.size; i++) {
          const e = elements.get(i)!;
          if (!(e instanceof Token)) continue;
          const tok = e as Token;
          if (tok.text === 'import') continue;
          if (tok.text === '.') continue;
          if (tok.text === ',') {
            if (nameParts.length > 0) {
              const importName = nameParts.join('.');
              const imp = new Import(List<AstElement>(), importName);
              imp.initInKb(currentKb);
              roots = new NList(List<AstElement>(), roots, imp);
              nameParts = [];
            }
          } else if (tok.type === TokenType.NAME) {
            nameParts.push(tok.text);
          }
        }
        if (nameParts.length > 0) {
          const importName = nameParts.join('.');
          const imp = new Import(List<AstElement>(), importName);
          imp.initInKb(currentKb);
          roots = new NList(List<AstElement>(), roots, imp);
        }
        return roots;
      };
      Functor.of(
        s(k('import'), r(r(t(TokenType.NAME), true, t('.')), true, t(','))),
        Type.ROOT.list(),
        false,
        importConstructor,
        null
      ).initInKb(kb);

      // 11. Variable declaration: Type varName, varName, ...
      const varDeclConstructor: NodeConstructor = (elements, _args, _functor) => {
        const currentKb = KnowledgeBase.CURRENT;
        const typeElem = elements.first();
        if (!(typeElem instanceof Type)) {
          return new NList(elements, Type.ROOT);
        }
        const type = typeElem as Type;
        let roots = new NList(List([type as AstElement]), Type.ROOT);

        for (let i = 1; i < elements.size; i++) {
          const e = elements.get(i)!;

          // Skip commas
          if (e instanceof Token && (e as Token).text === ',') {
            roots = roots.setAstElements(roots.astElements().push(e)) as NList;
            e;
            const nextE = elements.get(++i);
            if (!nextE) continue;
            const variable = nextE instanceof Variable
              ? new Variable(List([nextE as AstElement]), type, nextE as Variable)
              : new Variable(List([nextE as AstElement]), type, (nextE as Token).text);
            const varFunctor = currentKb.addVariable(variable);
            roots = new NList(List<AstElement>(), roots, varFunctor);
            continue;
          }

          const variable = e instanceof Variable
            ? new Variable(List([e as AstElement]), type, e as Variable)
            : new Variable(List([e as AstElement]), type, (e as Token).text);
          const varFunctor = currentKb.addVariable(variable);
          roots = new NList(List<AstElement>(), roots, varFunctor);
        }
        return roots;
      };
      Functor.of(
        s(n(Type.TYPE, Number.MAX_SAFE_INTEGER), r(t(TokenType.NAME), true, t(','))),
        Type.ROOT.list(),
        false,
        varDeclConstructor,
        0
      ).initInKb(kb);

      // 12. Rule: predicate <=> condition, condition, ...
      const ruleConstructor: NodeConstructor = (elements, args, functor) => {
        const currentKb = KnowledgeBase.CURRENT;
        const consequence = args[0] as Node;
        let condition = args[1] as Node;

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
      KnowledgeBase.ruleFunctor = Functor.of(
        s(n(Type.BOOLEAN, 0), t('<=>'), r(CONDITION, true, t(','))),
        Type.ROOT.list(),
        false,
        ruleConstructor,
        null
      ).initInKb(kb);

      // 13. Query: predicate ? [expected][falsehoods]
      const queryConstructor: NodeConstructor = (elements, args, functor) => {
        return new Query(functor, elements, ...args);
      };
      Functor.of(
        s(n(Type.BOOLEAN, 0), t('?'), o(s(t('['), PREDICTION, t(']'), t('['), PREDICTION, t(']')))),
        Type.QUERY,
        false,
        queryConstructor,
        null
      ).initInKb(kb);

      // 14. Fact: standalone boolean predicate
      const factConstructor: NodeConstructor = (elements, args, functor) => {
        return new Fact(functor, elements, ...args);
      };
      Functor.of(
        n(Type.BOOLEAN, 0),
        Type.FACT,
        false,
        factConstructor,
        null
      ).initInKb(kb);

      // 15. Parentheses grouping: (expression)
      const parenConstructor: NodeConstructor = (elements, args, _functor) => {
        const node = args[0] as Node;
        if (node && node.setAstElements) {
          return node.setAstElements(elements);
        }
        return new Node(Type.OBJECT, elements, ...args);
      };
      Functor.of(
        s(t('('), n(Type.OBJECT, 0), t(')')),
        Type.OBJECT,
        false,
        parenConstructor,
        null
      ).initInKb(kb);

      // 16. Transform: source ::> { targets }
      const transformConstructor: NodeConstructor = (elements, args, functor) => {
        const source = args[0] as Node;
        let targets = List<Node>();
        for (const arg of args[1] as List<Node>) {
          targets = targets.push(arg);
        }
        return new Transform(functor, elements, source, targets);
      };
      Functor.of(
        s(n(Type.ROOT, null), t('::>'), t('{'), ROOTS, t('}')),
        Type.TRANSFORM,
        false,
        transformConstructor,
        null
      ).initInKb(kb);

    } catch (e) {
      console.error('Error registering base syntax:', e);
      throw e;
    }
  });
}
