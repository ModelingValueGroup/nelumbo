// Bug: a collection-returning functor CALL on the RHS of `=` in a rule body is
// NOT evaluated - the result carries the unreduced term (e.g. catRow([...],0))
// plus leaking anonymous bindings. The SAME call on the LHS (`f(args)=var`)
// evaluates correctly, and an Integer-returning functor works on EITHER side,
// so `=` is inconsistently non-commutative for collection-returning functors.
//
//   pick(rw,c)=sc <=> sc = catRow(rw,c)   -> sc = catRow([...],0)   (WRONG)
//   pick(rw,c)=sc <=> catRow(rw,c) = sc   -> sc = {D1,D2}           (correct)
//   ipick(il,c)=k <=> k = iat(il,c)       -> k = 12                 (Integer ok on RHS)
//
// catRow standalone works; only the RHS-of-`=` placement of the set-returning
// call fails to reduce. No E-quantifier needed to trigger it.
// Found 2026-09-11 while writing examples/sudoku-4x4-csp.nl (Norvig CSP solver).
//
// Correct: sc = {D1,D2}. Actual: sc = catRow([{D1,D2},{D3},{D4},{D1}],0).
import nelumbo.collections

Digit :: Object
Digit ::= D1, D2, D3, D4

Integer          c
Set<Digit>       sc
List<Set<Digit>> rw

Set<Digit> ::= catRow(<List<Set<Digit>>>,<Integer>),
               pick(<List<Set<Digit>>>,<Integer>)

catRow(rw,c)=sc <=> sc pos rw = c if c>=0 & c<4
pick(rw,c)=sc   <=> sc = catRow(rw,c)   // set-returning functor on RHS -> not reduced

pick([{D1,D2},{D3},{D4},{D1}],0)=sc ? [(sc={D1,D2})][..]
