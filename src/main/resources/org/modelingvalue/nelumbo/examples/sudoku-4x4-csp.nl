// Norvig candidate-set CSP sudoku solver (4x4), options as a Digit enum.
// CLI-only. Run with: java -jar nelumbo-cli-*.jar <this file>
//
// STATUS (2026-10-19): Tasks 1-8, 10-12 done (10-12 probed without parse). A Norvig
// `return False` is an explicit `false` branch: a branch whose guard fails is silent
// (unknown), not false. `/` is exact division: floor division via bro/bco/fr/fc.
// Task 9 (parse) BLOCKED on bugs/call-arg-unbinds-index-in-pos-rule.nl; probes commented out.
// Task 17 (grid DSL) BLOCKED on bugs/single-hole-functor-parser-npe.nl: `Cell ::= ".", <Integer>`
// crashes the parser; its section at the end is commented out.
// History of fixed blocks and the plan: docs/superpowers/plans/2026-09-11-sudoku-csp.md.

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

List<List<Set<Digit>>> ::= elim(<List<List<Set<Digit>>>>,<Integer>,<Integer>,<Digit>),
                           afterElim(<List<List<Set<Digit>>>>,<Integer>,<Integer>,<Digit>,<Set<Digit>>),
                           assign(<List<List<Set<Digit>>>>,<Integer>,<Integer>,<Digit>),
                           assignOthers(<List<List<Set<Digit>>>>,<Integer>,<Integer>,<Digit>,<Integer>),
                           nakedSingle(<List<List<Set<Digit>>>>,<Integer>,<Integer>),
                           elimPeers(<List<List<Set<Digit>>>>,<Integer>,<Integer>,<Digit>),
                           elimPeersR(<List<List<Set<Digit>>>>,<Integer>,<Integer>,<Digit>,<Integer>),
                           elimPeersC(<List<List<Set<Digit>>>>,<Integer>,<Integer>,<Digit>,<Integer>,<Integer>)

Set<Integer>           ::= rowFree(<List<List<Set<Digit>>>>,<Integer>,<Digit>,<Integer>),
                           colFree(<List<List<Set<Digit>>>>,<Integer>,<Digit>,<Integer>),
                           boxFree(<List<List<Set<Digit>>>>,<Integer>,<Integer>,<Digit>,<Integer>)
List<List<Set<Digit>>> ::= hidden(<List<List<Set<Digit>>>>,<Integer>,<Integer>,<Digit>),
                           hRow(<List<List<Set<Digit>>>>,<Integer>,<Digit>,<Set<Integer>>),
                           hCol(<List<List<Set<Digit>>>>,<Integer>,<Digit>,<Set<Integer>>),
                           hBox(<List<List<Set<Digit>>>>,<Integer>,<Integer>,<Digit>,<Set<Integer>>)

List<List<Set<Digit>>> ::= parse(<List<List<Integer>>>),
                           parseAt(<List<List<Integer>>>,<List<List<Set<Digit>>>>,<Integer>,<Integer>)

Boolean ::= solved(<List<List<Set<Digit>>>>)
Integer ::= minCand(<List<List<Set<Digit>>>>),
            minCandAt(<List<List<Set<Digit>>>>,<Integer>,<Integer>,<Integer>),
            pickCell(<List<List<Set<Digit>>>>,<Integer>,<Integer>)

List<List<Set<Digit>>> ::= search(<List<List<Set<Digit>>>>),
                           tryCand(<List<List<Set<Digit>>>>,<Integer>,<Integer>)

Integer             ::= cellNum(<Set<Digit>>)
List<Integer>       ::= numRow(<List<Set<Digit>>>)
List<List<Integer>> ::= toNums(<List<List<Set<Digit>>>>),
                        solve(<List<List<Integer>>>)

Integer ::= bb(<Integer>), bro(<Integer>), bco(<Integer>), fr(<Integer>), fc(<Integer>)
Boolean ::= samebox(<Integer>,<Integer>,<Integer>,<Integer>),
            peer(<Integer>,<Integer>,<Integer>,<Integer>)

