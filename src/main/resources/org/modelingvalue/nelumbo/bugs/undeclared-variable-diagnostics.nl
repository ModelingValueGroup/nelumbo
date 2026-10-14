// Bug: an undeclared variable in a rule gives a misleading parse error at a nearby token
// ("Unexpected token '\n', expected '<','::'") that never names the variable, and the
// rule is silently dropped, so the query is undecided.
// Correct: an error naming the undeclared variable (q).
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
