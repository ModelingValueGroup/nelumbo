// Regression test (2026-10-26; was bugs/guard-on-continuation-line-dropped.nl).
// A NEWLINE after a non-operator ends the statement, so an `if` guard on its own line
// is a parse error on that line. The alternative before it has no guard, so f(3)
// matches both alternatives: "Inconsistent results". f(7) only matches the guardless one.
// RegressionTest.guardOnOwnLine checks both errors and the f(7) result.
import nelumbo.integers

Integer ::= f(<Integer>)
Integer x, y

f(x)=y <=>  y=1 if x<5,
            y=2
                if x>=5

f(3)=y ?
f(7)=y ? [(y=2)][..]
