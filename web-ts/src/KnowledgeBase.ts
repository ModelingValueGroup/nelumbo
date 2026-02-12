/**
 * KnowledgeBase - central hub with facts, rules, functors, memoization.
 * @JAVA_REF org.modelingvalue.nelumbo.KnowledgeBase
 */

import { List, Map, Set } from 'immutable';
import { TokenType } from './syntax/TokenType';
import type { Token } from './syntax/Token';
import type { AstElement } from './AstElement';
import { Type } from './Type';
import { Variable } from './Variable';
import { Node } from './Node';
import { Pattern } from './patterns/Pattern';
import './patterns/patternRegistry'; // Must be imported before Pattern factory methods are used
import { Functor } from './patterns/Functor';
import type { NodeConstructor } from './patterns/Functor';
import type { ParseContext } from './syntax/ParseContext';
import { ParseState } from './syntax/ParseState';
import { PatternResult } from './syntax/PatternResult';
import { ParseException } from './syntax/ParseException';
import type { Parser } from './syntax/Parser';
import { MatchState } from './MatchState';
import { Rule } from './Rule';
import type { Transform } from './Transform';
import { Predicate } from './logic/Predicate';
import { And } from './logic/And';
import { ExistentialQuantifier } from './logic/ExistentialQuantifier';
import { TokenTextPattern } from './patterns/TokenTextPattern';
import { NList } from './collections/NList';
import { InferResult } from './InferResult';
import { InferContext } from './InferContext';
import { registerBaseSyntax } from './BaseSyntax';
import { resolveModuleContent } from './ModuleContent';
import { checkFunctorSetter } from './ConstructorRegistry';
import { Tokenizer } from './syntax/Tokenizer';
import { Parser as ParserClass } from './syntax/Parser';

/**
 * Exception handler interface.
 */
export interface ParseExceptionHandler {
  addException(exception: ParseException): void;
  exceptions(): List<ParseException>;
}

/**
 * KnowledgeBase - central knowledge store.
 */
export class KnowledgeBase implements ParseExceptionHandler {
  // Current context (thread-local equivalent)
  private static _current: KnowledgeBase | null = null;

  static get CURRENT(): KnowledgeBase {
    if (KnowledgeBase._current === null) {
      throw new Error('No current KnowledgeBase');
    }
    return KnowledgeBase._current;
  }

  static setCurrent(kb: KnowledgeBase | null): void {
    KnowledgeBase._current = kb;
  }

  // Static base instance
  static BASE: KnowledgeBase;

  // Static functor references (set during initBase)
  static equalsFunctor: Functor;
  static ruleFunctor: Functor;

  // Instance state
  private _functors: Set<Functor> = Set();
  private _facts: Map<Predicate, InferResult> = Map();
  private _rules: Set<Rule> = Set();
  private _transforms: Set<Transform> = Set();
  private _memoization: Map<Predicate, InferResult> = Map();
  private _context: InferContext | null = null;

  private _prePatterns: Map<string, ParseState> = Map();
  private _postPatterns: Map<string, ParseState> = Map();
  private _localPrePatterns: Map<string, ParseState> = Map();
  private _localPostPatterns: Map<string, ParseState> = Map();

  private _literalFunctors: Map<Functor, Functor> = Map();
  private _imported: Set<string> = Set();

  private _ruleSignatures: MatchState<Rule> = MatchState.empty<Rule>();
  private _transformSignatures: MatchState<Transform> = MatchState.empty<Transform>();

  private _exceptionHandler: ParseExceptionHandler | null = null;
  private _exceptions: ParseException[] = [];

  private readonly _init: KnowledgeBase | null;

  constructor(init: KnowledgeBase | null = null) {
    this._init = init;
    this.initKb();
    this._context = InferContext.of(this, List(), Map(), false, false, false);
  }

  private initKb(): void {
    if (this._init !== null) {
      this._functors = this._init._functors;
      this._facts = this._init._facts;
      this._rules = this._init._rules;
      this._transforms = this._init._transforms;
      this._prePatterns = this._init._prePatterns;
      this._postPatterns = this._init._postPatterns;
      this._localPrePatterns = this._prePatterns;
      this._localPostPatterns = this._postPatterns;
      this._literalFunctors = this._init._literalFunctors;
      this._ruleSignatures = this._init._ruleSignatures;
      this._transformSignatures = this._init._transformSignatures;
      this._imported = this._init._imported;
      this._memoization = this._init._memoization;
    }
  }

