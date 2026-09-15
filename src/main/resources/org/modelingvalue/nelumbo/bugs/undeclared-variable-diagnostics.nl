// Bug: using an UNDECLARED variable in a rule produces a parse error that
// points at a nearby token with a completely unrelated message ("Unexpected
// token '\n', expected '<','::'" here; "Unexpected token '+'" when the
// unknown name is mid-expression), never mentioning the unknown identifier.
// The broken rule is then silently dropped and evaluation continues, so the
// query below reports undecided instead of failing fast.
// Correct: an error naming the undeclared variable (q), and ideally a hard
// stop. Actual: misleading error + rule silently missing.
// (This misdirection cost an hour in the 2026-09-10 sudoku2 session: the
// error was read as an arithmetic-argument limitation.)
import nelumbo.integers

R :: Object
Integer ::= f(<Integer>,<Integer>)
Integer a, b, z

// q is not declared:
f(a,b)=z <=>  z=a+q

// correct behavior would be a clear error about q; instead f is dropped and
// this query is undecided (the expectation states what f SHOULD yield if the
// rule had complained properly and q were fixed to b):
f(1,2)=z ? [(z=3)][..]
