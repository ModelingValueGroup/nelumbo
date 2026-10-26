// Regression test (fixed 2026-10-26; was bugs/speculative-guard-index-crash.nl).
// A guard conjunct that reads a list (at(l,i)) next to a false bound check (i<3,
// r<9 & c<9) may throw an index error, but the false check decides the conjunction:
// the error is irrelevant and the alternative is false. Either operand can go first
// (REVERSE_NELUMBO, RANDOM_NELUMBO), so first/last hold the check on either side.
// The solveS query scans an already solved grid: s = the grid itself.
import nelumbo.collections

// ---- types ----------------------------------------------------------
Grid :: Object        // placeholder so we can name the nested list type
Integer             ::= at(<List<Integer>>,<Integer>),
                        cell(<List<List<Integer>>>,<Integer>,<Integer>),
                        sel(<Integer>,<Integer>,<Integer>,<Integer>),
                        bb(<Integer>),
                        csize(<Set<Integer>>),
                        first(<Integer>),
                        last(<Integer>)
Boolean             ::= ok(<List<List<Integer>>>,<Integer>,<Integer>,<Integer>)
Set<Integer>        ::= fits(<List<List<Integer>>>,<Integer>,<Integer>)
List<Integer>       ::= row(<List<List<Integer>>>,<Integer>),
                        putRow(<List<Integer>>,<Integer>,<Integer>)
List<List<Integer>> ::= rowsBefore(<List<List<Integer>>>,<Integer>),
                        rowsFrom(<List<List<Integer>>>,<Integer>),
                        put(<List<List<Integer>>>,<Integer>,<Integer>,<Integer>),
                        solveS(<List<List<Integer>>>,<Integer>,<Integer>),
                        solveB(<List<List<Integer>>>,<Integer>,<Integer>),
                        sudoku2(<List<List<Integer>>>)

List<List<Integer>> g, g2, s, p, q
List<Integer>       l, rw, rw2
Set<Integer>        cs
Integer             r, c, d, x, y, i, j

// ---- access ---------------------------------------------------------
at(l,i)=x        <=>  x pos l = i
row(g,r)=rw      <=>  rw pos g = r
cell(g,r,c)=x    <=>  x = at(row(g,r),c)

// ---- minimal: the bound check before (first) or after (last) the read --
first(i)=x       <=>  x=0 if i>=3,  x=1 if i<3 & at([5,6,7],i)>5
last(i)=x        <=>  x=0 if i>=3,  x=1 if at([5,6,7],i)>5 & i<3

// ---- update (functional) -------------------------------------------
sel(j,i,d,x)=y      <=>  y=d if j=i,  y=x if j!=i
putRow(l,i,d)=rw    <=>  rw = [0,1,2,3,4,5,6,7,8] map [j](sel(j,i,d,at(l,j)))
rowsBefore(g,r)=s   <=>  s=[]                                                       if r=0,
                         E[rw,p](row(g,r-1)=rw & rowsBefore(g,r-1)=p & s=p+[rw])    if r>0
rowsFrom(g,r)=s     <=>  s=[]                                                       if r=9,
                         E[rw,q](row(g,r)=rw & rowsFrom(g,r+1)=q & s=[rw]+q)        if r<9
put(g,r,c,d)=s      <=>  E[p,q](rowsBefore(g,r)=p & rowsFrom(g,r+1)=q &
                                E[rw,rw2](row(g,r)=rw & putRow(rw,c,d)=rw2 & s=p+[rw2]+q))

// ---- constraint: d allowed at (r,c)? bb = box base row/column --------
bb(r)=x          <=>  x=0 if r<3,  x=3 if r>=3 & r<6,  x=6 if r>=6
ok(g,r,c,d)      <=>  !E[rw](row(g,r)=rw & d in rw) &
                      !E[i](i in {0,1,2,3,4,5,6,7,8} & cell(g,i,c)=d) &
                      !E[x,y](bb(r)=x & bb(c)=y & E[i,j](i in {0,1,2} & j in {0,1,2} & cell(g,x+i,y+j)=d))
fits(g,r,c)=cs   <=>  cs = {1,2,3,4,5,6,7,8,9} where [d](ok(g,r,c,d))
csize(cs)=x      <=>  |cs|=x

