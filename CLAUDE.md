# overview

This project is a programming language called Nelumbo.
It is a logical programming language in the style of Prolog.

# architecture

The main code is in src/ and written in java.
The only dependency is the java immutable collections library.
Update this CLAUDE.md file when you detect changes in the architecture.

## project layout

```
nelumbo/
  src/main/java/org/modelingvalue/nelumbo/   # Java source (leading implementation)
  src/main/resources/.../nelumbo/            # Standard library .nl files + examples
  src/test/java/.../nelumbo/test/            # Java tests (JUnit)
  web-ts/src/                                # TypeScript port (derived, 1-to-1)
  web-ts/tests/                              # TypeScript tests (Vitest)
  lsp/server/                                # Language Server Protocol server
  lsp/plugins/{vscode,intellij,eclipse,neovim}/  # IDE plugins
  docs/                                      # Documentation and slides
```

## java packages (src/main/java/org/modelingvalue/nelumbo/)

- **root package** — core classes: Node, Type, Variable, KnowledgeBase, Fact, Rule, Query, Transform, Import, InferContext, InferResult, MatchState, AstElement, Evaluatable
- **syntax/** — parsing: Parser, Tokenizer, Token, TokenType, ParseState, ParseContext, PatternResult, ParserResult, ParseException
- **patterns/** — syntax pattern matching: Pattern (abstract), Functor, SequencePattern, AlternationPattern, OptionalPattern, RepetitionPattern, TokenTextPattern, TokenTypePattern, NodeTypePattern
- **logic/** — logical inference: Predicate, CompoundPredicate, BinaryPredicate, And, Or, Not, When, Equal, NBoolean, Quantifier, ExistentialQuantifier, UniversalQuantifier
- **integers/** — integer arithmetic: NInteger, Add, Multiply, GreaterThan
- **strings/** — string operations: NString, Concat, Length, ToInteger
- **collections/** — collection types: NList, NSet
- **tools/** — Swing GUI editor: NelumboEditor, EditorWindow, TreeViewer, KnowledgeBaseViewer

## class hierarchy

```
AstElement (interface)
  ├── Token
  └── Node (base AST class, extends StructImpl)
        ├── Type
        ├── Variable
        ├── Transform
        ├── Import
        ├── Fact (implements Evaluatable)
        ├── Rule (implements Evaluatable)
        ├── Query (implements Evaluatable)
        ├── NInteger, NString, NList, NSet
        ├── Pattern (abstract)
        │     ├── SequencePattern
        │     ├── AlternationPattern
        │     ├── OptionalPattern
        │     ├── RepetitionPattern
        │     ├── TokenTextPattern
        │     ├── TokenTypePattern
        │     └── NodeTypePattern
        ├── Functor (connects patterns to AST construction)
        └── Predicate (logical predicates)
              ├── Equal
              ├── NBoolean (TRUE/FALSE/UNKNOWN singletons)
              ├── Add, Multiply, GreaterThan, Concat, Length, ToInteger
              └── CompoundPredicate (abstract)
                    ├── BinaryPredicate → And, Or, When
                    ├── Not
                    └── Quantifier → ExistentialQuantifier, UniversalQuantifier
```

## processing pipeline

1. **Tokenizer** — breaks input text into Token linked list
2. **Parser** — precedence climbing parser using ParseState state machine
3. **ParseState** — three typed transition maps (tokenTexts, tokenTypes, nodeTypes) with direction-based lookahead
4. **Pattern** — pattern classes build ParseState graphs via `state(ParseState next)`
5. **Functor** — connects patterns to node construction; registered in KnowledgeBase
6. **KnowledgeBase** — central orchestrator: stores functors, facts, rules, transforms, parse patterns; manages memoization and inference
7. **Inference** — Predicate.infer() with fixpoint computation, rule matching via MatchState, memoization

## standard library

Located in `src/main/resources/.../nelumbo/`:
- `logic/logic.nl` — boolean types, logical operators (!, &, |, ->, <->), quantifiers (E[], A[]), equality
- `integers/integers.nl` — integer operations (+, -, *, >, <)
- `strings/strings.nl` — string operations (++, length, toInteger)
- `collections/collections.nl` — collection predicates

## build

- Java: `./gradlew build` (Gradle with mvgplugin, shadow JAR for editor)
- TypeScript: `cd web-ts && npm test` (Vitest), `npm run build` (Vite + tsc)
- LSP: multi-project Gradle build under lsp/

# derived ts implementation

The web-ts/ directory contains a typescript implementation of the nelumbo language.
The following points are carved in stone, never ever deviate from them:
- It was completely generated using claude code.
- It is maintained using claude code.
- The typescript code should exactly follow the java code.
- The java code is always leading.
- When doing maintenance on the ts code you should never change java code.
- the ts tests should exactly follow the java tests.
- all java tests should be translated to ts tests.
- the ts code should be a 1-to-1 translation of the java code.
- add recognizable comments (with marker @JAVA_REF) that gives an exact reference to the java construct a class/function/constant/enum is translated from
- use these comments to keep track of the relationship between the two
- concentrate on correctly translating java to ts when debugging ts, dont try to fix problems by deviating from the java code.

## ts-specific implementation notes

- **immutable.js** (v4.3.0) is the only production dependency — replaces Java's immutable collections (List, Map, Set)
- **Node internal storage** uses `_data: unknown[]` array (index 0 = type/functor, index 1 = elements, index 2+ = args)
- **patternRegistry.ts** — registers Pattern subclasses with base Pattern to avoid circular imports; must be imported before Pattern factory methods are used
- **ConstructorRegistry.ts** — maps Java class names (from `@class` annotations in .nl files) to TS NodeConstructor functions
- **BaseSyntax.ts** — registers the 16 core syntax functors (equals, document, patterns, types, variables, rules, queries, facts, transforms, imports); equivalent to Java's KnowledgeBase.initBase()
- **ModuleContent.ts** — embeds standard library .nl files as strings for web portability (no filesystem access)
- **KnowledgeBase._current** — static field replacing Java's ThreadLocal context
- **Factory hooks on Node** (e.g., `Node._typeFromVariable`) break circular runtime dependencies

## ts directory structure

```
web-ts/src/
  ├── (core)         Node.ts, Type.ts, Variable.ts, KnowledgeBase.ts, ...
  ├── patterns/      Pattern.ts, Functor.ts, *Pattern.ts, patternRegistry.ts
  ├── logic/         Predicate.ts, And.ts, Or.ts, Not.ts, Equal.ts, NBoolean.ts, ...
  ├── syntax/        Parser.ts, Tokenizer.ts, Token.ts, TokenType.ts, ParseState.ts, ...
  ├── integers/      NInteger.ts, Add.ts, Multiply.ts, GreaterThan.ts
  ├── strings/       NString.ts, Concat.ts, Length.ts, ToInteger.ts
  ├── collections/   NList.ts, NSet.ts
  ├── editor/        Web UI components (EditorWindow, SyntaxHighlighter, etc.)
  ├── BaseSyntax.ts, ConstructorRegistry.ts, ModuleContent.ts
  └── index.ts, main.ts
web-ts/tests/
  ├── tokenizer.test.ts  (16 tests)
  ├── syntax.test.ts     (2 tests, 1 skipped)
  ├── nelumbo.test.ts    (12 tests — loads .nl resource files)
  └── resources/         .nl test files matching Java examples
```
