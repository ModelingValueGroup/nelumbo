# `nelumbo.strings`

> **Level:** optional package `nelumbo.strings`. Write `import nelumbo.strings`; it brings in `nelumbo.integers`, `nelumbo.logic` and `nelumbo.lang` transitively.

String values, concatenation, length, and integer-string conversion.

**Source:** [`src/main/resources/org/modelingvalue/nelumbo/strings/strings.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/strings/strings.nl) — 23 lines.

**Import:**

```nelumbo
import nelumbo.strings
```

`nelumbo.strings` imports `nelumbo.integers` (and so, transitively, `nelumbo.logic`), because its length and conversion operations cross over to `Integer`.

---

## Type

```nelumbo
String :: Object
```

---

## Literals

```nelumbo
String ::= <STRING>   @nelumbo.strings.NString
```

`<STRING>` is the language-level token defined in [`lang.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/lang/lang.nl) as `"([^"\\]|\\[\s\S])*"` — a double-quoted literal with backslash escapes. Examples: `""`, `"foo"`, `"Hello, World!"`. The native class `NString` wraps the parsed text as a `String` value.

---

## Operations

```nelumbo
String  ::=  <String> + <String>   #40,
             str(<Integer>)

Integer ::=  len(<String>),
             int(<String>)
```

| Pattern                | `#N` | Result    | Meaning                       |
|---|---|---|---|
| `<String> + <String>`  | 40   | `String`  | concatenation                 |
| `str(<Integer>)`       | —    | `String`  | format an integer as decimal  |
| `len(<String>)`        | —    | `Integer` | string length                 |
| `int(<String>)`        | —    | `Integer` | parse a digit string          |

All four reduce to three private native predicates:

```nelumbo
private Boolean ::= string_concat(<String>,<String>,<String>)  @nelumbo.strings.Strings,
                    string_length(<String>,<Integer>)          @nelumbo.strings.Strings,
                    integer_string(<Integer>,<String>)         @nelumbo.strings.Strings

String  a, b, c
Integer x

a + b = c    <=>  string_concat(a, b, c)
len(a) = x   <=>  string_length(a, x)
int(a) = x   <=>  integer_string(x, a)
str(x) = a   <=>  integer_string(x, a)
```

Note that `int` and `str` share a single native predicate — `integer_string(x, a)` relates the integer `x` to its decimal-string form `a`. The two surface functions are just opposite-direction wrappers around the same relation.

---

## Concatenation is relational

From `stringsTest.nl`:

```nelumbo
"foo" + "bar" = "foobar"   ? [()][]
 a    + "bar" = "foobar"   ? [(a="foo")][..]
"foo" +  a   = "foobar"    ? [(a="bar")][..]
"foo" + "bar" =  a         ? [(a="foobar")][..]
```

Any one of the three operands can be the unknown — `string_concat` splits as well as joins.

## Length

```nelumbo
len("foo") = 3   ? [()][]
len("foo") = d   ? [(d=3)][..]      // d=3 inferred
len(a)     = 3   ? [..][..]         // unknown — infinitely many strings of length 3
```

Forward direction is supported. The fully-reverse direction is unknown: there is no enumeration of strings of a given length.

## Parsing — `int(...)`

`int` expects a digit-only body; any other character — including whitespace and sign — fails:

```nelumbo
int("123456")        = 123456   ? [()][]
int("0000123456")    = d        ? [(d=123456)][..]

int("    123456")    = d        ? [][..]   // leading whitespace
int("123456    ")    = d        ? [][..]   // trailing whitespace
int("NaN")           = d        ? [][..]   // not a digit string
int("Hello, World!") = d        ? [][..]
```

A failed parse gives an empty facts side with `..` on the falsehoods.

## Formatting — `str(...)`

`str` produces the canonical decimal form — no leading zeros:

```nelumbo
str(123456)     = "123456"      ? [()][]
str(0000123456) = a             ? [(a="123456")][..]
```

`0000123456` is a literal that equals the integer `123456`; `str` formats the value, not the input syntax.

---

## Exports summary

Added to what `nelumbo.integers` and `nelumbo.logic` already export:

| Kind     | Names |
|---|---|
| Type     | `String`                          |
| Literal  | `<STRING>`                        |
| Operator | `+` (string concatenation)        |
| Functions| `str(i)`, `len(s)`, `int(s)`      |

`string_concat`, `string_length`, and `integer_string` are `private`.

---

## See also

- [`integers.md`](integers.md) — the module `strings` builds on
- [`stringsTest.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/tests/stringsTest.nl) — executable specification
- [`deHet.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/examples/deHet.nl) — natural-language DSL using strings
- [`transformation.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/examples/transformation.nl) — string-typed attributes via `::>` transformation
