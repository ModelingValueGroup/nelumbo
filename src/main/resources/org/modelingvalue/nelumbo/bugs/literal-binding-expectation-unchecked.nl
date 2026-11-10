// expect-error-per-query: Expected result
// Bug: an expected result that binds a variable of a user `:: Literal` type is not checked:
// here = p ? [(p=there)][..] passes although p is here, while an Integer binding such as
// (n=3) for 1+1=n is checked. What ran before in the JVM decides the symptom: in the CLI the
// binding is ignored, in a JUnit run (and when evaluating directly) it does not parse:
// "Unexpected token '='".
import nelumbo.integers

Pos   :: Literal
Pos   ::= here, there
Range :: Literal
Range ::= range(<Integer>, <Integer>)

Pos   p
Range r

here = p        ? [(p=there)][..]
range(1, 5) = r ? [(r=range(1,6))][..]
