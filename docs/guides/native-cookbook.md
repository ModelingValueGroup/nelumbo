# Native cookbook — writing Java natives for Nelumbo

This guide is a hands-on reference for writing new Java natives bound to Nelumbo patterns with `@`. It is organised as **recipes**: concrete patterns for the common shapes of native, each with a complete working template you can adapt.

Before diving in, make sure you have read:

- [`../reference/native-api.md`](../reference/native-api.md) — the API surface (`Predicate`, `infer`, `InferResult`, the helper family `factCC`/`factCI`/etc.)
- [`../reference/native-classes.md`](../reference/native-classes.md) — the catalogue of what ships today

And before reaching for a native at all, confirm you are not on the wrong extension path. **The in-language path (rules, transformations, modules) handles most cases.** See [`writing-your-own-module.md`](writing-your-own-module.md) and [`language-transformations.md`](language-transformations.md). Natives are for genuine primitives.

---

## Table of recipes

1. [Three-arg functional relation](#recipe-1-three-arg-functional-relation) — `Add`, `Multiply`, `Concat`, `IntegersRational`, `ToInteger`
2. [Comparison predicate](#recipe-2-comparison-predicate) — `GreaterThan`, `Length`
3. [Constant / literal type](#recipe-3-constant-literal-type) — `NInteger`, `NString`, `Rational`, `NBoolean`
4. [Binary logical connective](#recipe-4-binary-logical-connective) — `And`, `Or`
5. [Container / collection literal](#recipe-5-container-collection-literal) — `NSet`, `NList`

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
import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.InferContext;
import org.modelingvalue.nelumbo.InferResult;
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.logic.Predicate;
import org.modelingvalue.nelumbo.patterns.Functor;

public final class MyOp extends Predicate {
    @Serial
    private static final long serialVersionUID = 1L;

    @NelumboConstructor
    public MyOp(Functor functor, List<AstElement> elements, Object[] args) {
        super(functor, elements, args[0], args[1], args[2]);
    }

    private MyOp(Object[] array, List<AstElement> elements, MyOp declaration) {
        super(array, elements, declaration);
    }

    @Override
    protected MyOp struct(Object[] array, List<AstElement> elements, Node declaration) {
        return new MyOp(array, elements, (MyOp) declaration);
    }

    @Override
    protected InferResult infer(int nrOfUnbound, InferContext context) {
        if (nrOfUnbound > 1) {
            return unresolvable();
        }

        // Read values from the bound arguments.
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
            // Compute a from b and c.
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

- `org.modelingvalue.nelumbo.integers.Add` — cleanest example
- `org.modelingvalue.nelumbo.integers.Multiply` — shows the "falsehood when no answer exists" pattern (division remainder ≠ 0)
- `org.modelingvalue.nelumbo.strings.Concat` — shows "infer an operand via suffix/prefix matching"

---

## Recipe 2 — comparison predicate

**Use when** you have a two-argument Boolean relation that, when both sides are bound, produces `true` or `false`, but cannot always bind a missing side.

The stdlib uses this shape for `GreaterThan` (integers and rationals) and `Length`. `Equal` is a richer variant covered below.

### Nelumbo-side declaration

```
Boolean ::= <T> ">" <T>  #30  @com.example.MyCompare
```

(With whatever operator, precedence, and types make sense for your comparison.)

### Java skeleton

```java
public final class MyCompare extends Predicate {
    @Serial private static final long serialVersionUID = 1L;

    @NelumboConstructor
    public MyCompare(Functor functor, List<AstElement> elements, Object[] args) {
        super(functor, elements, args[0], args[1]);
    }

    private MyCompare(Object[] array, List<AstElement> elements, MyCompare declaration) {
        super(array, elements, declaration);
    }

    @Override
    protected MyCompare struct(Object[] array, List<AstElement> elements, Node declaration) {
        return new MyCompare(array, elements, (MyCompare) declaration);
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
- `org.modelingvalue.nelumbo.strings.Length` — two-arg predicate that uses `factCI()`/`falsehoodCC()` for specific cases

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
    @Serial private static final long serialVersionUID = 1L;

    private static Functor FUNCTOR;

    static {
        KnowledgeBase.registerFunctorSetter(NMyValue.class, f -> FUNCTOR = f);
    }

    @NelumboConstructor
    public NMyValue(Functor functor, List<AstElement> elements, Object[] args) {
        super(functor, elements, parse((String) args[0]));
    }

    private NMyValue(Functor functor, List<AstElement> elements, MyJavaValue val) {
        super(functor, elements, val);
    }

    public static NMyValue of(MyJavaValue val) {
        return new NMyValue(FUNCTOR, List.of(), val);
    }

    private static MyJavaValue parse(String source) {
        return /* parse source into MyJavaValue */ ;
    }

    private NMyValue(Object[] array, List<AstElement> elements, NMyValue declaration) {
        super(array, elements, declaration);
    }

    @Override
    protected NMyValue struct(Object[] array, List<AstElement> elements, Node declaration) {
        return new NMyValue(array, elements, (NMyValue) declaration);
    }

    @Override
    public NMyValue set(int i, Object... a) {
        return (NMyValue) super.set(i, a);
    }

    public MyJavaValue value() {
        return (MyJavaValue) get(0);
    }

    @Override
    public String toString(TokenType[] previous) {
        // Return the textual rendering. Use `previous[0]` to decide whether
        // to insert a leading space to separate adjacent tokens.
        previous[0] = TokenType.NAME;  // or NUMBER, DECIMAL, etc.
        return value().toString();
    }
}
```

### Key decisions

1. **Extend `Node`, not `Predicate`.** A constant/literal is a value, not a Boolean. `Predicate` is for things that participate in fact/falsehood reasoning.
2. **Provide a `FUNCTOR` static and a `registerFunctorSetter` block.** This allows other natives (like `Add`) to construct instances of your type at runtime via `NMyValue.of(value)`.
3. **The `parse` method** turns the raw source text (still the user's literal, e.g. `"36#abc"`) into the internal Java representation. Handle invalid input defensively.
4. **`toString(TokenType[])`** controls printing of query results. Set `previous[0]` to the token type your literal begins with (usually `NUMBER` for numeric literals, `NAME` for identifier-like values), so adjacent tokens print with appropriate spacing.

### Study the source

- `org.modelingvalue.nelumbo.integers.NInteger` — clean example, includes base-N parsing
- `org.modelingvalue.nelumbo.strings.NString` — simplest possible (just strip quotes)
- `org.modelingvalue.nelumbo.rationals.Rational` — two-value node (numerator, denominator) with `gcd` normalisation

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
    @Serial private static final long serialVersionUID = 1L;
    private static Functor FUNCTOR;

    static {
        KnowledgeBase.registerFunctorSetter(Nand.class, f -> FUNCTOR = f);
    }

    @NelumboConstructor
    public Nand(Functor functor, List<AstElement> elements, Object[] args) {
        super(functor, elements, args[0], args[1]);
    }

    private Nand(Object[] args, List<AstElement> elements, Nand declaration) {
        super(args, elements, declaration);
    }

    @Override
    protected Nand struct(Object[] array, List<AstElement> elements, Node declaration) {
        return new Nand(array, elements, (Nand) declaration);
    }

    // Truth-table short-circuits:

    @Override
    protected boolean isTrue(InferResult predResult, int i) {
        // Is NAND definitively TRUE after seeing one operand?
        // NAND(x, false) = true, NAND(false, x) = true
        return predResult.isFalseCC();
    }

    @Override
    protected boolean isFalse(InferResult predResult, int i) {
        // Is NAND definitively FALSE after seeing one operand?
        // Only when both are true; can't know from one.
        return false;
    }

    @Override
    protected boolean isUnknown(InferResult predResult, int i) { return false; }

    @Override
    protected boolean isTrue(InferResult[] predResult) {
        // After both operands evaluated.
        // NAND is true when at least one is false.
        return predResult[0].isFalseCC() || predResult[1].isFalseCC();
    }

    @Override
    protected boolean isFalse(InferResult[] predResult) {
        return predResult[0].isTrueCC() && predResult[1].isTrueCC();
    }

    @Override
    protected boolean isLeft(InferResult[] predResult)  { return false; }

    @Override
    protected boolean isRight(InferResult[] predResult) { return false; }
}
```

### Key decisions

1. **Extend `BinaryPredicate`, not `Predicate`.** The base class does the work of evaluating both sub-predicates, handling stack-overflow markers, and dispatching to the truth-table predicates you override.
2. **`isLeft`/`isRight`** are reduction optimisations. `And` returns `isLeft` when the right operand is `true` (so `p & true` reduces to `p`); `Or` returns `isLeft` when the right operand is `false` (so `p | false` reduces to `p`). For most connectives these can return `false` and rely on the general case.
3. **Order matters less than it looks.** `BinaryPredicate.order(...)` reorders operands for evaluation to prefer `NBoolean` literals first. Your `isTrue`/`isFalse` methods see results in *evaluation* order, not source order.

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
    @Serial private static final long serialVersionUID = 1L;

    @NelumboConstructor
    public NMyContainer(Functor functor, List<AstElement> elements, Object[] args) {
        // Wrap the elements in your desired internal collection.
        super(functor, elements, MyInternalColl.of(args));
    }

    private NMyContainer(Object[] array, List<AstElement> elements, NMyContainer declaration) {
        super(array, elements, declaration);
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
    protected NMyContainer struct(Object[] array, List<AstElement> elements, Node declaration) {
        return new NMyContainer(array, elements, (NMyContainer) declaration);
    }

    @Override
    public NMyContainer set(int i, Object... a) {
        return (NMyContainer) super.set(i, a);
    }

    @Override
    public String toString(TokenType[] previous) {
        return /* render your collection */ ;
    }
}
```

### Key decisions

1. **Extend `Node`, not `Predicate`.** A container is a value.
2. **`args()` override** tells the engine what the container's children are for traversal and binding purposes. For `NList`, `args()` returns the list in order. For `NSet`, it returns the set's elements in some canonical order (the shipped implementation uses `asList()`).
3. **Pick an internal representation** that matches your collection's semantics. The shipped `NSet` uses an immutable `Set` (no duplicates, unordered); `NList` uses an immutable `List`.
4. **`toString(TokenType[])`** should emit the user-facing syntax (e.g. `{a, b, c}` for a set, `[a, b, c]` for a list).

### Study the source

- `org.modelingvalue.nelumbo.collections.NSet` — simplest form
- `org.modelingvalue.nelumbo.collections.NList` — includes an `elementsFlattened()` helper for nested list flattening

---

## General checklist before shipping a native

Before you consider a new native "done":

- [ ] The class is `final` unless you expect it to be further subclassed.
- [ ] It has a private constructor `(Object[], List<AstElement>, TheClass)` and a `struct(...)` override calling it. The engine uses this to re-structure predicates during reasoning.
- [ ] The `@NelumboConstructor` annotation is on the public constructor that takes `(Functor, List<AstElement>, Object[])`.
- [ ] The `serialVersionUID` is set.
- [ ] If your native constructs instances of itself at runtime (like `Add` building `NInteger`s), you have a `static { KnowledgeBase.registerFunctorSetter(...) }` block and a public `of(...)` factory.
- [ ] `infer(int nrOfUnbound, InferContext context)` handles every combination of bound/unbound arguments and returns `unresolvable()` when it cannot proceed.
- [ ] Closure flags are chosen deliberately. `factCC`/`falsehoodCC` only when the result is fully determined. `factCI`/`falsehoodCI` when you've made a single-point claim but don't know about the other side. `unknown()` when you truly cannot decide.
- [ ] The class is on the classpath of any Nelumbo program that imports a module referencing it via `@`.
- [ ] You have a stdlib-style test file — similar to `integersTest.nl` — that exercises your native in every direction (all bound, each single-unbound case, pathological inputs).

---

## Common pitfalls

**Returning `factCC()` when you shouldn't.** `factCC` asserts both sides are complete — facts side exactly `{this}` and falsehoods side exactly `{}`. If the facts side is really just "this one tuple, but there could be others I haven't enumerated," use `factCI` instead. Getting this wrong makes downstream rules unsound (they'll derive false contradictions).

**Forgetting `unresolvable()` for the too-many-unbound case.** Returning anything else (`unknown()`, `factCI()` with garbage, etc.) when multiple arguments are unbound leaks confusion into the reasoner. `unresolvable()` is the honest answer: "come back later."

**Constructing native values without `of(...)`.** Calling a value-type's public constructor directly skips the functor registration and will fail at runtime. Always go through the static `of(...)` factory.

**Truth tables that don't handle `unknown`.** In `BinaryPredicate`, all four `isTrue`/`isFalse` overrides should consider what happens when one operand is unknown. For conjunction-like operators, `unknown & false = false` is non-obvious. Test the `unknown` rows of your truth table against a `logicTest.nl`-style file.

**Not mirroring the existing stdlib patterns.** If you are adding `mod` to integers, model it on `Multiply`. If you are adding a new comparison, model it on `GreaterThan`. The stdlib has already worked out the right shapes — follow them.

---

## See also

- [`../reference/native-api.md`](../reference/native-api.md) — full API surface reference
- [`../reference/native-classes.md`](../reference/native-classes.md) — catalogue of shipped natives
- [`../explanation/architecture.md`](../explanation/architecture.md) — why Java/Nelumbo is split where it is
- [`writing-your-own-module.md`](writing-your-own-module.md) — the in-language alternative
- [`language-transformations.md`](language-transformations.md) — the meta-level alternative
