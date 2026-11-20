// Standard mathematical notation for Nelumbo
//
// Adds the usual Unicode symbols of propositional and first-order logic,
// of sets and of lists (sequences), and defines each one as an equivalence
// (<=>, or == for a value) with the ASCII operators of nelumbo.logic and
// nelumbo.collections. Because a rule is an equivalence, a symbol here
// means exactly what its definition means, with the same four-valued
// result (facts, falsehoods and both completeness flags).
//
// Logic
// ⊤ ⊥ true, false
// ¬ φ !φ #25
// φ ∧ ψ φ & ψ #22
// φ ∨ ψ φ | ψ #20
// φ ⊕ ψ exclusive or #20
// φ ↑ ψ φ ↓ ψ nand, nor #20
// φ → ψ φ -> ψ #18
// φ ← ψ ψ -> φ #18
// φ ↔ ψ φ ≡ ψ φ <-> ψ #16
// ∀x.φ A[x](φ)
// ∃x.φ E[x](φ)
// ∄x.φ !E[x](φ)
// ∀x∈S.φ A[x](x in S -> φ)
// ∃x∈S.φ E[x](x in S & φ)
// a ≠ b a != b #30
// λx.e [x](e) #7
//
// Sets
// ∅ {}
// a ∈ S a in S #30
// a ∉ S !(a in S) #30
// S ⊆ U S <= U #30
// S ⊂ U S < U (proper subset) #30
// S ⊇ U S >= U #30
// S ⊃ U S > U #30
// S ∪ U S || U #60
// S ∩ U S && U #60
// S ∖ U S - U #50
// S △ U symmetric difference #50
// {x | φ} {[x](φ)}
//
// Lists
// ⟨a,b,c⟩ [a,b,c]
// a ∈ s a ∉ s a in s, !(a in s) #30
// s ⌢ t s + t #50
//
// The cardinality |S| and the length |s| are already standard notation.
//
// Mind the tokenizer: a run of operator characters is ONE token, so
// write p ∧ ¬q , ¬ ¬p and | ∅ | , not p∧¬q , ¬¬p or |∅| .
//
// Like a quantifier, a λ takes everything to its right as its body, so as
// the operand of an operator it needs parentheses: l map (λx.e) .
//
// A set of a restricted variable, {x ∈ S | φ} , is not defined: it reads as
// the set literal of the one formula x ∈ S | φ . Write {x | x ∈ S ∧ φ} .

import nelumbo.collections

// ---------------------------------------------------------------- syntax

Type T, R

Boolean        ::= ⊤,
                   ⊥,
                   ¬ <Boolean>                                   #25,
                   <Boolean> ∧ <Boolean>                         #22,
                   <Boolean> ∨ <Boolean>                         #20,
                   <Boolean> ⊕ <Boolean>                         #20,
                   <Boolean> ↑ <Boolean>                         #20,
                   <Boolean> ↓ <Boolean>                         #20,
                   <Boolean> → <Boolean>                         #18,
                   <Boolean> ← <Boolean>                         #18,
                   <Boolean> ↔ <Boolean>                         #16,
                   <Boolean> ≡ <Boolean>                         #16,
                   ∀ <Variable#100>. <Boolean>                   #5,
                   ∃ <Variable#100>. <Boolean>                   #5,
                   ∄ <Variable#100>. <Boolean>                   #5,
                   ∀ <{Variable,T}#100> ∈ <Set<T>#31>. <Boolean> #5,
                   ∃ <{Variable,T}#100> ∈ <Set<T>#31>. <Boolean> #5,
                   <Object> ≠ <Object>                           #30

// A lambda is a value, not a formula, so it has no defining rule: it is
// bound to the native class of [x](e) and is the same lambda.
Lambda1<T,R>   ::= λ <{Variable,T}>. <R> #7 @nelumbo.logic.Lambda

Boolean        ::= <T> ∈ <Collection<T>> #30,
                   <T> ∉ <Collection<T>> #30,
                   <Set<T>> ⊆ <Set<T>>   #30,
                   <Set<T>> ⊂ <Set<T>>   #30,
                   <Set<T>> ⊇ <Set<T>>   #30,
                   <Set<T>> ⊃ <Set<T>>   #30

Set<T>         ::= <Set<T>> ∪ <Set<T>>   #60,
                   <Set<T>> ∩ <Set<T>>   #60,
                   <Set<T>> ∖ <Set<T>>   #50,
                   <Set<T>> △ <Set<T>>   #50,
                   { <{Variable,T}> | <Boolean> }

List<T>        ::= <List<T>> ⌢ <List<T>> #50

// Like λ, the empty set and a list in angle brackets are values: they are
// bound to the native classes of {} and [a,b,c] and are the same values.
struct Set<T>  ::= ∅ @nelumbo.collections.NSet
struct List<T> ::= ⟨ <(> <(> <T> <|> .. <)> <,> , <)*> ⟩ @nelumbo.collections.NList

// ------------------------------------------------------------- semantics

Boolean       φ, ψ
T             a, b
Collection<T> c
Set<T>        S, U
List<T>       s, t

⊤       <=>  true
⊥       <=>  false