Integer                i, j, k, m, n, t, r, c, r2, c2, v
Digit                  d
Set<Digit>             sc, ns, sy
Set<Integer>           pl, pl2
List<Integer>          il
List<List<Integer>>    p, q
List<Set<Digit>>       rw, rw2
List<List<Set<Digit>>> g, g2, g3, s, pre, suf

dig(i)     == D1 if i=1, D2 if i=2, D3 if i=3, D4 if i=4
undig(d)   == 1 if d=D1, 2 if d=D2, 3 if d=D3, 4 if d=D4

iat(il,i)=k    <=> k pos il = i  if i>=0 & i<4
irow(p,r)=il   <=> il pos p = r  if r>=0 & r<4
icell(p,r,c)   == iat(irow(p,r),c)

catRow(rw,c)=sc <=> sc pos rw = c if c>=0 & c<4
crow(g,r)=rw    <=> rw pos g = r  if r>=0 & r<4
cell(g,r,c)     == catRow(crow(g,r),c)

fullGrid(p)   == [[{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4}],
                  [{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4}],
                  [{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4}],
                  [{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4}]]

// functional update: put candidate set ns at (r,c), everything else unchanged
sel(j,i,ns,sc)      == ns if j=i,  sc if j!=i
putRow(rw,i,ns)     == [0,1,2,3] map [j](sel(j,i,ns,catRow(rw,j)))
rowsBefore(g,r)=s   <=> s=[]                                                      if r=0,
                        E[rw,pre](crow(g,r-1)=rw & rowsBefore(g,r-1)=pre & s=pre+[rw]) if r>0
rowsFrom(g,r)=s     <=> s=[]                                                      if r=4,
                        E[rw,suf](crow(g,r)=rw & rowsFrom(g,r+1)=suf & s=[rw]+suf) if r<4
put(g,r,c,ns)=s     <=> E[pre,suf](rowsBefore(g,r)=pre & rowsFrom(g,r+1)=suf &
                                   E[rw,rw2](crow(g,r)=rw & putRow(rw,c,ns)=rw2 & s=pre+[rw2]+suf))

bb(r)   == 0 if r<2, 2 if r>=2
// `/` is exact division (1/2 has no value): floor division = exact division of (x - remainder)
bro(m)=k <=> E[t](t in {0,1} & (m-t)/2=k)
bco(m)   == m-2*bro(m)
fr(i)=k  <=> E[t](t in {0,1,2,3} & (i-t)/4=k)   // flat index i=r*4+c -> r
fc(i)    == i-4*fr(i)                          //                    -> c
samebox(r,c,r2,c2) <=> E[i,j](bb(r)=i & bb(c)=j & bb(r2)=i & bb(c2)=j)
peer(r,c,r2,c2)    <=> !(r=r2 & c=c2) & (r=r2 | c=c2 | samebox(r,c,r2,c2))

// eliminate d at (r,c); an emptied cell is an explicit `false` branch = Norvig's
// `return False` (a branch whose guard fails is silent = unknown, not false)
elim(g,r,c,d)=s <=> s=g                                              if !(d in cell(g,r,c)),
                    E[ns,g2](cell(g,r,c)-{d}=ns & put(g,r,c,ns)=g2 &
                             afterElim(g2,r,c,d,ns)=s)               if d in cell(g,r,c)

// naked single, then hidden single across the three units of (r,c)
afterElim(g,r,c,d,ns)=s <=> false                                             if |ns|=0,
                            E[g2](nakedSingle(g,r,c)=g2 & hidden(g2,r,c,d)=s) if |ns|=1,
                            hidden(g,r,c,d)=s                                 if |ns|>1