  /**
   * Run a function with this knowledge base as current.
   */
  run<T>(fn: () => T): T {
    const previous = KnowledgeBase._current;
    KnowledgeBase._current = this;
    try {
      return fn();
    } finally {
      KnowledgeBase._current = previous;
    }
  }

  /**
   * Set the exception handler.
   */
  setExceptionHandler(handler: ParseExceptionHandler): void {
    this._exceptionHandler = handler;
  }

  /**
   * End parsing phase.
   */
  endParsing(multiple: boolean): void {
    this._exceptionHandler = null;
    if (!multiple) {
      this._localPrePatterns = this._prePatterns;
      this._localPostPatterns = this._postPatterns;
    }
  }

  /**
   * Add an exception.
   */
  addException(exception: ParseException): void {
    if (this._exceptionHandler !== null) {
      this._exceptionHandler.addException(exception);
    } else {
      this._exceptions.push(exception);
      throw exception;
    }
  }

  /**
   * Get all exceptions.
   */
  exceptions(): List<ParseException> {
    if (this._exceptionHandler !== null) {
      return this._exceptionHandler.exceptions();
    }
    return List(this._exceptions);
  }

  /**
   * Get group state for parsing.
   */
  groupState(group: string): ParseState | null {
    return this._localPrePatterns.get(group) ?? null;
  }

  /**
   * Look up a variable.
   */
  // @JAVA_REF KnowledgeBase.variable(Token, String, Parser)
  variable(token: Token, group: string, _parser: Parser): Variable | null {
    const state = this.groupState(group);
    const found = state !== null ? state.tokenTexts().get(token.text) : undefined;
    if (found !== undefined && found.functor() !== null && found.functor()!.resultType() === Type.VARIABLE) {
      return found.functor()!.construct(List([token as AstElement]), []) as unknown as Variable;
    }
    return null;
  }

  /**
   * Pre-parse phase.
   */
  preParse(
    token: Token | null,
    ctx: ParseContext,
    left: Node | null,
    parser: Parser
  ): PatternResult | null {
    const patterns = left !== null
      ? this._localPostPatterns
      : this._localPrePatterns;
    const state = patterns.get(ctx.group());

    if (state === undefined) return null;

    return this.doPreParse(token, left, parser, state, ctx);
  }

  // @JAVA_REF KnowledgeBase.preParse(Token, Node, Parser, ParseState, ParseContext)
  private doPreParse(
    token: Token | null,
    left: Node | null,
    parser: Parser,
    state: ParseState,
    ctx: ParseContext
  ): PatternResult | null {
    if (left !== null) {
      for (const sup of left.type().allSupers()) {
        const found = state.nodeTypes().get(sup);
        if (found !== undefined) {
          const result = new PatternResult(parser, ctx);
          result.left(left);
          return found.parse(token, result, Map(), true) ? result : null;
        }
      }
      return null;
    }

    const result = new PatternResult(parser, ctx);
    return state.parse(token, result, Map(), true) ? result : null;
  }

  /**
   * Register a functor.
   */
  register(functor: Functor): Functor {
    const type = functor.resultType();
    let group = type.group();

    // For variables, get group from constructed variable
    if (Type.VARIABLE.isAssignableFrom(type)) {
      const constructed = functor.construct(List(), []);
      group = constructed.type().group();
    }

    const local = functor.local();

    try {
      const pre = functor.preStart();
      const post = functor.postStart();

      if (!local) {
        if (pre !== null) {
          this._prePatterns = this._prePatterns.set(
            group,
            pre.merge(this._prePatterns.get(group) ?? null)
          );
        }
        if (post !== null) {
          this._postPatterns = this._postPatterns.set(
            group,
            post.merge(this._postPatterns.get(group) ?? null)
          );
        }
      }

      if (pre !== null) {
        this._localPrePatterns = this._localPrePatterns.set(
          group,
          pre.merge(this._localPrePatterns.get(group) ?? null)
        );
      }
      if (post !== null) {
        this._localPostPatterns = this._localPostPatterns.set(
          group,
          post.merge(this._localPostPatterns.get(group) ?? null)
        );
      }
    } catch (e) {
      if (e instanceof Error) {
        this.addException(ParseException.fromElements(e.message, functor));
      }
    }

    if (!local) {
      this._functors = this._functors.add(functor);
    }

    // @JAVA_REF org.modelingvalue.nelumbo.KnowledgeBase#init (FUNCTOR_REGISTRATION check)
    const ctor = functor.constructorFn();
    if (ctor !== null) {
      checkFunctorSetter(ctor, functor);
    }

    return functor;
  }

