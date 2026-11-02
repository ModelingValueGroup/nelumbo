// A 0-arity function (lst == [1,2,3]) passed directly as an argument crashes the
// whole run with IllegalArgumentException "argument type mismatch" when the rule
// body hands it to a native (at, |l|). Bound through E[l](lst=l & second(l)=x),
// or as a call with an argument (lstOf(0)), it works.
import nelumbo.collections

List<Integer> ::= lst
Integer       ::= second(<List<Integer>>)

List<Integer> l
Integer       x

lst         == [1,2,3]
second(l)=x <=> at 1 in l = x

second(lst) ? [(2)][..]