// assign d at (r,c) = eliminate every OTHER digit index e (1..4) from the cell
assign(g,r,c,d)   == assignOthers(g,r,c,d,1)
assignOthers(g,r,c,d,i)=s <=> s=g                                     if i=5,
                              s=assignOthers(g,r,c,d,i+1)             if i<5 & dig(i)=d,
                              E[g2](elim(g,r,c,dig(i))=g2 & s=assignOthers(g2,r,c,d,i+1)) if i<5 & dig(i)!=d

// naked single: cell is a singleton {d} -> remove d from all peers
nakedSingle(g,r,c)=s <=> E[d](d in cell(g,r,c) & elimPeers(g,r,c,d)=s)

elimPeers(g,r,c,d)        == elimPeersR(g,r,c,d,0)
elimPeersR(g,r,c,d,r2)=s  <=> s=g if r2=4,
                              E[g2](elimPeersC(g,r,c,d,r2,0)=g2 & s=elimPeersR(g2,r,c,d,r2+1)) if r2<4
elimPeersC(g,r,c,d,r2,c2)=s <=> s=g                             if c2=4,
                                s=elimPeersC(g,r,c,d,r2,c2+1)   if c2<4 & !peer(r,c,r2,c2),
                                E[g2](elim(g,r2,c2,d)=g2 & s=elimPeersC(g2,r,c,d,r2,c2+1)) if c2<4 & peer(r,c,r2,c2)

// columns of row r where d is still a candidate
rowFree(g,r,d,c)=pl <=> pl={} if c=4,
                        E[pl2](rowFree(g,r,d,c+1)=pl2 & pl=pl2||{c}) if c<4 & d in cell(g,r,c),
                        pl=rowFree(g,r,d,c+1)                        if c<4 & !(d in cell(g,r,c))
// rows of column c where d is still a candidate
colFree(g,c,d,r)=pl <=> pl={} if r=4,
                        E[pl2](colFree(g,c,d,r+1)=pl2 & pl=pl2||{r}) if r<4 & d in cell(g,r,c),
                        pl=colFree(g,c,d,r+1)                        if r<4 & !(d in cell(g,r,c))
// box cells (index m=0..3 -> row bb(r)+bro(m), col bb(c)+bco(m)) where d is a candidate
boxFree(g,r,c,d,m)=pl <=> pl={} if m=4,
                          E[pl2](boxFree(g,r,c,d,m+1)=pl2 & pl=pl2||{m}) if m<4 & E[i,j](bb(r)=i & bb(c)=j &
                                                         d in cell(g,i+bro(m),j+bco(m))),
                          pl=boxFree(g,r,c,d,m+1)                        if m<4 & !E[i,j](bb(r)=i & bb(c)=j &
                                                         d in cell(g,i+bro(m),j+bco(m)))

// place d in a unit if it has exactly one free cell; 0 free -> fail (contradiction)
hRow(g,r,d,pl)=s <=> false if |pl|=0,  s=g if |pl|>1,  E[c2](c2 in pl & assign(g,r,c2,d)=s) if |pl|=1
hCol(g,c,d,pl)=s <=> false if |pl|=0,  s=g if |pl|>1,  E[r2](r2 in pl & assign(g,r2,c,d)=s) if |pl|=1
hBox(g,r,c,d,pl)=s <=> false if |pl|=0,
                       s=g if |pl|>1,
                       E[m,i,j](m in pl & bb(r)=i & bb(c)=j & assign(g,i+bro(m),j+bco(m),d)=s) if |pl|=1

hidden(g,r,c,d)=s <=> E[g2,g3](hRow(g,r,d,rowFree(g,r,d,0))=g2 &
                               hCol(g2,c,d,colFree(g2,c,d,0))=g3 &
                               hBox(g3,r,c,d,boxFree(g3,r,c,d,0))=s)

// walk the puzzle, assign each non-zero clue on the full grid
parse(p)   == parseAt(p, fullGrid(p), 0, 0)
parseAt(p,g,r,c)=s <=> s=g                       if r=4,
                       s=parseAt(p,g,r+1,0)      if r<4 & c=4,
                       s=parseAt(p,g,r,c+1)      if r<4 & c<4 & icell(p,r,c)=0,
                       E[g2](assign(g,r,c,dig(icell(p,r,c)))=g2 & s=parseAt(p,g2,r,c+1)) if r<4 & c<4 & icell(p,r,c)!=0

