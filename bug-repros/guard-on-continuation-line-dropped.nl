// Bug: when an alternative's `if` guard stands on its own continuation line
// (legal-looking indented continuation, but the previous line's parens are
// balanced), the parser reports "Unexpected token ..." for the guard line
// and then SILENTLY DROPS IT, keeping the alternative guardless. Evaluation
// continues with changed semantics: below, f(3) matches BOTH alternatives
// (y=1 via its guard, y=2 because its guard was dropped), which the engine
// reports as "Inconsistent results" for the f(3) query. In sudoku-9x9-smart.nl's
// development this turned a guarded solver into a givens-overwriting one
// that still looked green because the extra results hid in `,..` forms.
// Correct: either accept the continuation or fail the file hard.
// Actual: parse error printed, guard dropped, inconsistent results.
// Legal continuations must break INSIDE unbalanced parens with the guard on
// the closing line (see sudoku-9x9.nl solve).
import nelumbo.integers

R :: Object
Integer ::= f(<Integer>)
Integer x, y

f(x)=y <=>  y=1 if x<5,
            y=2
                if x>=5

f(3)=y ? [(y=1)][..]
f(7)=y ? [(y=2)][..]
