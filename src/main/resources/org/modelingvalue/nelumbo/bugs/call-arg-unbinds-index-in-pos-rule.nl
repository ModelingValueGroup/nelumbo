// Bug: with a call as the list argument, the bound index argument is not bound while
// the rule body runs: iat(lst(0),1)=0 evaluates `0 pos [1,0,0,0] = i` with i unbound
// and fails with "Inconsistent result: [(i=1),(i=2),(i=3)][..] has non unique bindings".
// A literal list, or a value that occurs once in the list, decides.
import nelumbo.collections

Integer       ::= iat(<List<Integer>>,<Integer>)
List<Integer> ::= lst(<Integer>)

Integer       i, k
List<Integer> il

iat(il,i)=k <=> k pos il = i
lst(i)=il   <=> il=[1,0,0,0]

iat([1,0,0,0],1)=0 ? [()][]
iat(lst(0),0)=1    ? [()][]
iat(lst(0),1)=0    ? [()][]
