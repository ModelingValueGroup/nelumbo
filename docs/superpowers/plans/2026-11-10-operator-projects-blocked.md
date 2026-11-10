# Two more operator projects for the tutorial (blocked)

Two tutorial projects in operator notation, next to `#number-toolkit`, `#set-algebra` and
`#list-statistics`. Both need a user type declared `:: Literal` with operators whose first hole has
that type, which hits `bugs/literal-type-hole-pattern-merge.nl` ("Non deterministic pattern merge").
Add them when that bug is fixed (`KnownBugsTest.literalTypeHolePatternMerge` then fails with
"KNOWN BUG APPEARS FIXED").

To add one: re-run the draft with the CLI (also with `-DRANDOM_NELUMBO=true` and
`-DREVERSE_NELUMBO=true`), write the section in the Projects format (intro, hints in
`<details class="project-hints">`, an exercise field with `data-syntax="exercise"` that holds only the
import and the given part, and the model solution ending with the identical given part), insert it
after `#list-statistics`, and add its id to `SECTIONS` in `website/src/main/frontend/e2e/tutorial.spec.ts`
and to the Projects list in `CLAUDE.md`.

## Expression interpreter

A small expression language inside Nelumbo: numbers `#5`, variables with `x := 3` facts, operators
`++` and `**` with their own precedences, and `eval` defined by recursion on the structure of the
expression. With `Expr :: Literal` every answer below is right, but loading reports three merge
errors (for `++`, `**` and `eval`). With `Expr :: Object` it loads cleanly, but every `eval` of a
compound expression stays `[..][..]`: a functor on `Object` is a function, so the rule head
`eval (e ++ f)` never matches a constructed value. That difference is worth a sentence in the intro.

```nelumbo
import nelumbo.integers
Expr    :: Literal
Var     :: Expr
Var     ::= x, y, z
Expr    ::= # <Integer>          #90,
            <Expr> ++ <Expr>     #40,
            <Expr> ** <Expr>     #50
Integer ::= eval <Expr>          #80
Boolean ::= <Var> := <one Integer> #20
Expr    e, f
Var     v
Integer i
eval #i       == i
eval v = i    <=> v := i
eval (e ++ f) == eval e + eval f
eval (e ** f) == eval e * eval f

// ---- given: the questions and the answers they should get ----
fact x := 3, y := 4

eval #5                 ? [(5)][..]
eval x                  ? [(3)][..]
eval (x ++ #2)          ? [(5)][..]
eval (x ++ y ** #2)     ? [(11)][..]
eval ((x ++ y) ** #2)   ? [(14)][..]
eval (z ++ #1)          ? [..][..]
```

Possible extensions: a symbolic derivative `d/dx` with simplification, or `:=` facts that use
other variables.

## 2D vectors

Vectors as values with `+`, scalar multiplication and a dot product. Besides the merge errors,
`2 * vec(1,2)` collides with the integer `*`: as a query it fails with "Node 2*vec(1,2) of
unexpected type Vec, expected List<Root> or Root", and inside `vec(1,2) + 2 * vec(1,1)` it stays
unevaluated. Re-test after the fix; if the clash remains, use another symbol for scalar
multiplication (for example `<Integer> times <Vec>`).

```nelumbo
import nelumbo.integers
Vec     :: Literal
Vec     ::= vec(<Integer>, <Integer>)
Vec     ::= <Vec> + <Vec>      #40,
            <Integer> * <Vec>  #50
Integer ::= <Vec> . <Vec>      #50
Integer a, b, c, d, k
vec(a,b) + vec(c,d) == vec(a+c, b+d)
k * vec(a,b)        == vec(k*a, k*b)
vec(a,b) . vec(c,d) == a*c+b*d

// ---- given: the questions and the answers they should get ----
vec(1,2) + vec(3,4)          ? [(vec(4,6))][..]
2 * vec(1,2)                 ? [(vec(2,4))][..]
vec(1,2) . vec(3,4)          ? [(11)][..]
vec(1,2) + 2 * vec(1,1)      ? [(vec(3,4))][..]
vec(1,2) = vec(1,2)          ? [()][]
```

Possible extensions: length squared `|v|^2` (with the power operator of `#number-toolkit`),
perpendicular vectors (`u . v = 0`), or a point type with translation.
