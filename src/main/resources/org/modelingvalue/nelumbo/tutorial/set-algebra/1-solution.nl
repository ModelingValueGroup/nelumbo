import nelumbo.collections
Set<Integer> ::= <Set<Integer>> xor <Set<Integer>> #60,
                 <Set<Integer>> + <Set<Integer>>   #40,
                 <Set<Integer>> * <Integer>        #50
Boolean      ::= <Set<Integer>> disjoint <Set<Integer>> #30
Set<Integer> s, t
Integer      i, j, k, n
s xor t      == (s - t) || (t - s)
s * k        == {[n](E[i](i in s & n = i*k))}
s + t        == {[n](E[i,j](i in s & j in t & n = i+j))}
s disjoint t <=> s && t = {}

// ---- given: the questions and the answers they should get ----
{1,2,3} xor {2,3,4}   ? [({1,4})][..]
{1,2} xor {2,1}       ? [({})][..]
{1,2} disjoint {3,4}  ? [()][]
{1,2} disjoint {2,3}  ? [][()]
{1,2,3} * 10          ? [({10,20,30})][..]
{1,2} + {10,20}       ? [({11,12,21,22})][..]
|{1,2,3} + {1,2,3}|   ? [(5)][..]
{1,2} + {10} * 2      ? [({21,22})][..]
{1,2} + {0} = {1,2}   ? [()][]
{1,2,..} xor {2}      ? [({1,..})][..]