solved(g) <=> !E[r,c](r in {0,1,2,3} & c in {0,1,2,3} & |cell(g,r,c)|!=1)

// smallest candidate-set size among unfilled cells (best starts at 5 = above max)
minCand(g)   == minCandAt(g,0,0,5)
minCandAt(g,r,c,k)   == k                                if r=4,
                        minCandAt(g,r+1,0,k)             if r<4 & c=4,
                        minCandAt(g,r,c+1,k)             if r<4 & c<4 & |cell(g,r,c)|<=1,
                        minCandAt(g,r,c+1,|cell(g,r,c)|) if r<4 & c<4 & |cell(g,r,c)|>1 & |cell(g,r,c)|<k,
                        minCandAt(g,r,c+1,k)             if r<4 & c<4 & |cell(g,r,c)|>1 & |cell(g,r,c)|>=k

// flat index (r*4+c) of the first cell whose candidate count = n, scanning from i
pickCell(g,n,i)   == i                 if i<16 & |cell(g,fr(i),fc(i))|=n,
                     pickCell(g,n,i+1) if i<16 & |cell(g,fr(i),fc(i))|!=n

// solved -> done; else the MRV cell k, its candidates tried in digit order, first success wins
search(g)=s <=> s=g                                                   if solved(g),
                E[k](k=pickCell(g,minCand(g),0) & tryCand(g,k,1)=s)  if !solved(g)
// digit i at flat cell k; when assign or the search below it fails, try the next digit
tryCand(g,k,i)=s <=> false                                              if i=5,
                     tryCand(g,k,i+1)=s                                 if i<5 & !(dig(i) in cell(g,fr(k),fc(k))),
                     E[g2](assign(g,fr(k),fc(k),dig(i))=g2 & search(g2)=s) if i<5 & dig(i) in cell(g,fr(k),fc(k)) &
                         E[g2,g3](assign(g,fr(k),fc(k),dig(i))=g2 & search(g2)=g3),
                     tryCand(g,k,i+1)=s                                 if i<5 & dig(i) in cell(g,fr(k),fc(k)) &
                         !E[g2,g3](assign(g,fr(k),fc(k),dig(i))=g2 & search(g2)=g3)

// unparse: every solved singleton back to its integer
cellNum(sc)=v <=> E[d](d in sc & |sc|=1 & undig(d)=v)
numRow(rw)    == [0,1,2,3] map [c](cellNum(catRow(rw,c)))
toNums(g)     == [0,1,2,3] map [r](numRow(crow(g,r)))
solve(p)      == toNums(search(parse(p)))

