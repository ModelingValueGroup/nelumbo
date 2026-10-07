# Bootstrap grammar

> **Level:** Java core. Always present, nothing to import.

`KnowledgeBase.BASE`, the knowledge base every file starts from, begins with a small grammar written in Java (`KnowledgeBase.initBase()`). It holds just enough syntax to read [`lang.nl`](../lang/index.md). Nothing else is in `BASE`. Every other construct, including most of the syntax you write, arrives through `import`.

---

## What the Java core declares

| Construct | Shape | Re-declared in `lang.nl` as |
|---|---|---|
| File | begin-of-file, top-level statements separated by newlines, end-of-file | `Namespace ::= <BEGINOFFILE> ... <ENDOFFILE>` |
| Import | `import a.b.c, d.e` | `Root ::= "import" ...` |
| Type declaration | `T :: S1, S2`, `T<A> :: S`, optional `#GROUP` | `Root ::= ... :: ...` |
| Pattern declaration | `private? T ::= P #N @qualified.Class, ...` | `Root ::= ... ::= ...` |
| Named pattern | `pattern N ::= P` | `PatternPart ::= "pattern" <NAME> ::= <PATTERNS>` |
| Token patterns | a `NAME`, `OPERATOR`, `SEMICOLON`, `SINGLEQUOTE`, `COMMA` or `STRING` token as a literal | `Pattern ::= <NAME>, <STRING>, ...` |
| Alternation | `<(> P <|> Q <)>` | `Pattern ::= ...` |
| Repetition | `<(> P <,> sep <)*>`, `<(> P <)+>` | `Pattern ::= ...` |
| Optional | `<(> P <)?>` | `Pattern ::= ...` |
| Sequence | `( P )`, `[ P ]`, `{ P }`, and the connected group `<[> P <]>` | `Pattern ::= ...` |
| Type hole | `<T>`, `<T#100>`, `<hidden T>`, `<visible T>` | `Pattern ::= "<" ... <Type#100> ... ">"` |
| Named-pattern hole | `<N>` | `Pattern ::= "<" <PatternPart#100> ">"` |

Besides these patterns the core registers one type per token type (`NAME`, `NUMBER`, `STRING`, ...) and a fixed set of predefined types (`Type.predefined()`): `NATIVE`, `Object`, `Type`, `World`, `Namespace`, `Function`, `Literal`, `Root`, `Boolean`, `FactType`, `Variable`, `Functor`, `PatternPart`, `Pattern`, `Struct`, `Collection`, `Set`, `List` and `Lambda`. The Java classes behind the reasoner and the collections refer to these types. Being registered does not give them any syntax: values, operators and statements for `Boolean`, `FactType` or `Set` only exist once the `.nl` file that declares them is imported.

---

## What happens next

`import nelumbo.lang` loads `lang.nl`. It declares the constructs above again, as ordinary `::=` patterns bound to Java classes with `@`, and adds the rest of the `nelumbo.lang` syntax: variable declarations, `::>` transformations, `{ }` scope blocks, the `<Variable>` hole and generic parenthesisation. From then on files are parsed with those declarations.

The other levels come in the same way. `nelumbo.logic` imports `nelumbo.lang`, `nelumbo.integers` imports `nelumbo.logic`, and the other packages import `nelumbo.integers`.

---

## See also

- [`nelumbo.lang`](../lang/index.md): the module this grammar exists to load
- [Architecture](../../explanation/architecture.md): how the levels fit together
- [Native API](native-api.md): how `@`-bound Java classes implement patterns
