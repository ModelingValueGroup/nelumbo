// Norvig candidate-set CSP sudoku solver (9x9): line-for-line rescale of
// sudoku-4x4-csp.nl (digits D1..D9, 3x3 boxes, indices 0..8, flat index over 81).
// Every cell holds the set of digits still possible, assign/eliminate propagate
// naked and hidden singles, search is a DFS on the cell with the fewest candidates
// (MRV). Puzzles go in and out as List<List<Integer>> (0 = blank).
// CLI-only (not in ExamplesTest): java -jar nelumbo-cli-*.jar <this file>
//
// A Norvig `return False` is an explicit `false` branch: a branch whose guard fails
// is silent (unknown), not false. `/` is exact division: floor division via
// bro/bco/fr/fc. The grid DSL is not ported (Task 17 BLOCKED, see the 4x4 file).
// Spec: docs/superpowers/specs/2026-09-11-sudoku-csp-design.md,
// plan: docs/superpowers/plans/2026-09-11-sudoku-csp.md.

import nelumbo.collections

Digit :: Object
Digit ::= D1, D2, D3, D4, D5, D6, D7, D8, D9

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

dig(i)     == D1 if i=1, D2 if i=2, D3 if i=3, D4 if i=4, D5 if i=5, D6 if i=6, D7 if i=7, D8 if i=8, D9 if i=9
undig(d)   == 1 if d=D1, 2 if d=D2, 3 if d=D3, 4 if d=D4, 5 if d=D5, 6 if d=D6, 7 if d=D7, 8 if d=D8, 9 if d=D9

iat(il,i)=k    <=> at i in il = k  if i>=0 & i<9
irow(p,r)=il   <=> at r in p = il  if r>=0 & r<9
icell(p,r,c)   == iat(irow(p,r),c)

catRow(rw,c)=sc <=> at c in rw = sc if c>=0 & c<9
crow(g,r)=rw    <=> at r in g = rw  if r>=0 & r<9
cell(g,r,c)     == catRow(crow(g,r),c)

fullGrid(p)   == [[{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9}],
                  [{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9}],
                  [{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9}],
                  [{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9}],
                  [{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9}],
                  [{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9}],
                  [{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9}],
                  [{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9}],
                  [{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9},{D1,D2,D3,D4,D5,D6,D7,D8,D9}]]

// functional update: put candidate set ns at (r,c), everything else unchanged
sel(j,i,ns,sc)      == ns if j=i,  sc if j!=i
putRow(rw,i,ns)     == [0,1,2,3,4,5,6,7,8] map [j](sel(j,i,ns,catRow(rw,j)))
rowsBefore(g,r)=s   <=> s=[]                                                      if r=0,
                        E[rw,pre](crow(g,r-1)=rw & rowsBefore(g,r-1)=pre & s=pre+[rw]) if r>0
rowsFrom(g,r)=s     <=> s=[]                                                      if r=9,
                        E[rw,suf](crow(g,r)=rw & rowsFrom(g,r+1)=suf & s=[rw]+suf) if r<9
put(g,r,c,ns)=s     <=> E[pre,suf](rowsBefore(g,r)=pre & rowsFrom(g,r+1)=suf &
                                   E[rw,rw2](crow(g,r)=rw & putRow(rw,c,ns)=rw2 & s=pre+[rw2]+suf))

bb(r)   == 0 if r<3, 3 if r>=3 & r<6, 6 if r>=6
// `/` is exact division (1/3 has no value): floor division = exact division of (x - remainder)
bro(m)=k <=> E[t](t in {0,1,2} & (m-t)/3=k)
bco(m)   == m-3*bro(m)
fr(i)=k  <=> E[t](t in {0,1,2,3,4,5,6,7,8} & (i-t)/9=k)   // flat index i=r*9+c -> r
fc(i)    == i-9*fr(i)                                    //                    -> c
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

// assign d at (r,c) = eliminate every OTHER digit index e (1..9) from the cell
assign(g,r,c,d)   == assignOthers(g,r,c,d,1)
assignOthers(g,r,c,d,i)=s <=> s=g                                     if i=10,
                              s=assignOthers(g,r,c,d,i+1)             if i<10 & dig(i)=d,
                              E[g2](elim(g,r,c,dig(i))=g2 & s=assignOthers(g2,r,c,d,i+1)) if i<10 & dig(i)!=d

// naked single: cell is a singleton {d} -> remove d from all peers
nakedSingle(g,r,c)=s <=> E[d](d in cell(g,r,c) & elimPeers(g,r,c,d)=s)

elimPeers(g,r,c,d)        == elimPeersR(g,r,c,d,0)
elimPeersR(g,r,c,d,r2)=s  <=> s=g if r2=9,
                              E[g2](elimPeersC(g,r,c,d,r2,0)=g2 & s=elimPeersR(g2,r,c,d,r2+1)) if r2<9
elimPeersC(g,r,c,d,r2,c2)=s <=> s=g                             if c2=9,
                                s=elimPeersC(g,r,c,d,r2,c2+1)   if c2<9 & !peer(r,c,r2,c2),
                                E[g2](elim(g,r2,c2,d)=g2 & s=elimPeersC(g2,r,c,d,r2,c2+1)) if c2<9 & peer(r,c,r2,c2)

