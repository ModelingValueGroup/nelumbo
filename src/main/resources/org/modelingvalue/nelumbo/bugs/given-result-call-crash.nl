// A function query whose given result is itself a call crashes the whole run with
// IllegalArgumentException "argument type mismatch": the call reaches a native predicate
// unevaluated (g(1) = h(1) becomes 1+1 = h(1)). Also |[1,2]| = |[3,4]|, and as a rule guard
// (if |l| = |m|). With the result as a literal, or bound first (E[n](|l|=n & |m|=n)), it works.
import org.modelingvalue.nelumbo.sudoku.csp4x4

List<List<Integer>> ::= solution(<Integer>)
Integer             ::= g(<Integer>), h(<Integer>)

Integer i

solution(i) == [[1,2,4,3],[3,4,1,2],[4,3,2,1],[2,1,3,4]]
g(i)        == i+1
h(i)        == i*2

g(1) = h(1)       ? [()][]
|[1,2]| = |[3,4]| ? [()][]
sudoku([[1,0,0,0],[0,0,1,0],[0,3,0,0],[0,0,0,4]]) = solution(0) ? [()][]