// ---- search ---------------------------------------------------------
// solveS: scan for a blank with <=1 candidates; place it (or fail on 0)
// and rescan; a full scan without one hands over to solveB
solveS(g,r,c)=s  <=>  s=solveB(g,0,0)    if r=9,
                      s=solveS(g,r+1,0)  if r<9 & c=9,
                      s=solveS(g,r,c+1)  if r<9 & c<9 & cell(g,r,c)!=0,
                      s=solveS(g,r,c+1)  if r<9 & c<9 & cell(g,r,c)=0 & !E[cs,x](fits(g,r,c)=cs & csize(cs)=x & x<2),
                      E[d,g2](d in {1,2,3,4,5,6,7,8,9} & ok(g,r,c,d) & put(g,r,c,d)=g2 & s=solveS(g2,0,0)) if r<9 & c<9 & cell(g,r,c)=0 & E[cs,x](fits(g,r,c)=cs & csize(cs)=x & x<2)
// solveB: guess at the first blank; no blank at all means solved
solveB(g,r,c)=s  <=>  s=g                if r=9,
                      s=solveB(g,r+1,0)  if r<9 & c=9,
                      s=solveB(g,r,c+1)  if r<9 & c<9 & cell(g,r,c)!=0,
                      E[d,g2](d in {1,2,3,4,5,6,7,8,9} & ok(g,r,c,d) & put(g,r,c,d)=g2 & s=solveS(g2,0,0)) if r<9 & c<9 & cell(g,r,c)=0


// ---- tests ----------------------------------------------------------
at([4,5,6],1)=x                       ? [(x=5)][..]
putRow([1,0,3,0,5,6,7,0,9],1,8)=l     ? [(l=[1,8,3,0,5,6,7,0,9])][..]
put([[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0]],1,2,5)=g ? [(g=[[0,0,0,0,0,0,0,0,0],[0,0,5,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0]])][..]
bb(4)=x                               ? [(x=3)][..]
ok([[1,2,3,4,5,6,7,8,9],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0]],1,0,4) ? [()][]
ok([[1,2,3,4,5,6,7,8,9],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0]],1,0,1) ? [][()]
ok([[1,2,3,4,5,6,7,8,9],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0]],8,0,1) ? [][()]
fits([[1,0,0,8,5,7,0,9,0],[0,3,0,0,2,0,0,0,8],[0,0,9,6,0,0,5,0,0],[0,0,5,3,1,0,9,0,0],[0,1,0,0,8,0,0,0,2],[6,0,0,0,0,4,0,0,0],[3,5,6,4,7,8,0,1,0],[2,4,1,9,3,5,0,0,7],[0,9,7,2,6,1,3,0,0]],2,4)=cs ? [(cs={4})][..]
fits([[1,0,0,8,5,7,0,9,0],[0,3,0,0,2,0,0,0,8],[0,0,9,6,0,0,5,0,0],[0,0,5,3,1,0,9,0,0],[0,1,0,0,8,0,0,0,2],[6,0,0,0,0,4,0,0,0],[3,5,6,4,7,8,0,1,0],[2,4,1,9,3,5,0,0,7],[0,9,7,2,6,1,3,0,0]],1,0)=cs ? [(cs={4,5,7})][..]
csize({1,2})=x                        ? [(x=2)][..]

// one of the two reads the list before its bound check, in every operand order
first(7)=x                            ? [(x=0)][..]
last(7)=x                             ? [(x=0)][..]
first(1)=x                            ? [(x=1)][..]
last(1)=x                             ? [(x=1)][..]

// 72 clues, one blank per row/column/box, unique solution
solveS([[1,2,3,4,5,6,7,8,9],[4,5,6,7,8,9,1,2,3],[7,8,9,1,2,3,4,5,6],[2,3,1,5,6,4,8,9,7],[5,6,4,8,9,7,2,3,1],[8,9,7,2,3,1,5,6,4],[3,1,2,6,4,5,9,7,8],[6,4,5,9,7,8,3,1,2],[9,7,8,3,1,2,6,4,5]],0,0)=s ? [(s=[[1,2,3,4,5,6,7,8,9],[4,5,6,7,8,9,1,2,3],[7,8,9,1,2,3,4,5,6],[2,3,1,5,6,4,8,9,7],[5,6,4,8,9,7,2,3,1],[8,9,7,2,3,1,5,6,4],[3,1,2,6,4,5,9,7,8],[6,4,5,9,7,8,3,1,2],[9,7,8,3,1,2,6,4,5]])][..]
