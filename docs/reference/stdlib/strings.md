# `nelumbo.strings`

UTF-8 string values, concatenation, length, and integer-string conversion.

**Source:** [`src/main/resources/org/modelingvalue/nelumbo/strings/strings.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/strings/strings.nl) — 24 lines.

**Import:**

```
import nelumbo.strings
```

`nelumbo.strings` imports `nelumbo.integers` (and thus `logic`), because its length and conversion operations produce or consume `Integer` values.

---

## Type

```
String :: Object
```

---

## Literals

```
String ::= <STRING>   @...NString
```

`<STRING>` matches a double-quoted string literal: `""`, `"foo"`, `"Hello, World!"`.

---

## Operations

| Pattern | `#N` | Meaning |
|---|---|---|
| `<String> + <String>` | 40 | concatenation |
| `str(<Integer>)`      | —  | integer to string |
| `len(<String>)`       | —  | string length (as `Integer`) |
| `int(<String>)`       | —  | parse string as integer |

All four are defined in Nelumbo over three private native primitives:

```
private Boolean ::= string_concat(<String>, <String>, <String>)  @...Concat
private Boolean ::= string_length(<String>, <Integer>)           @...Length
private Boolean ::= integer_string(<Integer>, <String>)          @...ToInteger

a + b = c     <=>  string_concat(a, b, c)
len(a) = x    <=>  string_length(a, x)
int(a) = x    <=>  integer_string(x, a)
str(x) = a    <=>  integer_string(x, a)
```

Note that `int` and `str` share the same native predicate `integer_string`, wrapping it in two directions: one asking "which string represents integer `x`?" and the other asking "which integer is parsed from string `a`?"

---

## Bidirectional behaviour

As with arithmetic, the concatenation relation works in any direction:

```
"foo" + "bar" = a      ? [(a="foobar")][..]       // forward
a + "bar" = "foobar"   ? [(a="foo")][..]          // split: solve for prefix
"foo" + a = "foobar"   ? [(a="bar")][..]          // split: solve for suffix
```

For length, the forward direction is supported, but asking "which string has length 3?" produces an unknown result — there are infinitely many:

```
len("foo") = 3   ? [()][]                         // verified fact
len("foo") = d   ? [(d=3)][..]                    // compute the length
len(a)     = 3   ? [..][..]                       // unknown — cannot enumerate
```

---

## Parsing — `int(...)`

`int` expects a digit-only string with no whitespace and no sign prefix in the current module:

```
int("123456")           = 123456    ? [()][]
int("123456")           = d         ? [(d=123456)][..]
int("0000123456")       = d         ? [(d=123456)][..]

int("    123456")       = d         ? [][..]      // leading whitespace
int("123456    ")       = d         ? [][..]      // trailing whitespace
int("NaN")              = d         ? [][..]      // not a number
int("Hello, World!")    = d         ? [][..]      // not a number
```

An unparseable input produces a closed-empty facts side with `..` on the falsehoods. The falsehoods side is open because "which `d` is *not* `int("NaN")`?" has no meaningful answer — every integer is a falsehood, and the reasoner declines to enumerate.

---

## Formatting — `str(...)`

`str` produces the canonical decimal form of an integer, with no leading zeros:

```
str(123456)     = "123456"   ? [()][]
str(0000123456) = a          ? [(a="123456")][..]
```

The second example is instructive: `0000123456` is an integer literal that equals `123456`; `str` formats it as `"123456"`, not `"0000123456"`. Leading zeros are a feature of the literal syntax, not the integer value.

---

## Exports summary

After `import nelumbo.strings`, in addition to everything from `integers` and `logic`:

- Type: `String`
- Literals: `<STRING>`
- Operators: `+` on strings
- Functions: `str(i)`, `len(s)`, `int(s)`

`string_concat`, `string_length`, and `integer_string` are `private`.

---

## See also

- [`integers.md`](integers.md) — the module `strings` builds on
- [`stringsTest.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/examples/stringsTest.nl) — executable specification
- [`deHet.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/examples/deHet.nl) — natural-language DSL using strings
- [`transformation.nl`](../../../src/main/resources/org/modelingvalue/nelumbo/examples/transformation.nl) — string-typed attributes via `::>` transformation