// columns of row r where d is still a candidate
rowFree(g,r,d,c)=pl <=> pl={} if c=9,
                        E[pl2](rowFree(g,r,d,c+1)=pl2 & pl=pl2||{c}) if c<9 & d in cell(g,r,c),
                        pl=rowFree(g,r,d,c+1)                        if c<9 & !(d in cell(g,r,c))
// rows of column c where d is still a candidate
colFree(g,c,d,r)=pl <=> pl={} if r=9,
                        E[pl2](colFree(g,c,d,r+1)=pl2 & pl=pl2||{r}) if r<9 & d in cell(g,r,c),
                        pl=colFree(g,c,d,r+1)                        if r<9 & !(d in cell(g,r,c))
// box cells (index m=0..8 -> row bb(r)+bro(m), col bb(c)+bco(m)) where d is a candidate
boxFree(g,r,c,d,m)=pl <=> pl={} if m=9,
                          E[pl2](boxFree(g,r,c,d,m+1)=pl2 & pl=pl2||{m}) if m<9 & E[i,j](bb(r)=i & bb(c)=j &
                                                         d in cell(g,i+bro(m),j+bco(m))),
                          pl=boxFree(g,r,c,d,m+1)                        if m<9 & !E[i,j](bb(r)=i & bb(c)=j &
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
parseAt(p,g,r,c)=s <=> s=g                       if r=9,
                       s=parseAt(p,g,r+1,0)      if r<9 & c=9,
                       s=parseAt(p,g,r,c+1)      if r<9 & c<9 & icell(p,r,c)=0,
                       E[g2](assign(g,r,c,dig(icell(p,r,c)))=g2 & s=parseAt(p,g2,r,c+1)) if r<9 & c<9 & icell(p,r,c)!=0

solved(g) <=> !E[r,c](r in {0,1,2,3,4,5,6,7,8} & c in {0,1,2,3,4,5,6,7,8} & |cell(g,r,c)|!=1)

// smallest candidate-set size among unfilled cells (best starts at 10 = above max)
minCand(g)   == minCandAt(g,0,0,10)
minCandAt(g,r,c,k)   == k                                if r=9,
                        minCandAt(g,r+1,0,k)             if r<9 & c=9,
                        minCandAt(g,r,c+1,k)             if r<9 & c<9 & |cell(g,r,c)|<=1,
                        minCandAt(g,r,c+1,|cell(g,r,c)|) if r<9 & c<9 & |cell(g,r,c)|>1 & |cell(g,r,c)|<k,
                        minCandAt(g,r,c+1,k)             if r<9 & c<9 & |cell(g,r,c)|>1 & |cell(g,r,c)|>=k

// flat index (r*9+c) of the first cell whose candidate count = n, scanning from i
pickCell(g,n,i)   == i                 if i<81 & |cell(g,fr(i),fc(i))|=n,
                     pickCell(g,n,i+1) if i<81 & |cell(g,fr(i),fc(i))|!=n

// solved -> done; else the MRV cell k, its candidates tried in digit order, first success wins
search(g)=s <=> s=g                                                   if solved(g),
                E[k](k=pickCell(g,minCand(g),0) & tryCand(g,k,1)=s)  if !solved(g)
// digit i at flat cell k; when assign or the search below it fails, try the next digit
tryCand(g,k,i)=s <=> false                                              if i=10,
                     tryCand(g,k,i+1)=s                                 if i<10 & !(dig(i) in cell(g,fr(k),fc(k))),
                     E[g2](assign(g,fr(k),fc(k),dig(i))=g2 & search(g2)=s) if i<10 & dig(i) in cell(g,fr(k),fc(k)) &
                         E[g2,g3](assign(g,fr(k),fc(k),dig(i))=g2 & search(g2)=g3),
                     tryCand(g,k,i+1)=s                                 if i<10 & dig(i) in cell(g,fr(k),fc(k)) &
                         !E[g2,g3](assign(g,fr(k),fc(k),dig(i))=g2 & search(g2)=g3)

// unparse: every solved singleton back to its integer
cellNum(sc)=v <=> E[d](d in sc & |sc|=1 & undig(d)=v)
numRow(rw)    == [0,1,2,3,4,5,6,7,8] map [c](cellNum(catRow(rw,c)))
toNums(g)     == [0,1,2,3,4,5,6,7,8] map [r](numRow(crow(g,r)))
solve(p)      == toNums(search(parse(p)))
dig(9)    ? [(D9)][..]
undig(D7) ? [(7)][..]
icell([[1,0,0,8,5,7,0,9,0],[0,3,0,0,2,0,0,0,8],[0,0,9,6,0,0,5,0,0],[0,0,5,3,1,0,9,0,0],[0,1,0,0,8,0,0,0,2],[6,0,0,0,0,4,0,0,0],[3,5,6,4,7,8,0,1,0],[2,4,1,9,3,5,0,0,7],[0,9,7,2,6,1,3,0,0]],2,3) ? [(6)][..]
E[g](fullGrid([[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0]])=g & cell(g,8,8)=sc) ? [(sc={D1,D2,D3,D4,D5,D6,D7,D8,D9})][..]
bb(4)   ? [(3)][..]
bro(5)  ? [(1)][..]
bco(5)  ? [(2)][..]
fr(40)  ? [(4)][..]
fc(40)  ? [(4)][..]
fr(80)  ? [(8)][..]
fc(80)  ? [(8)][..]
peer(0,0,2,2) ? [()][]
peer(4,4,3,5) ? [()][]
peer(0,0,3,3) ? [][()]
peer(0,0,0,0) ? [][()]
cell(put(fullGrid([[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0]]),4,4,{D5}),4,4) ? [({D5})][..]
cell(put(fullGrid([[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0]]),4,4,{D5}),4,5) ? [({D1,D2,D3,D4,D5,D6,D7,D8,D9})][..]
// eliminate, assign (one assign on the full grid: ~10 s)
cell(elim(fullGrid([[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0]]),0,0,D1),0,0) ? [({D2,D3,D4,D5,D6,D7,D8,D9})][..]
cell(assign(fullGrid([[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0]]),4,4,D5),3,3) ? [({D1,D2,D3,D4,D6,D7,D8,D9})][..]
// oracle: the sudoku-9x9-smart.nl puzzles. Only the easiest is active: parse alone
// costs ~7 s per clue, and this one did not finish within 30 minutes (2026-10-28).
// 72 clues, one blank per row/column/box, unique solution
solve([[0,2,3,4,5,6,7,8,9],[4,5,6,0,8,9,1,2,3],[7,8,9,1,2,3,0,5,6],[2,0,1,5,6,4,8,9,7],[5,6,4,8,0,7,2,3,1],[8,9,7,2,3,1,5,0,4],[3,1,0,6,4,5,9,7,8],[6,4,5,9,7,0,3,1,2],[9,7,8,3,1,2,6,4,0]]) ? [([[1,2,3,4,5,6,7,8,9],[4,5,6,7,8,9,1,2,3],[7,8,9,1,2,3,4,5,6],[2,3,1,5,6,4,8,9,7],[5,6,4,8,9,7,2,3,1],[8,9,7,2,3,1,5,6,4],[3,1,2,6,4,5,9,7,8],[6,4,5,9,7,8,3,1,2],[9,7,8,3,1,2,6,4,5]])][..]
//solve([[0,0,0,0,0,0,0,0,0],[0,5,6,7,8,9,1,2,3],[7,0,9,1,2,3,4,5,6],[2,3,0,5,6,4,8,9,7],[5,6,4,0,9,7,2,3,1],[8,9,7,2,0,1,5,6,4],[3,1,2,6,4,0,9,7,8],[6,4,5,9,7,8,0,1,2],[9,7,8,3,1,2,6,0,5]]) ? [([[1,2,3,4,5,6,7,8,9],[4,5,6,7,8,9,1,2,3],[7,8,9,1,2,3,4,5,6],[2,3,1,5,6,4,8,9,7],[5,6,4,8,9,7,2,3,1],[8,9,7,2,3,1,5,6,4],[3,1,2,6,4,5,9,7,8],[6,4,5,9,7,8,3,1,2],[9,7,8,3,1,2,6,4,5]])][..]
//solve([[1,0,0,8,5,7,0,9,0],[0,3,0,0,2,0,0,0,8],[0,0,9,6,0,0,5,0,0],[0,0,5,3,1,0,9,0,0],[0,1,0,0,8,0,0,0,2],[6,0,0,0,0,4,0,0,0],[3,5,6,4,7,8,0,1,0],[2,4,1,9,3,5,0,0,7],[0,9,7,2,6,1,3,0,0]]) ? [([[1,6,2,8,5,7,4,9,3],[5,3,4,1,2,9,6,7,8],[7,8,9,6,4,3,5,2,1],[4,7,5,3,1,2,9,8,6],[9,1,3,5,8,6,7,4,2],[6,2,8,7,9,4,1,3,5],[3,5,6,4,7,8,2,1,9],[2,4,1,9,3,5,8,6,7],[8,9,7,2,6,1,3,5,4]])][..]
// 26 clues, needs real guessing (unique solution, verified externally)
//solve([[3,0,0,0,0,0,0,0,0],[8,2,1,0,0,0,3,9,0],[0,0,7,0,0,0,0,8,5],[0,1,4,0,9,5,0,0,3],[9,0,0,0,0,0,5,0,0],[0,6,0,0,0,0,0,0,0],[2,3,0,5,0,0,1,0,0],[0,0,0,6,0,0,0,0,0],[4,0,5,8,0,0,0,0,7]]) ? [([[3,5,9,4,2,8,7,1,6],[8,2,1,7,5,6,3,9,4],[6,4,7,9,3,1,2,8,5],[7,1,4,2,9,5,8,6,3],[9,8,2,3,6,4,5,7,1],[5,6,3,1,8,7,4,2,9],[2,3,6,5,7,9,1,4,8],[1,7,8,6,4,3,9,5,2],[4,9,5,8,1,2,6,3,7]])][..]
