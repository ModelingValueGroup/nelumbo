// Norvig candidate-set CSP sudoku solver (4x4), options as a Digit enum.
// CLI-only. Run with: java -DPARALLEL_COLLECTIONS=false -jar nelumbo-cli-*.jar <this file>
//
// STATUS (2026-09-11): BLOCKED at the functional grid update. Building this
// surfaced three deterministic engine bugs (repros in bug-repros/):
//   1. nested-list-functor-arg-type-mismatch.nl - a collection-returning functor
//      CALL nested as an argument crashes; worked around here by E-binding.
//   2. set-functor-rhs-equals-not-reduced.nl - a collection-valued expression on
//      the RHS of `=` is not reduced; worked around here by using the LHS form.
//   3. recursive-list-concat-classcast.nl - a recursive collection-accumulation
//      builder (concat/union) crashes (ClassCast) or diverges. This is a
//      REGRESSION and it blocks `put` and every candidate-set builder, so the
//      solver cannot progress past the accessors until it is fixed.
// What works so far and is kept below: the Digit/IO map, all grid accessors, and
// the peer/box relation. See docs/superpowers/plans/2026-09-11-sudoku-csp.md.

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

Set<Digit>             ::= catRow(<List<Set<Digit>>>,<Integer>),
                          cell(<List<List<Set<Digit>>>>,<Integer>,<Integer>)
List<Set<Digit>>       ::= crow(<List<List<Set<Digit>>>>,<Integer>)
List<List<Set<Digit>>> ::= fullGrid(<List<List<Integer>>>)

Integer ::= bb(<Integer>)
Boolean ::= samebox(<Integer>,<Integer>,<Integer>,<Integer>),
            peer(<Integer>,<Integer>,<Integer>,<Integer>)

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

catRow(rw,c)=sc <=> sc pos rw = c if c>=0 & c<4
crow(g,r)=rw    <=> rw pos g = r  if r>=0 & r<4
cell(g,r,c)=sc  <=> E[rw](crow(g,r)=rw & catRow(rw,c) = sc)   // LHS form: bug 3

fullGrid(p)=g <=> g=[[{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4}],
                     [{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4}],
                     [{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4}],
                     [{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4}]]

bb(r)=k <=> k=0 if r<2, k=2 if r>=2
samebox(r,c,r2,c2) <=> E[i,j](bb(r)=i & bb(c)=j & bb(r2)=i & bb(c2)=j)
peer(r,c,r2,c2)    <=> !(r=r2 & c=c2) & (r=r2 | c=c2 | samebox(r,c,r2,c2))

dig(2)=d    ? [(d=D2)][..]
undig(D3)=v ? [(v=3)][..]
icell([[1,0,0,0],[0,0,1,0],[0,3,0,0],[0,0,0,4]],2,1)=v ? [(v=3)][..]
cell([[{D1,D2},{D3},{D4},{D1}]],0,0)=sc ? [(sc={D1,D2})][..]
E[g](fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]])=g & cell(g,1,2)=sc) ? [(sc={D1,D2,D3,D4})][..]
peer(0,0,1,1) ? [()][]
peer(0,0,2,2) ? [][()]
peer(0,0,0,0) ? [][()]
