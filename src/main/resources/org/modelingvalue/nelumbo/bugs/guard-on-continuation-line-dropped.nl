// Bug: an `if` guard on its own continuation line (the previous line's parens are
// balanced) gives a parse error and is then silently dropped: the alternative becomes
// guardless, so f(3) matches both alternatives ("Inconsistent results").
// Correct: accept the continuation, or fail the file hard.
import nelumbo.integers

R :: Object
Integer ::= f(<Integer>)
Integer x, y

f(x)=y <=>  y=1 if x<5,
            y=2
                if x>=5

f(3)=y ? [(y=1)][..]
f(7)=y ? [(y=2)][..]
