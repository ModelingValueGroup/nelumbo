// Regression test (fixed 2026-10-27; was bugs/call-arg-unbinds-index-in-pos-rule.nl).
// With a call as the list argument, iat(lst(0),1)=0 evaluates the index relation with
// the index unbound. With the removed `pos`, a function, a value occurring more than
// once in the list then failed with "Inconsistent result ... has non unique bindings".
// The relation `at i in l = e` may have several answers.
import nelumbo.collections

Integer       ::= iat(<List<Integer>>,<Integer>)
List<Integer> ::= lst(<Integer>)

Integer       i, k
List<Integer> il

iat(il,i)=k <=> at i in il = k
lst(i)=il   <=> il=[1,0,0,0]

iat([1,0,0,0],1)=0 ? [()][]
iat(lst(0),0)=1    ? [()][]
iat(lst(0),1)=0    ? [()][]
