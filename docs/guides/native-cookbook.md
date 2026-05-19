# Native cookbook — writing Java natives for Nelumbo

This guide is a hands-on reference for writing new Java natives bound to Nelumbo patterns with `@`. It is organised as **recipes**: concrete patterns for the common shapes of native, each with a complete working template you can adapt.

Before diving in, make sure you have read:

- [`../reference/native-api.md`](../reference/native-api.md) — the API surface (`Predicate`, `infer`, `InferResult`, the helper family `factCC` / `factCI` / etc.)
- [`../reference/native-classes.md`](../reference/native-classes.md) — the catalogue of what ships today

And before reaching for a native at all, confirm you are not on the wrong extension path. **The in-language path (rules, transformations, modules) handles most cases.** See [`writing-your-own-module.md`](writing-your-own-module.md) and [`language-transformations.md`](language-transformations.md). Natives are for genuine primitives.

---

## The two annotations

Two annotations wire a Java class to the Nelumbo engine. Both exist because reflection-driven code looks unused to static analysers — the annotations document that the constructor or field is intentional and let the engine find them deterministically.

### `@NelumboConstructor`

Marks the single public constructor that the engine calls when it parses a use of the pattern. The signature is fixed:

```java
@NelumboConstructor
public MyClass(NodeInfo nodeInfo, Object... args) {
    super(nodeInfo, args);
}
```

That is the entire constructor body for almost every native. There is no `struct(...)` method to override, no private re-structuring constructor — the base classes (`Node`, `Predicate`, `BinaryPredicate`, …) take care of restructuring automatically.

### `@NelumboFunctorField`

Marks a `static Functor FUNCTOR` field that the engine fills in by reflection during parsing. You only need this if your native's Java code needs to **construct new instances of the pattern at runtime** — typically a value type whose factory is called from another predicate (`NInteger.of(BigInteger)` called from `Add`, etc.).

```java
@NelumboFunctorField
private static Functor FUNCTOR;

public static MyValue of(MyJavaValue val) {
    return new MyValue(NodeInfo.of(FUNCTOR), val);
}
```

Predicate natives like `Add`, `Multiply`, `GreaterThan`, `Concat`, `Length`, and `ToInteger` do **not** need `@NelumboFunctorField` — they never build instances of themselves; they only build instances of the value types they yield (`NInteger`, `NString`).

---

## Table of recipes

