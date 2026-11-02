// Regression test (fixed 2026-10-26; was bugs/undeclared-variable-diagnostics.nl).
// An undeclared name in a rule body (q) is not a variable: the rule is not well-formed.
// The parse error is on q itself, and the query over the rule is unknown.
// RegressionTest.undeclaredNameInRule checks the error position and the result.
import nelumbo.integers

Integer ::= f(<Integer>,<Integer>)
Integer a, b, z

f(a,b)=z <=>  z=a+q

f(1,2)=z ? [..][..]