dig(2)    ? [(D2)][..]
undig(D3) ? [(3)][..]
icell([[1,0,0,0],[0,0,1,0],[0,3,0,0],[0,0,0,4]],2,1) ? [(3)][..]
cell([[{D1,D2},{D3},{D4},{D1}]],0,0) ? [({D1,D2})][..]
E[g](fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]])=g & cell(g,1,2)=sc) ? [(sc={D1,D2,D3,D4})][..]
peer(0,0,1,1) ? [()][]
peer(0,0,2,2) ? [][()]
peer(0,0,0,0) ? [][()]
// were undecided until the 2026-09-25 fix of rule-pos-on-mapped-collection-list-undecided
cell(put(fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]]),0,0,{D2}),0,0) ? [({D2})][..]
cell(put(fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]]),0,0,{D2}),0,1) ? [({D1,D2,D3,D4})][..]
// Task 6: bare eliminate
cell(elim(fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]]),0,0,D1),0,0) ? [({D2,D3,D4})][..]
cell(elim(put(fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]]),0,0,{D2}),0,0,D1),0,0) ? [({D2})][..]
// [][..] alone, [..][..] after the first probe above: bugs/empty-set-query-changes-later-result.nl
//elim(put(fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]]),0,0,{D2}),0,0,D2) ? [][..]
// Task 7: assign + naked single
cell(assign(fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]]),0,0,D1),0,0) ? [({D1})][..]
cell(assign(fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]]),0,0,D1),0,1) ? [({D2,D3,D4})][..]
cell(assign(fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]]),0,0,D1),1,1) ? [({D2,D3,D4})][..]
cell(assign(fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]]),0,0,D1),2,2) ? [({D1,D2,D3,D4})][..]
// Task 8: hidden single
rowFree(fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]]),0,D1,0) ? [({0,1,2,3})][..]
boxFree(fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]]),0,0,D1,0) ? [({0,1,2,3})][..]
// D1 left only at (0,3) in row 0 -> placed there; that removes D4 from (0,3) -> D4 placed at (0,2)
cell(elim([[{D1,D2,D3},{D2,D3},{D2,D3,D4},{D1,D4}],[{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4}],[{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4}],[{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4}]],0,0,D1),0,3) ? [({D1})][..]
cell(elim([[{D1,D2,D3},{D2,D3},{D2,D3,D4},{D1,D4}],[{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4}],[{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4}],[{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4},{D1,D2,D3,D4}]],0,0,D1),0,2) ? [({D4})][..]
// Task 9: parse; propagation alone solves P0 (solution [[1,2,4,3],[3,4,1,2],[4,3,2,1],[2,1,3,4]])
// BLOCKED on bugs/call-arg-unbinds-index-in-pos-rule.nl: icell(p,r,c)=0 -> "Inconsistent result"
//cell(parse([[1,0,0,0],[0,0,1,0],[0,3,0,0],[0,0,0,4]]),0,0) ? [({D1})][..]
//cell(parse([[1,0,0,0],[0,0,1,0],[0,3,0,0],[0,0,0,4]]),0,1) ? [({D2})][..]
//cell(parse([[1,0,0,0],[0,0,1,0],[0,3,0,0],[0,0,0,4]]),3,0) ? [({D2})][..]
// Task 10: solved + MRV; parse is blocked, so on literal grids: Q2=[[0,0,3,4],[3,4,1,2],[0,0,4,3],[4,3,2,1]]
// after propagation (4 blanks {D1,D2}, 2 solutions), and a solved grid
solved([[{D1,D2},{D1,D2},{D3},{D4}],[{D3},{D4},{D1},{D2}],[{D1,D2},{D1,D2},{D4},{D3}],[{D4},{D3},{D2},{D1}]]) ? [][()]
solved([[{D1},{D2},{D3},{D4}],[{D3},{D4},{D1},{D2}],[{D2},{D1},{D4},{D3}],[{D4},{D3},{D2},{D1}]]) ? [()][]
minCand([[{D1,D2},{D1,D2},{D3},{D4}],[{D3},{D4},{D1},{D2}],[{D1,D2},{D1,D2},{D4},{D3}],[{D4},{D3},{D2},{D1}]]) ? [(2)][..]
pickCell([[{D1,D2},{D1,D2},{D3},{D4}],[{D3},{D4},{D1},{D2}],[{D1,D2},{D1,D2},{D4},{D3}],[{D4},{D3},{D2},{D1}]],2,0) ? [(0)][..]
pickCell([[{D1},{D2},{D3},{D4}],[{D3},{D4},{D1},{D2}],[{D1,D2},{D1,D2},{D4},{D3}],[{D4},{D3},{D2},{D1}]],2,0) ? [(8)][..]
// Task 11: search; Q2's first success puts D1 at (0,0)
solved(search([[{D1},{D2},{D3},{D4}],[{D3},{D4},{D1},{D2}],[{D2},{D1},{D4},{D3}],[{D4},{D3},{D2},{D1}]])) ? [()][]
search([[{D1,D2},{D1,D2},{D3},{D4}],[{D3},{D4},{D1},{D2}],[{D1,D2},{D1,D2},{D4},{D3}],[{D4},{D3},{D2},{D1}]]) ? [([[{D1},{D2},{D3},{D4}],[{D3},{D4},{D1},{D2}],[{D2},{D1},{D4},{D3}],[{D4},{D3},{D2},{D1}]])][..]
// backtracking: D1 at (0,0) empties (2,0), so D2 wins
search([[{D1,D2},{D1,D2},{D3},{D4}],[{D3},{D4},{D1},{D2}],[{D1},{D1,D2},{D4},{D3}],[{D4},{D3},{D2},{D1}]]) ? [([[{D2},{D1},{D3},{D4}],[{D3},{D4},{D1},{D2}],[{D1},{D2},{D4},{D3}],[{D4},{D3},{D2},{D1}]])][..]
// Task 12: unparse on a literal solved grid; the solve oracle needs parse (Task 9 BLOCKED)
toNums([[{D1},{D2},{D3},{D4}],[{D3},{D4},{D1},{D2}],[{D2},{D1},{D4},{D3}],[{D4},{D3},{D2},{D1}]]) ? [([[1,2,3,4],[3,4,1,2],[2,1,4,3],[4,3,2,1]])][..]
// end to end without parse: the givens assigned by hand (P0: propagation alone; Q2: DFS)
toNums(search(assign(assign(assign(assign(fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]]),0,0,D1),1,2,D1),2,1,D3),3,3,D4))) ? [([[1,2,4,3],[3,4,1,2],[4,3,2,1],[2,1,3,4]])][..]
toNums(search(assign(assign(assign(assign(assign(assign(assign(assign(assign(assign(assign(assign(fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]]),0,2,D3),0,3,D4),1,0,D3),1,1,D4),1,2,D1),1,3,D2),2,2,D4),2,3,D3),3,0,D4),3,1,D3),3,2,D2),3,3,D1))) ? [([[1,2,3,4],[3,4,1,2],[2,1,4,3],[4,3,2,1]])][..]
//solve([[1,0,0,0],[0,0,1,0],[0,3,0,0],[0,0,0,4]]) ? [([[1,2,4,3],[3,4,1,2],[4,3,2,1],[2,1,3,4]])][..]
//solve([[0,2,3,4],[3,4,1,0],[2,0,4,3],[4,3,0,1]]) ? [([[1,2,3,4],[3,4,1,2],[2,1,4,3],[4,3,2,1]])][..]
//solve([[0,0,0,0],[0,4,1,2],[2,1,0,3],[4,3,2,0]]) ? [([[1,2,3,4],[3,4,1,2],[2,1,4,3],[4,3,2,1]])][..]
// Q2 has two solutions; search keeps the first (D1 at (0,0))
//solve([[0,0,3,4],[3,4,1,2],[0,0,4,3],[4,3,2,1]]) ? [([[1,2,3,4],[3,4,1,2],[2,1,4,3],[4,3,2,1]])][..]

// ---- Grid DSL (plan Task 17): row notation -> List<Integer>, `.` = blank = 0 ----
// BLOCKED on bugs/single-hole-functor-parser-npe.nl: the cell's lone-hole
// alternative `<Integer>` makes the parser NPE on every later Integer
// expression, so the whole section (declarations included) stays commented out.
//
//Cell          :: Object
//Cell          ::= ".", <Integer>
//Integer       ::= cellNum(<Cell>)
//List<Integer> ::= sudokuRow <(> <Cell#100> <)+> #0
//
//Cell          x0, x1, x2, x3
//
//cellNum(.)=v <=> v=0
//cellNum(k)=v <=> v=k
//sudokuRow x0 x1 x2 x3 = il <=> il=[cellNum(x0),cellNum(x1),cellNum(x2),cellNum(x3)]
//
//sudokuRow . 2 3 4 ? [([0,2,3,4])][..]
