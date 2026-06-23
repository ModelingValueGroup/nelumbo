# Native API — implementing predicates in Java

Some patterns in Nelumbo cannot be expressed purely in the language — the basic arithmetic primitives, string concatenation, rational construction. These are implemented in Java and bound to a pattern declaration using the `@` annotation:

```
private Boolean ::= add(<Integer>, <Integer>, <Integer>)   @org.modelingvalue.nelumbo.integers.Integers
```

This page describes how such bindings are written. It is the reference for anyone extending Nelumbo with new primitives — either to add capabilities to the standard library or to integrate Nelumbo with a Java application.

For the in-language extension mechanism (`::>` pattern transformations), see [`../guides/language-transformations.md`](../guides/language-transformations.md) instead. That is usually the right tool; Java natives are only needed for primitives that fundamentally cannot be derived from existing rules.

---

## Anatomy of a native binding

The form of a pattern with a native binding is:

```
<Type> ::= pattern   @fully.qualified.JavaClassName
```

The Java class named by `@` must:

1. Extend an appropriate base class — typically `org.modelingvalue.nelumbo.logic.Predicate`.
2. Provide a single public constructor annotated with `@NelumboConstructor`, with the signature `(NodeInfo, Object...)`. The engine instantiates the class through this constructor by reflection.
3. Implement the native reasoning. **The preferred way is a `@NelumboMethod`** — a method whose name and parameter count match the functor; the engine binds it by reflection and one class can host a method per relation. Override `infer(int, InferContext)` instead when the functor is an operator (its name is not a Java identifier) or when the logic needs the `InferContext`. (Other base-class hooks exist too: `init(...)` for parsed literals, the `isTrue` / `isFalse` family for `BinaryPredicate` subclasses.) The trade-off is spelled out in [`../guides/native-cookbook.md`](../guides/native-cookbook.md#implementing-the-logic-prefer-nelumbomethod-over-overriding-infer); everything below about `InferResult` applies equally to both.

That is the entire required surface. There is no `struct(...)` override and no private re-structuring constructor; the base classes handle re-structuring automatically.

If the native needs to construct instances of itself at runtime (a typical literal-type concern — e.g., `Integers` building `NInteger`s from `BigInteger` results), declare a static `Functor FUNCTOR` field marked with `@NelumboFunctorField`. The engine populates it by reflection during parsing, and your `of(...)` factory can then build instances via `NodeInfo.of(FUNCTOR)`. There is no static block, no `registerFunctorSetter` call.

```java
@NelumboFunctorField
private static Functor FUNCTOR;

public static NMyValue of(MyJavaValue val) {
    return new NMyValue(NodeInfo.of(FUNCTOR), val);
}
```

`@NelumboConstructor` enforces the `(NodeInfo, Object...)` signature; `@NelumboFunctorField` enforces a `static Functor` field. Both annotations exist purely to (a) document that the constructor or field is wired by reflection (it would otherwise look unused) and (b) let the engine find them deterministically.

---

## Where the logic goes: `@NelumboMethod` and `infer`

The reasoning of a native predicate returns an **`InferResult`** — the same fact/falsehood/completeness structure described in [`test-expression-semantics.md`](test-expression-semantics.md), but at the Java level. It lives in one of two places.

**A `@NelumboMethod` (preferred).** The method name equals the functor's name and the parameter count equals its argument count; the engine binds it by reflection. Each bound argument arrives as its typed node and each unbound argument as `null`:

```java
@NelumboMethod
protected InferResult add(NInteger addend1, NInteger addend2, NInteger sum);
```

**An `infer` override.** Used for operator functors and for natives that need the reasoning context:

```java
protected InferResult infer(int nrOfUnbound, InferContext context);
```

- `nrOfUnbound` is the number of the predicate's arguments that are still unbound variables at the call site (in a `@NelumboMethod`, call the `nrOfUnbound()` accessor instead)
- `context` provides access to the current reasoning context (used rarely)
- read arguments with `getVal(i, 0)` (returns `null` when argument `i` is unbound), since there are no typed parameters

Either way, a typical implementation inspects the currently bound arguments, decides what result it can justify, and returns the appropriate `InferResult`. The rest of this page applies to both forms.

---

## Building an `InferResult`

`Predicate` provides helpers on `this` for constructing the common result shapes. The two-letter suffixes on the helper names encode the **completeness flags** of the two sides: `C` = complete (closed), `I` = incomplete (open). First letter is facts-side completeness, second is falsehoods-side.

| Helper | Facts | Falsehoods | Meaning |
|---|---|---|---|
| `factCC()`      | `{this}` complete | `{}` complete   | proven true, nothing else possible |
| `falsehoodCC()` | `{}` complete     | `{this}` complete | proven false, nothing else possible |
| `factCI()`      | `{this}` complete | `{}` incomplete  | proven true; falsehoods side not enumerated |
| `factIC()`      | `{this}` incomplete | `{}` complete  | proven true, but more facts may exist |
| `falsehoodCI()` | `{}` complete    | `{this}` incomplete | falsehood with open falsehoods side |
| `falsehoodIC()` | `{}` incomplete  | `{this}` complete   | falsehood with open facts side |
| `falsehoodsII()` | `{}` incomplete | `{this}` incomplete | a falsehood observed; both sides open |
| `unknown()`     | `{}` incomplete  | `{}` incomplete  | no claim either way |
| `unresolvable()` |  —  |  —  | cannot resolve with current bindings; engine should retry later |

The `set(i, v)` family of helpers constructs a new predicate with argument `i` set to value `v`. This is how a native predicate can **bind a variable** — the result carries the completed tuple.

---

## Worked example — `add`

The integer addition primitive, bound to the pattern `add(<Integer>, <Integer>, <Integer>)`. This is the `add` `@NelumboMethod` on `org.modelingvalue.nelumbo.integers.Integers` (which also hosts `mult` and `gt`):

```java
@NelumboMethod
protected InferResult add(NInteger addend1, NInteger addend2, NInteger sum) {
    if (nrOfUnbound() > 1) {
        return unresolvable();
    }
    BigInteger a1 = addend1 == null ? null : addend1.value();
    BigInteger a2 = addend2 == null ? null : addend2.value();
    BigInteger s  = sum     == null ? null : sum.value();
    if (a1 != null && a2 != null) {
        BigInteger r = a1.add(a2);
        if (s != null) {
            return r.equals(s) ? factCC() : falsehoodCC();
        }
        return set(2, NInteger.of(r)).factCI();
    } else if (a1 != null && s != null) {
        return set(1, NInteger.of(s.subtract(a1))).factCI();
    } else if (a2 != null && s != null) {
        return set(0, NInteger.of(s.subtract(a2))).factCI();
    }
    return unknown();
}
```

Reading this:

- The method name `add` and its three parameters match the functor `add(<Integer>,<Integer>,<Integer>)`. No functor field is needed because `Integers` never constructs an `Integers` instance — only `NInteger`s, which carry their own `@NelumboFunctorField`.
- Each bound argument is its typed `NInteger`; each unbound argument is `null`. `addend.value()` reads the underlying `BigInteger`.
- If at least two arguments are unbound, the predicate cannot do anything useful — it returns `unresolvable()` so the engine can retry once more bindings are known.
- If all three are bound, the predicate checks whether the sum is correct and returns `factCC()` (proven true, closed on both sides) or `falsehoodCC()` (proven false, closed on both sides).
- If two are bound and one is not, the predicate computes the missing value and returns `set(i, computedValue).factCI()` — the facts side contains the completed tuple, closed; the falsehoods side is left open because the native is not claiming any particular falsehoods.

This is what lets `add(a, b, c)` be used relationally: the same Java method answers *all six combinations* of bound/unbound inputs, as long as at most one is unbound.

---

## Worked example — `gt` (comparison)

A comparison primitive showing the "enumerate falsehoods" pattern. This is the `gt` `@NelumboMethod` on `org.modelingvalue.nelumbo.integers.Integers`. Because `>` is an operator (its functor name is not an identifier), `integers.nl` exposes the comparison through a named helper functor and a rule — `gt(<Integer>,<Integer>) @…Integers` together with `a>b <=> gt(a,b)` — which lets the logic stay in a method:

```java
@NelumboMethod
protected InferResult gt(NInteger left, NInteger right) {
    if (nrOfUnbound() > 1) {
        return unresolvable();
    }
    if (left == null) {
        return set(0, get(1)).falsehoodsII();
    }
    if (right == null) {
        return set(1, get(0)).falsehoodsII();
    }
    return left.value().compareTo(right.value()) > 0 ? factCC() : falsehoodCC();
}
```

When both sides are bound, the comparison is decisive — `factCC()` or `falsehoodCC()`. When one side is unbound, the native contributes a specific falsehood (`l > l` is false for any `l`, so setting the unbound side to the other side's value yields a falsehood) but leaves both sides open (`II`) — there are many more facts and falsehoods, and the native does not enumerate them. The engine uses this partial information as one building block alongside others.

The alternative is to bind a class straight to the `>` operator and override `infer` — `org.modelingvalue.nelumbo.datetime.GreaterThan` does exactly that, with the same body shape but reading arguments via `getVal(i, 0)`.

---

## Base classes

| Base class | Use when |
|---|---|
| `Predicate` (in `org.modelingvalue.nelumbo.logic`) | The pattern produces `Boolean` (a predicate/relation) |
| `Function` (also in `logic`)                     | The pattern produces a non-Boolean value (a function) |
| `NConstant` and siblings under `integers`, `rationals`, `strings` | Specialised bases for literal-producing patterns |

For most new predicates, extend `Predicate`. For patterns whose result is an `Integer`, `Rational`, `String`, or your own `Object` subtype that is computed (not merely a literal), extend `Function`.

Study the stdlib implementations for patterns close to what you want:

- `Predicate` subclass in `integers/`: `Integers` (the `add`, `mult`, `gt` `@NelumboMethod`s)
- Value type in `integers/`: `NInteger` (literal)
- Similar families in `rationals/` (`Rationals`, `Rational`), `strings/` (`Concat`, `Length`, `ToInteger` — `infer` overrides), and `collections/` (`Collections`)

---

## Registering natives with the engine

At runtime, the engine loads native classes by name from the `@` annotation on the pattern declaration. As long as the class is on the classpath when the Nelumbo program is parsed, it will be found and instantiated. There is no separate registration step.

For tests that live alongside Nelumbo's own build, placing the Java file in the appropriate package and ensuring it is compiled is enough. For external projects integrating Nelumbo as a library, add your native classes to your application's classpath; `@` references to them will resolve normally.

---

## Hooking into the engine from Java

Three classes in the root `org.modelingvalue.nelumbo` package are the main integration surface:

- `KnowledgeBase` — holds the loaded declarations and facts; you get one per Nelumbo program
- `Query` — construct and run queries against a knowledge base
- `InferResult` — the result shape documented above

If you are building a Java application that uses Nelumbo for rules (rather than extending Nelumbo itself), these are the classes you drive. Full details are in the Javadoc of those classes.

---

## When not to write a native

Writing a native is a significant step. Before reaching for it, check whether the behaviour you want can be expressed as:

- A Nelumbo rule (`<=>`) — most derived behaviour belongs here
- A combination of existing stdlib operators
- A `::>` pattern transformation (see [`../guides/language-transformations.md`](../guides/language-transformations.md))

Native code should be reserved for:

- **Primitive operations** the language cannot derive — arithmetic, comparison, string manipulation, I/O
- **Performance-critical paths** where a rule-based implementation is too slow
- **Integration with external Java libraries**

The stdlib itself follows this rule: `add`, `mult`, `string_concat`, and the comparison predicates are native; subtraction, division, concatenation-with-padding, absolute value, and all the `<=`/`>=`/etc. derivatives are defined in Nelumbo on top of them.

---

## See also

- [`grammar.md`](grammar.md) — where `@` fits syntactically
- [`writing-rules.md`](writing-rules.md) — the in-language alternative
- [`../guides/language-transformations.md`](../guides/language-transformations.md) — the meta-level alternative (Phase 3)
- [`stdlib/integers.md`](stdlib/integers.md) — a concrete module with three native predicates and several Nelumbo-defined ones
- [`stdlib/logic.md`](stdlib/logic.md) — see how `->`, `<->`, and `!=` are in-language, while `!`, `&`, `|`, `E[]`, `A[]`, `NIs` (for `=`), and `Equal` (for the private `eq`) are native