  /**
   * Get all functors.
   */
  functors(): Set<Functor> {
    return this._functors;
  }

  /**
   * Get literal functor for a functor.
   */
  literal(functor: Functor): Functor | null {
    return this._literalFunctors.get(functor) ?? null;
  }

  /**
   * Add a literal functor mapping.
   */
  addLiteral(nodFunctor: Functor, litFunctor: Functor): void {
    this._literalFunctors = this._literalFunctors.set(nodFunctor, litFunctor);
  }

  /**
   * Add a type to the knowledge base.
   */
  addType(type: Type): Functor {
    const variable = type.variable();
    let pattern: Pattern;

    if (variable !== null) {
      pattern = Pattern.tVarWithElements(List([type as AstElement]), variable);
    } else if (type.isCollection()) {
      pattern = Pattern.sWithElements(
        List([type as AstElement]),
        Pattern.tTextWithElements(List(), type.rawName(), false),
        Pattern.t('<'),
        Pattern.n(Type.TYPE, Number.MAX_SAFE_INTEGER),
        Pattern.t('>')
      );
    } else {
      pattern = Pattern.tTextWithElements(List([type as AstElement]), type.rawName(), false);
    }

    const functor = Functor.ofWithElements(
      List([type as AstElement]),
      pattern,
      Type.TYPE,
      false,
      ((elements, args, fn) => {
        let result = (fn.astElements().first() as Type).setAstElements(elements);
        if (result.isCollection() && args[0] instanceof Type) {
          result = result.setElement(args[0] as Type);
        }
        return result.setFunctor(fn) as unknown as Node;
      }) as NodeConstructor,
      null
    );

    return this.register(functor);
  }

  /**
   * Add a variable to the knowledge base.
   */
  addVariable(variable: Variable): Functor {
    if (Type.TYPE.equals(variable.type())) {
      return this.addType(Type.fromVariable(variable));
    }

    const functor = Functor.ofWithElements(
      List([variable as AstElement]),
      Pattern.tVarWithElements(List([variable as AstElement]), variable),
      Type.VARIABLE,
      true,
      ((elements, _args, fn) => {
        const result = (fn.astElements().first() as Variable).setAstElements(elements);
        return result.setFunctor(fn) as unknown as Node;
      }) as NodeConstructor,
      null
    );

    return this.register(functor);
  }