1. [Three-arg functional relation](#recipe-1-three-arg-functional-relation) — `Add`, `Multiply`, `Concat`, `IntegersRational`, `ToInteger`
2. [Comparison predicate](#recipe-2-comparison-predicate) — `GreaterThan`, `Length`
3. [Constant / literal type](#recipe-3-constant--literal-type) — `NInteger`, `NString`, `Rational`, `NBoolean`
4. [Binary logical connective](#recipe-4-binary-logical-connective) — `And`, `Or`
5. [Container / collection literal](#recipe-5-container--collection-literal) — `NSet`, `NList`

Each recipe includes: when to use it, the skeleton class, the key decisions you must make, and pointers to the shipped implementation to study.

---

## Recipe 1 — three-arg functional relation

**Use when** you have a three-argument relation where, given any *N−1* of the arguments, the native can compute or verify the missing one. This is the workhorse shape; it covers addition, multiplication, concatenation, conversion.

The stdlib uses this shape for `Add`, `Multiply` (in both `integers` and `rationals`), `Concat`, `IntegersRational`, and `ToInteger`.

### Nelumbo-side declaration

```
private Boolean ::= myop(<T1>, <T2>, <T3>)  @com.example.MyOp
```

Declaring `myop` as `private` is idiomatic — the native predicate is usually wrapped by user-facing operators in the same module:

```
T1 a
T2 b
T3 c

a + b = c  <=>  myop(a, b, c)
a - b = c  <=>  myop(c, b, a)
```

### Java skeleton

```java
package com.example;

import java.io.Serial;

import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.NodeInfo;
import org.modelingvalue.nelumbo.logic.InferContext;
import org.modelingvalue.nelumbo.logic.InferResult;
import org.modelingvalue.nelumbo.logic.Predicate;

public final class MyOp extends Predicate {
    @Serial
    private static final long serialVersionUID = 1L;

    @NelumboConstructor
    public MyOp(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    @Override
    protected InferResult infer(int nrOfUnbound, InferContext context) {
        if (nrOfUnbound > 1) {
            return unresolvable();
        }

        // getVal(i, j) reads sub-value j of argument i; for simple scalars j=0.
        MyT1Value a = getVal(0, 0);   // null if unbound
        MyT2Value b = getVal(1, 0);
        MyT3Value c = getVal(2, 0);

        if (a != null && b != null) {
            // Both inputs bound — compute the expected output.
            MyT3Value computed = /* compute a op b */ ;
            if (c != null) {
                // Output also bound — verify.
                return c.equals(computed) ? factCC() : falsehoodCC();
            } else {
                // Bind the output.
                return set(2, MyT3Native.of(computed)).factCI();
            }
        } else if (a != null && c != null) {
            // Compute b from a and c, if possible.
            MyT2Value computedB = /* compute inverse */ ;
            return set(1, MyT2Native.of(computedB)).factCI();
            // Or, if no b works: return falsehoodCI();
        } else if (b != null && c != null) {
            MyT1Value computedA = /* compute inverse */ ;
            return set(0, MyT1Native.of(computedA)).factCI();
        }

        return unknown();
    }
}
```

### Key decisions

1. **What does "bound" mean for each argument?** `getVal(i, 0)` returns `null` if argument `i` is unbound. Use `null` checks to drive the branch structure.
2. **Is the relation total in every direction?** `Add` is (every inverse exists). `Multiply` is not — division by zero or a non-divisor produces `falsehoodCI()`.
3. **Which closure flags should you use?** Most three-way natives use `factCI()` on a successful computation (the facts side is closed on the computed tuple, but we make no claim about falsehoods). They use `factCC()` / `falsehoodCC()` only when *all three* are bound — then the result is fully determined.
4. **What if multiple arguments are unbound?** Return `unresolvable()`. The engine will retry once other parts of the reasoning have bound more variables.

### Study the source

- `org.modelingvalue.nelumbo.integers.Add` — cleanest example, ~15 lines of real code
- `org.modelingvalue.nelumbo.integers.Multiply` — shows the "falsehood when no answer exists" pattern (division remainder ≠ 0)
- `org.modelingvalue.nelumbo.strings.Concat` — shows "infer an operand via suffix/prefix matching"

---

## Recipe 2 — comparison predicate

**Use when** you have a two-argument Boolean relation that, when both sides are bound, produces `true` or `false`, but cannot always bind a missing side.

The stdlib uses this shape for `GreaterThan` (integers and rationals) and `Length`. `Equal` and `NIs` are richer variants for equality.

### Nelumbo-side declaration

```
Boolean ::= <T> ">" <T>  #30  @com.example.MyCompare
```

### Java skeleton

```java
public final class MyCompare extends Predicate {
    @Serial
    private static final long serialVersionUID = 1L;

    @NelumboConstructor
    public MyCompare(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    @Override
    protected InferResult infer(int nrOfUnbound, InferContext context) {
        if (nrOfUnbound > 1) {
            return unresolvable();
        }

        MyT left  = getVal(0, 0);
        MyT right = getVal(1, 0);

        if (left == null) {
            // Right is bound; contribute the partial falsehood "x relation x" is false.
            return set(0, get(1)).falsehoodsII();
        }
        if (right == null) {
            return set(1, get(0)).falsehoodsII();
        }
        // Both bound — decisive.
        return compare(left, right) ? factCC() : falsehoodCC();
    }

    private boolean compare(MyT l, MyT r) {
        return /* your comparison */ ;
    }
}
```

### Key decisions

1. **What partial information can you contribute when one side is unbound?** `GreaterThan` uses `set(i, get(1-i)).falsehoodsII()` — it knows `x > x` is false for any `x`, so it contributes that falsehood with both sides open. Other comparisons can do similar things.
2. **When both sides are bound, is the result always decisive?** For `>`, yes — `factCC()` or `falsehoodCC()`. For a comparison that can legitimately produce "unknown" (e.g. partial order), use `unknown()` for the unknown case.

### Study the source

- `org.modelingvalue.nelumbo.integers.GreaterThan` — simplest form
- `org.modelingvalue.nelumbo.rationals.GreaterThan` — same strategy, cross-multiply internally
- `org.modelingvalue.nelumbo.strings.Length` — two-arg predicate that uses `factCI()` / `falsehoodCC()` for specific cases

---

## Recipe 3 — constant / literal type

**Use when** you are adding a new **value type** whose instances are produced by parsing a source-level literal.

The stdlib uses this shape for `NInteger` (integers), `NString` (strings), `Rational` (rationals), and — less typically — `NBoolean` (Booleans).

### Nelumbo-side declaration

```
MyValue :: Object

MyValue ::= <NUMBER>   @com.example.NMyValue
```

(Or `<STRING>`, `<DECIMAL>`, or a custom lexical token.)

### Java skeleton

```java
public final class NMyValue extends Node {
    @Serial
    private static final long serialVersionUID = 1L;

    @NelumboFunctorField
    private static Functor FUNCTOR;

    public static NMyValue of(MyJavaValue val) {
        return new NMyValue(NodeInfo.of(FUNCTOR), val);
    }

    @NelumboConstructor
    public NMyValue(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    private static MyJavaValue parse(String source) {
        return /* parse source into MyJavaValue */ ;
    }

    public MyJavaValue value() {
        return (MyJavaValue) get(0);
    }

    @Override
    public String toString(TokenType[] previous) {
        previous[0] = TokenType.NAME;  // or NUMBER / DECIMAL, etc.
        return value().toString();
    }

    @Override
    public Node init(KnowledgeBase knowledgeBase, ParseContext ctx, ConstructionReason reason)
            throws ParseException {
        if (reason == ConstructionReason.parsing && get(0) instanceof String string) {
            return set(0, parse(string));
        }
        return this;
    }
}
```

### Key decisions

1. **Extend `Node`, not `Predicate`.** A constant/literal is a value, not a Boolean. `Predicate` is for things that participate in fact/falsehood reasoning.
2. **Parse in `init(...)`, not in the constructor.** During parsing, the engine constructs the value with the raw source text as `args[0]`. The `init` callback fires once the construction reason is `parsing`; replace the string with the parsed Java value via `set(0, parsed)` and return the updated node.
3. **Use `@NelumboFunctorField` for the static `FUNCTOR`.** No static block, no `registerFunctorSetter`. The engine fills the field by reflection.
4. **The `of(...)` factory** uses `NodeInfo.of(FUNCTOR)` to construct instances at runtime. Other natives (like `Add`) call it to build results.
5. **`toString(TokenType[])`** controls printing of query results. Set `previous[0]` to the token type your literal ends with (usually `NUMBER` for numeric literals, `NAME` for identifier-like values), so adjacent tokens print with appropriate spacing. Inspect `previous[0]` *on entry* to decide whether to emit a leading space.

### Study the source

- `org.modelingvalue.nelumbo.integers.NInteger` — clean example, includes base-N parsing
- `org.modelingvalue.nelumbo.strings.NString` — simplest possible (just strip the surrounding quotes)
- `org.modelingvalue.nelumbo.rationals.Rational` — two-value node (numerator, denominator) with `gcd` normalisation; parses in `init` and uses `set(nodeInfo().resetDeclaration(), parse(string))` because the parse produces two values

---

## Recipe 4 — binary logical connective

**Use when** you are adding a new Boolean connective that combines two sub-predicates according to a truth table.

The stdlib uses this shape for `And` and `Or`. Writing one from scratch is uncommon; you are extending the `nelumbo.logic` module. But the recipe is documented here for completeness.

### Nelumbo-side declaration

```
Boolean ::= <Boolean> "nand" <Boolean>  #21  @com.example.Nand
```

### Java skeleton

`BinaryPredicate` factors out all the short-circuiting and reduction logic. You only need to provide the truth-table overrides:

```java
public final class Nand extends BinaryPredicate {
    @Serial
    private static final long serialVersionUID = 1L;

    @NelumboFunctorField
    private static Functor FUNCTOR;

    @NelumboConstructor
    public Nand(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    public static Nand of(Predicate p1, Predicate p2) {
        return new Nand(NodeInfo.of(FUNCTOR), p1, p2);
    }

    @Override
    public Nand declaration() {
        return (Nand) super.declaration();
    }

    // --- truth table after seeing ONE operand ---

    @Override
    protected boolean isTrue(InferResult predResult, int i) {
        // NAND is definitively TRUE after seeing one operand iff that operand is FALSE.
        // NAND(false, x) = true; NAND(x, false) = true.
        return predResult.isFalseCC();
    }

    @Override
    protected boolean isFalse(InferResult predResult, int i) {
        // NAND is never definitively FALSE from a single operand — needs both.
        return false;
    }

    @Override
    protected boolean isUnknown(InferResult predResult, int i) {
        return false;
    }

    // --- truth table after seeing BOTH operands ---

    @Override
    protected boolean isTrue(InferResult[] predResult) {
        return predResult[0].isFalseCC() || predResult[1].isFalseCC();
    }

    @Override
    protected boolean isFalse(InferResult[] predResult) {
        return predResult[0].isTrueCC() && predResult[1].isTrueCC();
    }

    // --- reduction (optional optimisations) ---

    @Override
    protected boolean isLeft(InferResult[] predResult)  { return false; }

    @Override
    protected boolean isRight(InferResult[] predResult) { return false; }
}
```

### Key decisions

1. **Extend `BinaryPredicate`, not `Predicate`.** The base class evaluates both sub-predicates, handles stack-overflow markers, and dispatches to the truth-table predicates you override.
2. **Seven overrides total:** `isTrue` / `isFalse` / `isUnknown` for the *single-operand* case (called after each operand is evaluated; allows short-circuiting), `isTrue` / `isFalse` for the *both-operands* case, and `isLeft` / `isRight` for reduction.
3. **`isLeft` / `isRight`** are reduction optimisations. `And` returns `isLeft` when the right operand is `true` (so `p & true` reduces to `p`); `Or` returns `isLeft` when the right operand is `false` (so `p | false` reduces to `p`). For most connectives these can return `false` and rely on the general case.
4. **Use `@NelumboFunctorField`** if you provide an `of(...)` factory (the `And.of(p1, p2)` family). The base class itself does not require it.

### Study the source

- `org.modelingvalue.nelumbo.logic.And` and `.Or` — both are short and mirror each other
- `org.modelingvalue.nelumbo.logic.BinaryPredicate` — the base class, to understand what your overrides feed into

---

## Recipe 5 — container / collection literal

**Use when** you are adding a new collection type with a literal syntax.

The stdlib uses this shape for `NSet` (`{...}`) and `NList` (`[...]`).

### Nelumbo-side declaration

```
Type E

MyContainer<E> ::= < <(> <E> <,> , <)*> >  @com.example.NMyContainer
```

### Java skeleton

```java
public class NMyContainer extends Node {
    @Serial
    private static final long serialVersionUID = 1L;

    @NelumboConstructor
    public NMyContainer(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    @Override
    protected NMyContainer set(NodeInfo nodeInfo, Object[] args) {
        return new NMyContainer(nodeInfo, args);
    }

    public Type elementType() {
        return type().element();
    }

    @SuppressWarnings("unchecked")
    public <T> MyInternalColl<T> elements() {
        return (MyInternalColl<T>) get(0);
    }

    @Override
    public List<Object> args() {
        return elements().asList();
    }

    @Override
    public NMyContainer set(int i, Object... a) {
        return (NMyContainer) super.set(i, a);
    }

    @Override
    public String toString(TokenType[] previous) {
        return /* render your collection */ ;
    }

    @Override
    public Node init(KnowledgeBase knowledgeBase, ParseContext ctx, ConstructionReason reason)
            throws ParseException {
        if (reason != ConstructionReason.parsing
                || (length() > 0 && get(0) instanceof MyInternalColl)) {
            return this;
        }
        // Wrap the children that the parser delivered into the internal collection.
        return setArgs(new Object[] { MyInternalColl.of(super.args()) });
    }
}
```

### Key decisions

1. **Extend `Node`, not `Predicate`.** A container is a value.
2. **Wrap into the internal collection in `init(...)`, not the constructor.** At parse time the engine hands you the children as individual args; `init` is where you collapse them into a single `Set` / `List` and stash it as `args[0]`. After that initial wrapping, subsequent reconstructions (when `length() > 0 && get(0) instanceof MyInternalColl`) leave the node alone.
3. **Override `set(NodeInfo, Object[])`** if you need to control how the engine rebuilds the node during reasoning. `NSet` does this; the new instance is what the engine uses going forward.
4. **`args()` override** tells the engine what the container's children are for traversal and binding purposes. For `NList`, `args()` returns the list in order. For `NSet`, it returns the set's elements in some canonical order (the shipped implementation uses `asList()`).
5. **`toString(TokenType[])`** should emit the user-facing syntax (e.g. `{a, b, c}` for a set, `[a, b, c]` for a list).

### Study the source

- `org.modelingvalue.nelumbo.collections.NSet` — simplest form
- `org.modelingvalue.nelumbo.collections.NList` — includes an `elementsFlattened()` helper for nested list flattening

---

## General checklist before shipping a native

Before you consider a new native "done":

- [ ] The class is `final` unless you expect it to be further subclassed (`BinaryPredicate`, `CompoundPredicate`, and `Predicate` itself are non-final because subclasses extend them).
- [ ] There is exactly one `@NelumboConstructor` and it has signature `(NodeInfo, Object...)`. No other public constructors.
- [ ] The `serialVersionUID` is set (the engine serialises nodes during deep reasoning).
- [ ] If your native constructs instances of itself at runtime — e.g., a value type with an `of(...)` factory — there is a `@NelumboFunctorField private static Functor FUNCTOR;`. No static `registerFunctorSetter` block is needed; the engine populates the field by reflection.
- [ ] For Predicate-style natives: `infer(int nrOfUnbound, InferContext context)` handles every combination of bound/unbound arguments and returns `unresolvable()` when it cannot proceed.
- [ ] For literal-type natives: `init(KnowledgeBase, ParseContext, ConstructionReason)` parses the raw `String` from `args[0]` exactly once, gated on `reason == ConstructionReason.parsing`. `toString(TokenType[])` is implemented for human-readable result printing.
- [ ] For container natives: `init(...)` wraps the parsed children into the internal collection, gated on the children not already being wrapped. `args()` is overridden to return the contained elements.
- [ ] For `BinaryPredicate` subclasses: all seven truth-table / reduction overrides are present (`isTrue`/`isFalse`/`isUnknown` for one operand; `isTrue`/`isFalse` for both; `isLeft`/`isRight` for reduction).
- [ ] Closure flags are chosen deliberately. `factCC` / `falsehoodCC` only when the result is fully determined. `factCI` / `falsehoodCI` when you've made a single-point claim but don't know about the other side. `unknown()` when you truly cannot decide.
- [ ] The class is on the classpath of any Nelumbo program that imports a module referencing it via `@`.
- [ ] You have a stdlib-style test file — similar to `integersTest.nl` — that exercises your native in every direction (all bound, each single-unbound case, pathological inputs).

---

## Common pitfalls

**Wrong constructor signature.** The `@NelumboConstructor` constructor must be `(NodeInfo, Object...)`. The older signature `(Functor, List<AstElement>, Object[])` is no longer accepted — the `@NelumboConstructor.Finder` checks the parameter types at class-load time and reports the mismatch.

**Forgetting `@NelumboFunctorField` on the static field.** A bare `private static Functor FUNCTOR;` will remain `null` because the engine looks for the annotation, not for a field named `FUNCTOR`. Calls to your `of(...)` factory will then build a `NodeInfo` with a null functor and fail at runtime.

**Parsing in the constructor instead of in `init(...)`.** During parsing, the constructor receives the raw source `String` as `args[0]`; if you try to cast it to `BigInteger` (or call `parse()` on it inside `super(...)`) you'll either crash or silently corrupt the node when the engine later restructures it during reasoning. Always parse in `init`, gated on `reason == ConstructionReason.parsing`.

**Returning `factCC()` when you shouldn't.** `factCC` asserts both sides are complete — facts side exactly `{this}` and falsehoods side exactly `{}`. If the facts side is really just "this one tuple, but there could be others I haven't enumerated," use `factCI` instead. Getting this wrong makes downstream rules unsound (they will derive false contradictions).

**Forgetting `unresolvable()` for the too-many-unbound case.** Returning anything else (`unknown()`, `factCI()` with garbage, etc.) when multiple arguments are unbound leaks confusion into the reasoner. `unresolvable()` is the honest answer: "come back later."

**Constructing native values without `of(...)`.** Calling a value-type's public constructor directly with `null` for the `NodeInfo` skips the functor wiring and will fail at runtime. Always go through the static `of(...)` factory, which uses `NodeInfo.of(FUNCTOR)`.

**Truth tables that don't handle `unknown`.** In `BinaryPredicate`, all the `isTrue` / `isFalse` / `isUnknown` overrides should consider what happens when one operand is unknown. For conjunction-like operators, `unknown & false = false` is non-obvious. Test the `unknown` rows of your truth table against a `logicTest.nl`-style file.

**Not mirroring the existing stdlib patterns.** If you are adding `mod` to integers, model it on `Multiply`. If you are adding a new comparison, model it on `GreaterThan`. The stdlib has already worked out the right shapes — follow them.

---

## See also

- [`../reference/native-api.md`](../reference/native-api.md) — full API surface reference
- [`../reference/native-classes.md`](../reference/native-classes.md) — catalogue of shipped natives
- [`../explanation/architecture.md`](../explanation/architecture.md) — why Java/Nelumbo is split where it is
- [`writing-your-own-module.md`](writing-your-own-module.md) — the in-language alternative
- [`language-transformations.md`](language-transformations.md) — the meta-level alternative
