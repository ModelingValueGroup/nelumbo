// Bug: a List-returning functor CALL used directly as an argument to another
// functor (whose body reduces to a native predicate) crashes with
//   java.lang.IllegalArgumentException: argument type mismatch
//   at logic/Predicate.callMethod (reflective native invoke)
//
// Deterministic: 9/9 runs, independent of -DPARALLELISM and
// -DPARALLEL_COLLECTIONS (unlike the interning race repros next to this file).
//
// Minimal trigger: nest the List-returning call `row(g,r)` as the first
// argument of `at`, which reduces to the native `pos` / indexOf. The inner
// call's value reaches the native consumer unresolved.
//   - Binding the inner call via E[..] first WORKS (see the commented form).
//   - Simple arithmetic nesting `inc(inc(i))` does NOT crash - so it is
//     specific to a functor-call argument the native consumer gets unresolved.
//
// examples/sudoku-4x4-smart.nl uses this exact
//   cell(g,r,c)=x <=> x=at(row(g,r),c)
// nesting (and bug-repros/speculative-guard-index-crash.nl reproduces it for a
// DIFFERENT bug, so the nesting itself used to be accepted). Both 4x4 sudoku
// example files now also crash on the CLI (ClassCastException) - the same
// interning / type-confusion, a different face.
//
// Correct: x = 3 (element at column 1 of row 2 of the grid).
// Actual:  IllegalArgumentException: argument type mismatch.
// Found 2026-09-11 while writing examples/sudoku-4x4-csp.nl (Norvig CSP solver).
import nelumbo.collections

Integer             i, r, c, x
List<Integer>       l, rw
List<List<Integer>> g

Integer       ::= at(<List<Integer>>,<Integer>),
                  cell(<List<List<Integer>>>,<Integer>,<Integer>)
List<Integer> ::= row(<List<List<Integer>>>,<Integer>)

at(l,i)=x     <=> x pos l = i  if i>=0 & i<4
row(g,r)=rw   <=> rw pos g = r if r>=0 & r<4

// direct nesting of the List-returning call row(g,r) into at -> crash
cell(g,r,c)=x <=> x = at(row(g,r),c)

// WORKS (kept for contrast): bind the inner call via E first
// cell(g,r,c)=x <=> E[rw](row(g,r)=rw & x = at(rw,c))

cell([[1,0,0,0],[0,0,1,0],[0,3,0,0],[0,0,0,4]],2,1)=x ? [(x=3)][..]