  /**
   * Create a functor from a pattern declaration (::=).
   * @JAVA_REF org.modelingvalue.nelumbo.KnowledgeBase#createFunctor()
   */
  createFunctor(type: Type, roots: NList, ast: List<AstElement>, constructor: NodeConstructor | null, pattern: Pattern, local: boolean, prec: number | null): NList {
    let toLiteral = false;
    let isFunction = false;
    const args = pattern.argTypes(List());
    const e = type.isCollection() ? type.element() : null;
    if (args.every(t => !(Type.OBJECT.isAssignableFrom(t) && !t.equals(e)))) {
      type = type.literal();
    } else {
      if (!Type.BOOLEAN.isAssignableFrom(type) && !Type.ROOT.isAssignableFrom(type)) {
        type = type.function();
        isFunction = true;
      }
      if (!Type.ROOT.isAssignableFrom(type) && !Type.COLLECTION.isAssignableFrom(type)
        && !args.every(t => Type.OBJECT.equals(t.element()))
        && !args.every(t => Type.BOOLEAN.isAssignableFrom(t.element()) || Type.VARIABLE.isAssignableFrom(t.element()))
        && !args.some(t => Type.LITERAL.isAssignableFrom(t.element()))) {
        toLiteral = true;
      }
    }
    const nodType = toLiteral && Type.FACT_TYPE.isAssignableFrom(type) ? Type.BOOLEAN : type;
    const nodFunctor = Functor.ofWithElements(
      ast.unshift(pattern as unknown as AstElement),
      pattern,
      nodType,
      local,
      toLiteral ? null : constructor,
      prec
    ).initInKb(this);
    roots = new NList(List<AstElement>(), roots, nodFunctor);
    if (pattern instanceof TokenTextPattern) {
      nodFunctor.construct(List<AstElement>(), []);
    }
    if (toLiteral) {
      const litPattern = pattern.setTypes((t: Type) => t.literal());
      const litFunctor = Functor.ofWithElements(
        List<AstElement>(),
        litPattern,
        type,
        local,
        constructor,
        prec
      ).initInKb(this);
      roots = new NList(List<AstElement>(), roots, litFunctor);
      this.addLiteral(nodFunctor, litFunctor);
      // Implied Rule
      const nodVars: Variable[] = new Array(args.size);
      const litVars: Variable[] = new Array(args.size);
      const litArgs = args.map((t: Type) => t.literal());
      for (let v = 0; v < args.size; v++) {
        nodVars[v] = new Variable(List<AstElement>(), args.get(v)!, 'n' + (v + 1));
        litVars[v] = new Variable(List<AstElement>(), litArgs.get(v)!, 'l' + (v + 1));
      }
      const nodNode = nodFunctor.construct(List<AstElement>(), nodVars);
      const litNode = litFunctor.construct(List<AstElement>(), litVars);
      const rightVar = isFunction ? new Variable(List<AstElement>(), type.nonFunction(), 'r') : null;
      let nodCons: Predicate = isFunction
        ? new Predicate(KnowledgeBase.equalsFunctor, List<AstElement>(), nodNode, rightVar)
        : nodNode as Predicate;
      let litCond: Predicate = isFunction
        ? new Predicate(KnowledgeBase.equalsFunctor, List<AstElement>(), litNode, rightVar)
        : litNode as Predicate;
      for (let c = args.size - 1; c >= 0; c--) {
        const eq = new Predicate(KnowledgeBase.equalsFunctor, List<AstElement>(), nodVars[c], litVars[c]);
        // @JAVA_REF: litCond = And.of(eq, litCond);
        litCond = And.of(eq, litCond);
      }
      // @JAVA_REF: new ExistentialQuantifier(List.of(), List.of(litVars), litCond)
      const exists = new ExistentialQuantifier(ExistentialQuantifier.FUNCTOR!, List<AstElement>(), List(litVars), litCond);
      const rule = new Rule(KnowledgeBase.ruleFunctor, List<AstElement>(), nodCons, exists);
      this.addRule(rule);
      roots = new NList(List<AstElement>(), roots, rule);
    }
    return roots;
  }

  /**
   * Add a rule to the knowledge base.
   */
  addRule(rule: Rule): Rule {
    this._rules = this._rules.add(rule);
    const state = rule.consequence().state(MatchState.of<Rule>(rule));
    this._ruleSignatures = state.merge(this._ruleSignatures);
    this.resetMemoization();
    return rule;
  }

  /**
   * Get rules matching a predicate.
   */
  getRules(predicate: Predicate): Set<Rule> {
    return this._ruleSignatures.match(predicate);
  }

  /**
   * Get all rules.
   */
  rules(): Set<Rule> {
    return this._rules;
  }

  /**
   * Add a transform to the knowledge base.
   */
  addTransform(transform: Transform): Transform {
    this._transforms = this._transforms.add(transform);
    const source = transform.source();
    const state = source.state(MatchState.of<Transform>(transform));
    this._transformSignatures = state.merge(this._transformSignatures);
    return transform;
  }

  /**
   * Get transforms matching a node.
   */
  getTransforms(root: Node): Set<Transform> {
    return this._transformSignatures.match(root);
  }

  /**
   * Get all transforms.
   */
  transforms(): Set<Transform> {
    return this._transforms;
  }

  /**
   * Add a fact to the knowledge base.
   */
  addFact(fact: Predicate): void {
    this._facts = this._facts.set(fact, fact.factCC());
    this.resetMemoization();
  }

