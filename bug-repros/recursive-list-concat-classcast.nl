// Bug: a recursive list-CONCAT builder crashes with
//   java.lang.ClassCastException: org.modelingvalue.nelumbo.lang.Variable
//   cannot be cast to org.modelingvalue.collections.List
// DETERMINISTIC on the CLI: 5/5 runs, with -DPARALLEL_COLLECTIONS=false.
//
// This is the exact rowsBefore/rowsFrom shape used by
// examples/sudoku-9x9-smart.nl and sudoku-4x4-smart.nl - which now ALSO crash on
// the CLI (REGRESSION vs the 2026-09-10 notes of 20/20 CLI-clean). Concat-FREE
// recursion is fine (a plain counter recurses without crashing); the crash needs
// the `s = pre+[rw]` list concatenation inside the recursion. The same builder
// over a Set-element grid instead DIVERGES (hits the engine deadline) rather than
// crashing - same root (the interning/type-confusion race, cf.
// nondeterministic-inference.nl), a different face.
//
// A recursive SET-UNION accumulator (`pl2||{c}=pl` down a counter) crashes
// identically, so the bug is recursive COLLECTION accumulation in general, not
// list concat specifically. This blocks any functional grid update (`put`) and
// every candidate-set builder in a CSP-style solver.
//
// Correct: s = [[1,2],[3,4]]. Actual: ClassCastException (Variable -> List).
// Found 2026-09-11 while writing examples/sudoku-4x4-csp.nl (Norvig CSP solver).
import nelumbo.collections

Integer             r
List<Integer>       rw
List<List<Integer>> g, s, pre

Integer             ::= at(<List<Integer>>,<Integer>)
List<Integer>       ::= row(<List<List<Integer>>>,<Integer>)
List<List<Integer>> ::= rowsBefore(<List<List<Integer>>>,<Integer>)

row(g,r)=rw       <=> rw pos g = r if r>=0 & r<4
rowsBefore(g,r)=s <=> s=[]                                                        if r=0,
                      E[rw,pre](row(g,r-1)=rw & rowsBefore(g,r-1)=pre & s=pre+[rw]) if r>0

rowsBefore([[1,2],[3,4],[5,6],[7,8]],2)=s ? [(s=[[1,2],[3,4]])][..]
