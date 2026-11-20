// Standard logic notation for Nelumbo
//
// Adds the usual Unicode symbols of propositional and first-order logic
// and defines each one as an equivalence (<=>) with the ASCII operators of
// nelumbo.logic. Because a rule is an equivalence, a symbol here means
// exactly what its definition means, with the same four-valued result
// (facts, falsehoods and both completeness flags).
//
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
// a ≠ b a != b #30
//
// Mind the tokenizer: a run of operator characters is ONE token, so
// write p ∧ ¬q and ¬ ¬p , not p∧¬q or ¬¬p .

import nelumbo.logic

// ---------------------------------------------------------------- syntax

Boolean ::= ⊤,
            ⊥,
            ¬ <Boolean>             #25,
            <Boolean> ∧ <Boolean>   #22,
            <Boolean> ∨ <Boolean>   #20,
            <Boolean> ⊕ <Boolean>   #20,
            <Boolean> ↑ <Boolean>   #20,
            <Boolean> ↓ <Boolean>   #20,
            <Boolean> → <Boolean>   #18,
            <Boolean> ← <Boolean>   #18,
            <Boolean> ↔ <Boolean>   #16,
            <Boolean> ≡ <Boolean>   #16,
            ∀ <Variable>. <Boolean> #5,
            ∃ <Variable>. <Boolean> #5,
            ∄ <Variable>. <Boolean> #5,
            <Object> ≠ <Object>     #30

// ------------------------------------------------------------- semantics

Boolean φ, ψ
Type    T
T       a, b

⊤     <=>  true
⊥     <=>  false

¬ φ   <=>  !φ
φ ∧ ψ <=>  φ & ψ
φ ∨ ψ <=>  φ | ψ
φ ⊕ ψ <=>  (φ | ψ) & !(φ & ψ)
φ ↑ ψ <=>  !(φ & ψ)
φ ↓ ψ <=>  !(φ | ψ)
φ → ψ <=>  φ -> ψ
φ ← ψ <=>  ψ -> φ
φ ↔ ψ <=>  φ <-> ψ
φ ≡ ψ <=>  φ <-> ψ

∀a.φ  <=>  A[a](φ)
∃a.φ  <=>  E[a](φ)
∄a.φ  <=>  !E[a](φ)

a ≠ b <=>  a != b

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

Ann ≠ Bob                  ? [()][]
Ann ≠ Ann                  ? [][()]
∃x.likes(Ann, x)           ? [()][]
∃x.likes(x, Ann)           ? [..][..]
∄x.likes(x, Ann)           ? [..][..]
∀y.likes(Ann, y) → y ≠ Ann ? [..][..]
