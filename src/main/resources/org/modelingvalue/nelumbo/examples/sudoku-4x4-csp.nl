import nelumbo.collections

Digit :: Object
Digit ::= D1, D2, D3, D4

Digit   ::= dig(<Integer>)
Integer ::= undig(<Digit>)

// NOTE: inner functor-calls are E-bound (not nested as args) to get past
// bug-repros/nested-list-functor-arg-type-mismatch.nl (found this session).
Integer       ::= iat(<List<Integer>>,<Integer>),
                  icell(<List<List<Integer>>>,<Integer>,<Integer>)
List<Integer> ::= irow(<List<List<Integer>>>,<Integer>)

Integer                i, j, k, m, r, c, r2, c2, v
Digit                  d
Set<Digit>             sc, ns, sy
Set<Integer>           pl
List<Integer>          il
List<List<Integer>>    p
List<Set<Digit>>       rw, rw2
List<List<Set<Digit>>> g, g2, g3, s, pre, suf

dig(i)=d   <=> d=D1 if i=1, d=D2 if i=2, d=D3 if i=3, d=D4 if i=4
undig(d)=v <=> v=1 if d=D1, v=2 if d=D2, v=3 if d=D3, v=4 if d=D4

iat(il,i)=k    <=> k pos il = i  if i>=0 & i<4
irow(p,r)=il   <=> il pos p = r  if r>=0 & r<4
icell(p,r,c)=k <=> E[il](irow(p,r)=il & k = iat(il,c))

dig(2)=d    ? [(d=D2)][..]
undig(D3)=v ? [(v=3)][..]
icell([[1,0,0,0],[0,0,1,0],[0,3,0,0],[0,0,0,4]],2,1)=v ? [(v=3)][..]
