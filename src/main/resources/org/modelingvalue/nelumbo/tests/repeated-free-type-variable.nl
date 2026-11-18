// Regression test.
// The free element type of [] occurs twice in the type of dup([]): Pair<List<T>,List<T>>.
// The hole of fst binds it through its second argument (List<Integer>), so the first one,
// which gives fst its result type, is List<Integer> too. RegressionTest also checks that
// texts(fst(dup([]))) is a type error.
import nelumbo.collections
import nelumbo.strings

Type A, B
Pair<A,B>        :: Object
struct Pair<A,A> ::= dup(<A>)
A                ::= fst(<Pair<A,List<Integer>>>)
Integer          ::= numbers(<List<Integer>>),
                     texts(<List<String>>)

List<Integer> x
List<String>  y

fst(dup(x)) == x
numbers(x)  == |x|
texts(y)    == |y|

fst(dup([]))          ? [([])][..]
fst(dup([1,2]))       ? [([1,2])][..]
numbers(fst(dup([]))) ? [(0)][..]
texts([])             ? [(0)][..]