¬ φ     <=>  !φ
φ ∧ ψ   <=>  φ & ψ
φ ∨ ψ   <=>  φ | ψ
φ ⊕ ψ   <=>  (φ | ψ) & !(φ & ψ)
φ ↑ ψ   <=>  !(φ & ψ)
φ ↓ ψ   <=>  !(φ | ψ)
φ → ψ   <=>  φ -> ψ
φ ← ψ   <=>  ψ -> φ
φ ↔ ψ   <=>  φ <-> ψ
φ ≡ ψ   <=>  φ <-> ψ

∀a.φ    <=>  A[a](φ)
∃a.φ    <=>  E[a](φ)
∄a.φ    <=>  !E[a](φ)
∀a∈S.φ  <=>  A[a]((a in S) -> φ)
∃a∈S.φ  <=>  E[a](a in S & φ)

a ≠ b   <=>  a != b

a ∈ c   <=>  a in c
a ∉ c   <=>  !(a in c)
S ⊆ U   <=>  S <= U
S ⊂ U   <=>  S < U
S ⊇ U   <=>  S >= U
S ⊃ U   <=>  S > U

S ∪ U   ==  S || U
S ∩ U   ==  S && U
S ∖ U   ==  S - U
S △ U   ==  (S - U) || (U - S)
{a | φ} ==  {[a](φ)}

s ⌢ t   ==  s + t

// ----------------------------------------------------------------- tests
// Each query should give the same result as its ASCII counterpart.

⊤           ? [()][]
⊥           ? [][()]
¬ ⊥         ? [()][]
⊤ ∧ ⊥       ? [][()]
⊤ ∨ ⊥       ? [()][]
⊤ ⊕ ⊤       ? [][()]
⊤ ⊕ ⊥       ? [()][]
⊤ ↑ ⊤       ? [][()]
⊥ ↓ ⊥       ? [()][]
⊥ → ⊥       ? [()][]
⊤ → ⊥       ? [][()]
⊥ ← ⊤       ? [][()]
⊤ ↔ ⊤       ? [()][]
⊤ ≡ ⊥       ? [][()]
¬ ⊤ ∨ ⊤ ∧ ⊥ ? [][()]
unknown ∨ ⊤ ? [()][]
unknown ∧ ⊥ ? [][()]

Person          :: Object
Person          ::= Ann, Bob, Cy
private Boolean ::= likes(<Person>, <Person>)
fact likes(Ann, Bob), likes(Bob, Cy)

Person x, y

likes(x, y) <=>  likes(y, x)

Ann ≠ Bob                  ? [()][]
Ann ≠ Ann                  ? [][()]
∃x.likes(Ann, x)           ? [()][]
∃x.likes(x, Ann)           ? [()][]
∄x.likes(x, Ann)           ? [][()]
∀y.likes(Ann, y) → y ≠ Ann ? [..][..]

// A quantifier is E or A applied to a lambda, so λ can be used there too.

E λx.likes(Ann, x)           ? [()][]
A λy.likes(Ann, y) → y ≠ Ann ? [..][..]

Integer      i
Set<Integer> si

[1,2,3] map (λi.i+1)         ? [([2,3,4])][..]
[1,2,3] where (λi.i>1)       ? [([2,3])][..]
{1,2,3} where (λi.i>1 ∧ i<3) ? [({2})][..]

// sets

2 ∈ {1,2,3}                  ? [()][]
4 ∈ {1,2,3}                  ? [][()]
4 ∉ {1,2,3}                  ? [()][]
i ∈ {1,2}                    ? [(i=1),(i=2)][..]
Bob ∈ {Ann,Bob}              ? [()][]
{1} ⊆ {1,2}                  ? [()][]
{1,2} ⊆ {1,2}                ? [()][]
{1} ⊂ {1,2}                  ? [()][]
{1,2} ⊂ {1,2}                ? [][()]
{1,2} ⊇ {1,2,3}              ? [][()]
{1,2} ⊃ {1}                  ? [()][]
{1,2} ∪ {2,3}                ? [({1,2,3})][..]
{1,2} ∩ {2,3}                ? [({2})][..]
{1,2} ∖ {2,3}                ? [({1})][..]
{1,2} △ {2,3}                ? [({1,3})][..]
{1,2} ∩ {3,4} = ∅            ? [()][]
{1,2} ∪ ∅ = si               ? [(si={1,2})][..]
∅ ⊆ {1}                      ? [()][]
1 ∈ ∅                        ? [][()]
| ∅ |                        ? [(0)][..]
|{1,2} ∪ {2,3}|              ? [(3)][..]
{i | i ∈ {1,2,3} ∧ i>1}      ? [({2,3})][({1,..}),..]
3 ∈ {i | i ∈ {1,2,3} ∧ i>1}  ? [()][]
∀i∈{1,2,3}.i>0               ? [()][]
∀i∈{1,2,3}.i>1               ? [][()]
∃i∈{1,2,3}.i>2               ? [()][]
∃i∈{1,2} ∪ {3}.i>3           ? [][()]

// lists

⟨1,2,3⟩ = [1,2,3]            ? [()][]
⟨⟩ = []                      ? [()][]
⟨1,2⟩ ⌢ ⟨3⟩                  ? [([1,2,3])][..]
2 ∈ ⟨1,2,3⟩                  ? [()][]
4 ∉ ⟨1,2,3⟩                  ? [()][]
|⟨1,2⟩ ⌢ ⟨3⟩|                ? [(3)][..]
⟨⟨1⟩,⟨2,3⟩⟩ map (λs.|s|)     ? [([1,2])][..]
