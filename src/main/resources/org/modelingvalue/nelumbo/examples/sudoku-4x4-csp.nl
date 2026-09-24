// Norvig candidate-set CSP sudoku solver (4x4), options as a Digit enum.
// CLI-only. Run with: java -DPARALLEL_COLLECTIONS=false -jar nelumbo-cli-*.jar <this file>
//
// STATUS (2026-09-24): BLOCKED again at the functional grid update (plan Task 4).
// The first block (three reduction bugs found 2026-09-11: nested collection call
// as argument, collection call on the RHS of `=`, recursive collection
// accumulation) was fixed on 2026-09-24 - they live on as tests/*.nl in
// RegressionTest and the workarounds were removed here. Resuming immediately hit
// the next one: `put` decides, but a cell(g,r,c) read on a putRow-built row is
// undecided - a user rule wrapping `pos` over a `map`-produced list of
// collections. Repro: bugs/rule-pos-on-mapped-collection-list-undecided.nl
// (side finding: bugs/set-literal-arithmetic-unevaluated.nl). The put rules
// are kept below; the two put probes are commented out at the end.
// See docs/superpowers/plans/2026-09-11-sudoku-csp.md.

import nelumbo.collections

Digit :: Object
Digit ::= D1, D2, D3, D4

Digit   ::= dig(<Integer>)
Integer ::= undig(<Digit>)

Integer       ::= iat(<List<Integer>>,<Integer>),
                  icell(<List<List<Integer>>>,<Integer>,<Integer>)
List<Integer> ::= irow(<List<List<Integer>>>,<Integer>)

Set<Digit>             ::= catRow(<List<Set<Digit>>>,<Integer>),
                          cell(<List<List<Set<Digit>>>>,<Integer>,<Integer>)
List<Set<Digit>>       ::= crow(<List<List<Set<Digit>>>>,<Integer>)
List<List<Set<Digit>>> ::= fullGrid(<List<List<Integer>>>)

Set<Digit>             ::= sel(<Integer>,<Integer>,<Set<Digit>>,<Set<Digit>>)
List<Set<Digit>>       ::= putRow(<List<Set<Digit>>>,<Integer>,<Set<Digit>>)
List<List<Set<Digit>>> ::= rowsBefore(<List<List<Set<Digit>>>>,<Integer>),
                           rowsFrom(<List<List<Set<Digit>>>>,<Integer>),
                           put(<List<List<Set<Digit>>>>,<Integer>,<Integer>,<Set<Digit>>)

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
icell(p,r,c)=k <=> k = iat(irow(p,r),c)

catRow(rw,c)=sc <=> sc pos rw = c if c>=0 & c<4
crow(g,r)=rw    <=> rw pos g = r  if r>=0 & r<4
cell(g,r,c)=sc  <=> sc = catRow(crow(g,r),c)

fullGrid(p)=g <=> g=[[{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4}],
                     [{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4}],
                     [{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4}],
                     [{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4}]]

// functional update: put candidate set ns at (r,c), everything else unchanged
sel(j,i,ns,sc)=sy   <=> sy=ns if j=i,  sy=sc if j!=i
putRow(rw,i,ns)=rw2 <=> rw2 = [0,1,2,3] map [j](sel(j,i,ns,catRow(rw,j)))
rowsBefore(g,r)=s   <=> s=[]                                                      if r=0,
                        E[rw,pre](crow(g,r-1)=rw & rowsBefore(g,r-1)=pre & s=pre+[rw]) if r>0
rowsFrom(g,r)=s     <=> s=[]                                                      if r=4,
                        E[rw,suf](crow(g,r)=rw & rowsFrom(g,r+1)=suf & s=[rw]+suf) if r<4
put(g,r,c,ns)=s     <=> E[pre,suf](rowsBefore(g,r)=pre & rowsFrom(g,r+1)=suf &
                                   E[rw,rw2](crow(g,r)=rw & putRow(rw,c,ns)=rw2 & s=pre+[rw2]+suf))

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
// BLOCKED (bugs/rule-pos-on-mapped-collection-list-undecided.nl): both undecided
// cell(put(fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]]),0,0,{D2}),0,0)=sc ? [(sc={D2})][..]
// cell(put(fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]]),0,0,{D2}),0,1)=sc ? [(sc={D1,D2,D3,D4})][..]