  /**
   * Get facts for a predicate.
   */
  getFacts(predicate: Predicate, _context: InferContext): InferResult {
    const result = this._facts.get(predicate);
    if (result !== undefined) {
      return result.cast(predicate);
    }
    if (predicate.isFullyBound()) {
      return predicate.falsehoodCC();
    }
    return InferResult.factsCI(Set());
  }

  /**
   * Get all facts.
   */
  facts(): Map<Predicate, InferResult> {
    return this._facts;
  }

  /**
   * Get memoized result.
   */
  // @JAVA_REF org.modelingvalue.nelumbo.KnowledgeBase#getMemoiz(Predicate)
  getMemoiz(predicate: Predicate): InferResult | null {
    const result = this._memoization.get(predicate);
    if (result !== undefined) {
      return result.cast(predicate);
    }
    return null;
  }

  /**
   * Store memoization.
   */
  memoization(predicate: Predicate, result: InferResult): void {
    const known = result.cycles().isEmpty() && result.isComplete();
    if (known) {
      this._memoization = this._memoization.set(predicate, result);
    }
    for (const fact of result.facts()) {
      this._memoization = this._memoization.set(fact, fact.factCC());
    }
    for (const falsehood of result.falsehoods()) {
      this._memoization = this._memoization.set(falsehood, falsehood.falsehoodCC());
    }
  }

  /**
   * Reset memoization.
   */
  private resetMemoization(): void {
    this._memoization = this._init?._memoization ?? Map();
  }

  /**
   * Create an inference context.
   */
  createInferContext(): InferContext {
    return this._context!;
  }

  /**
   * Get the context.
   */
  context(): InferContext {
    return this._context!;
  }

  static _importCache: globalThis.Map<string, KnowledgeBase> = new globalThis.Map();

  /**
   * Do an import.
   */
  doImport(name: string, imp: Node): void {
    if (!this._imported.contains(name)) {
      this._imported = this._imported.add(name);
      let cached = KnowledgeBase._importCache.get(name);
      if (!cached) {
        const content = resolveModuleContent(name);
        if (content === null) return;
        cached = new KnowledgeBase(KnowledgeBase.BASE);
        cached.run(() => {
          new ParserClass(cached!, new Tokenizer(content, name).tokenize()).parseEvaluate();
        });
        KnowledgeBase._importCache.set(name, cached);
      }
      this.merge(cached, imp as AstElement);
    }
  }

  /**
   * Merge another knowledge base.
   */
  merge(kb: KnowledgeBase, _element: AstElement): void {
    this._functors = this._functors.union(kb._functors);
    this._facts = this._facts.merge(kb._facts);
    this._rules = this._rules.union(kb._rules);
    this._transforms = this._transforms.union(kb._transforms);
    const mergeStates = (existing: ParseState, incoming: ParseState) => incoming.merge(existing);
    this._prePatterns = this._prePatterns.mergeWith(mergeStates, kb._prePatterns);
    this._postPatterns = this._postPatterns.mergeWith(mergeStates, kb._postPatterns);
    this._localPrePatterns = this._localPrePatterns.mergeWith(mergeStates, kb._prePatterns);
    this._localPostPatterns = this._localPostPatterns.mergeWith(mergeStates, kb._postPatterns);
    this._literalFunctors = this._literalFunctors.merge(kb._literalFunctors);
    this._ruleSignatures = kb._ruleSignatures.merge(this._ruleSignatures);
    this._transformSignatures = kb._transformSignatures.merge(this._transformSignatures);
    this._imported = this._imported.union(kb._imported);
    this.resetMemoization();
  }

  /**
   * Get imported names.
   */
  imported(): Set<string> {
    return this._imported;
  }

  /**
   * Initialize base knowledge base.
   */
  static initBase(): KnowledgeBase {
    const kb = new KnowledgeBase(null);

    kb.run(() => {
      // Add predefined types
      for (const type of Type.predefined()) {
        kb.addType(type);
      }

      // Add token types
      for (const tt of TokenType.getMatchedTypes()) {
        if (!tt.isNotMatched() && !tt.isSkip()) {
          kb.addType(Type.fromTokenType(tt));
        }
      }
    });

    // Register base syntax patterns (after types are added)
    registerBaseSyntax(kb);

    return kb;
  }
}

// Initialize base knowledge base
KnowledgeBase.BASE = KnowledgeBase.initBase();
