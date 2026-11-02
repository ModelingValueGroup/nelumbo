// Regression test (fixed 2026-10-27; was bugs/where-filter-npe-in-recursion.nl).
// A singles-first sudoku solver whose scan filters with `where` over a user Boolean rule
// recurses deeper than MAX_LOGIC_DEPTH; it must still return the solved grid in every
// rule order. 4 copies of the solver that differ only in functor names vary the rule order.
import nelumbo.collections
// ---- types ----------------------------------------------------------
Grid :: Object        // placeholder so we can name the nested list type
Integer             ::= at1(<List<Integer>>,<Integer>),
                        cell1(<List<List<Integer>>>,<Integer>,<Integer>),
                        sel1(<Integer>,<Integer>,<Integer>,<Integer>),
                        bb1(<Integer>),
                        csize1(<Set<Integer>>)
Boolean             ::= ok1(<List<List<Integer>>>,<Integer>,<Integer>,<Integer>)
Set<Integer>        ::= fits1(<List<List<Integer>>>,<Integer>,<Integer>)
List<Integer>       ::= row1(<List<List<Integer>>>,<Integer>),
                        putRow1(<List<Integer>>,<Integer>,<Integer>)
List<List<Integer>> ::= rowsBefore1(<List<List<Integer>>>,<Integer>),
                        rowsFrom1(<List<List<Integer>>>,<Integer>),
                        put1(<List<List<Integer>>>,<Integer>,<Integer>,<Integer>),
                        solveS1(<List<List<Integer>>>,<Integer>,<Integer>),
                        solveB1(<List<List<Integer>>>,<Integer>,<Integer>)
Integer             ::= at2(<List<Integer>>,<Integer>),
                        cell2(<List<List<Integer>>>,<Integer>,<Integer>),
                        sel2(<Integer>,<Integer>,<Integer>,<Integer>),
                        bb2(<Integer>),
                        csize2(<Set<Integer>>)
Boolean             ::= ok2(<List<List<Integer>>>,<Integer>,<Integer>,<Integer>)
Set<Integer>        ::= fits2(<List<List<Integer>>>,<Integer>,<Integer>)
List<Integer>       ::= row2(<List<List<Integer>>>,<Integer>),
                        putRow2(<List<Integer>>,<Integer>,<Integer>)
List<List<Integer>> ::= rowsBefore2(<List<List<Integer>>>,<Integer>),
                        rowsFrom2(<List<List<Integer>>>,<Integer>),
                        put2(<List<List<Integer>>>,<Integer>,<Integer>,<Integer>),
                        solveS2(<List<List<Integer>>>,<Integer>,<Integer>),
                        solveB2(<List<List<Integer>>>,<Integer>,<Integer>)
Integer             ::= at3(<List<Integer>>,<Integer>),
                        cell3(<List<List<Integer>>>,<Integer>,<Integer>),
                        sel3(<Integer>,<Integer>,<Integer>,<Integer>),
                        bb3(<Integer>),
                        csize3(<Set<Integer>>)
Boolean             ::= ok3(<List<List<Integer>>>,<Integer>,<Integer>,<Integer>)
Set<Integer>        ::= fits3(<List<List<Integer>>>,<Integer>,<Integer>)
List<Integer>       ::= row3(<List<List<Integer>>>,<Integer>),
                        putRow3(<List<Integer>>,<Integer>,<Integer>)
List<List<Integer>> ::= rowsBefore3(<List<List<Integer>>>,<Integer>),
                        rowsFrom3(<List<List<Integer>>>,<Integer>),
                        put3(<List<List<Integer>>>,<Integer>,<Integer>,<Integer>),
                        solveS3(<List<List<Integer>>>,<Integer>,<Integer>),
                        solveB3(<List<List<Integer>>>,<Integer>,<Integer>)
Integer             ::= at4(<List<Integer>>,<Integer>),
                        cell4(<List<List<Integer>>>,<Integer>,<Integer>),
                        sel4(<Integer>,<Integer>,<Integer>,<Integer>),
                        bb4(<Integer>),
                        csize4(<Set<Integer>>)
Boolean             ::= ok4(<List<List<Integer>>>,<Integer>,<Integer>,<Integer>)
Set<Integer>        ::= fits4(<List<List<Integer>>>,<Integer>,<Integer>)
List<Integer>       ::= row4(<List<List<Integer>>>,<Integer>),
                        putRow4(<List<Integer>>,<Integer>,<Integer>)
List<List<Integer>> ::= rowsBefore4(<List<List<Integer>>>,<Integer>),
                        rowsFrom4(<List<List<Integer>>>,<Integer>),
                        put4(<List<List<Integer>>>,<Integer>,<Integer>,<Integer>),
                        solveS4(<List<List<Integer>>>,<Integer>,<Integer>),
                        solveB4(<List<List<Integer>>>,<Integer>,<Integer>)

List<List<Integer>> g, g2, s, p, q
List<Integer>       l, rw, rw2
Set<Integer>        cs
Integer             r, c, d, x, y, i, j

// ==== copy 1 ====
// ---- access ---------------------------------------------------------
at1(l,i)=x        <=>  at i in l = x
row1(g,r)=rw      <=>  at r in g = rw
cell1(g,r,c)=x    <=>  x = at1(row1(g,r),c)

// ---- update (functional) -------------------------------------------
sel1(j,i,d,x)=y      <=>  y=d if j=i,  y=x if j!=i
putRow1(l,i,d)=rw    <=>  rw = [0,1,2,3,4,5,6,7,8] map [j](sel1(j,i,d,at1(l,j)))
rowsBefore1(g,r)=s   <=>  s=[]                                                       if r=0,
                         E[rw,p](row1(g,r-1)=rw & rowsBefore1(g,r-1)=p & s=p+[rw])    if r>0
rowsFrom1(g,r)=s     <=>  s=[]                                                       if r=9,
                         E[rw,q](row1(g,r)=rw & rowsFrom1(g,r+1)=q & s=[rw]+q)        if r<9
put1(g,r,c,d)=s      <=>  E[p,q](rowsBefore1(g,r)=p & rowsFrom1(g,r+1)=q &
                                E[rw,rw2](row1(g,r)=rw & putRow1(rw,c,d)=rw2 & s=p+[rw2]+q))

// ---- constraint: d allowed at (r,c)? bb = box base row/column --------
bb1(r)=x          <=>  x=0 if r<3,  x=3 if r>=3 & r<6,  x=6 if r>=6
ok1(g,r,c,d)      <=>  !E[rw](row1(g,r)=rw & d in rw) &
                      !E[i](i in {0,1,2,3,4,5,6,7,8} & cell1(g,i,c)=d) &
                      !E[x,y](bb1(r)=x & bb1(c)=y & E[i,j](i in {0,1,2} & j in {0,1,2} & cell1(g,x+i,y+j)=d))
fits1(g,r,c)=cs   <=>  cs = {1,2,3,4,5,6,7,8,9} where [d](ok1(g,r,c,d))
csize1(cs)=x      <=>  |cs|=x

// ---- search ---------------------------------------------------------
// solveS: scan for a blank with <=1 candidates; place it (or fail on 0)
// and rescan; a full scan without one hands over to solveB
solveS1(g,r,c)=s  <=>  s=solveB1(g,0,0)    if r=9,
                      s=solveS1(g,r+1,0)  if r<9 & c=9,
                      s=solveS1(g,r,c+1)  if r<9 & c<9 & cell1(g,r,c)!=0,
                      s=solveS1(g,r,c+1)  if r<9 & c<9 & cell1(g,r,c)=0 & !E[cs,x](fits1(g,r,c)=cs & csize1(cs)=x & x<2),
                      E[d,g2](d in {1,2,3,4,5,6,7,8,9} & ok1(g,r,c,d) & put1(g,r,c,d)=g2 & s=solveS1(g2,0,0)) if r<9 & c<9 & cell1(g,r,c)=0 & E[cs,x](fits1(g,r,c)=cs & csize1(cs)=x & x<2)
// solveB: guess at the first blank; no blank at all means solved
solveB1(g,r,c)=s  <=>  s=g                if r=9,
                      s=solveB1(g,r+1,0)  if r<9 & c=9,
                      s=solveB1(g,r,c+1)  if r<9 & c<9 & cell1(g,r,c)!=0,
                      E[d,g2](d in {1,2,3,4,5,6,7,8,9} & ok1(g,r,c,d) & put1(g,r,c,d)=g2 & s=solveS1(g2,0,0)) if r<9 & c<9 & cell1(g,r,c)=0

// ==== copy 2 ====
// ---- access ---------------------------------------------------------
at2(l,i)=x        <=>  at i in l = x
row2(g,r)=rw      <=>  at r in g = rw
cell2(g,r,c)=x    <=>  x = at2(row2(g,r),c)

// ---- update (functional) -------------------------------------------
sel2(j,i,d,x)=y      <=>  y=d if j=i,  y=x if j!=i
putRow2(l,i,d)=rw    <=>  rw = [0,1,2,3,4,5,6,7,8] map [j](sel2(j,i,d,at2(l,j)))
rowsBefore2(g,r)=s   <=>  s=[]                                                       if r=0,
                         E[rw,p](row2(g,r-1)=rw & rowsBefore2(g,r-1)=p & s=p+[rw])    if r>0
rowsFrom2(g,r)=s     <=>  s=[]                                                       if r=9,
                         E[rw,q](row2(g,r)=rw & rowsFrom2(g,r+1)=q & s=[rw]+q)        if r<9
put2(g,r,c,d)=s      <=>  E[p,q](rowsBefore2(g,r)=p & rowsFrom2(g,r+1)=q &
                                E[rw,rw2](row2(g,r)=rw & putRow2(rw,c,d)=rw2 & s=p+[rw2]+q))

// ---- constraint: d allowed at (r,c)? bb = box base row/column --------
bb2(r)=x          <=>  x=0 if r<3,  x=3 if r>=3 & r<6,  x=6 if r>=6
ok2(g,r,c,d)      <=>  !E[rw](row2(g,r)=rw & d in rw) &
                      !E[i](i in {0,1,2,3,4,5,6,7,8} & cell2(g,i,c)=d) &
                      !E[x,y](bb2(r)=x & bb2(c)=y & E[i,j](i in {0,1,2} & j in {0,1,2} & cell2(g,x+i,y+j)=d))
fits2(g,r,c)=cs   <=>  cs = {1,2,3,4,5,6,7,8,9} where [d](ok2(g,r,c,d))
csize2(cs)=x      <=>  |cs|=x

// ---- search ---------------------------------------------------------
// solveS: scan for a blank with <=1 candidates; place it (or fail on 0)
// and rescan; a full scan without one hands over to solveB
solveS2(g,r,c)=s  <=>  s=solveB2(g,0,0)    if r=9,
                      s=solveS2(g,r+1,0)  if r<9 & c=9,
                      s=solveS2(g,r,c+1)  if r<9 & c<9 & cell2(g,r,c)!=0,
                      s=solveS2(g,r,c+1)  if r<9 & c<9 & cell2(g,r,c)=0 & !E[cs,x](fits2(g,r,c)=cs & csize2(cs)=x & x<2),
                      E[d,g2](d in {1,2,3,4,5,6,7,8,9} & ok2(g,r,c,d) & put2(g,r,c,d)=g2 & s=solveS2(g2,0,0)) if r<9 & c<9 & cell2(g,r,c)=0 & E[cs,x](fits2(g,r,c)=cs & csize2(cs)=x & x<2)
// solveB: guess at the first blank; no blank at all means solved
solveB2(g,r,c)=s  <=>  s=g                if r=9,
                      s=solveB2(g,r+1,0)  if r<9 & c=9,
                      s=solveB2(g,r,c+1)  if r<9 & c<9 & cell2(g,r,c)!=0,
                      E[d,g2](d in {1,2,3,4,5,6,7,8,9} & ok2(g,r,c,d) & put2(g,r,c,d)=g2 & s=solveS2(g2,0,0)) if r<9 & c<9 & cell2(g,r,c)=0

// ==== copy 3 ====
// ---- access ---------------------------------------------------------
at3(l,i)=x        <=>  at i in l = x
row3(g,r)=rw      <=>  at r in g = rw
cell3(g,r,c)=x    <=>  x = at3(row3(g,r),c)

// ---- update (functional) -------------------------------------------
sel3(j,i,d,x)=y      <=>  y=d if j=i,  y=x if j!=i
putRow3(l,i,d)=rw    <=>  rw = [0,1,2,3,4,5,6,7,8] map [j](sel3(j,i,d,at3(l,j)))
rowsBefore3(g,r)=s   <=>  s=[]                                                       if r=0,
                         E[rw,p](row3(g,r-1)=rw & rowsBefore3(g,r-1)=p & s=p+[rw])    if r>0
rowsFrom3(g,r)=s     <=>  s=[]                                                       if r=9,
                         E[rw,q](row3(g,r)=rw & rowsFrom3(g,r+1)=q & s=[rw]+q)        if r<9
put3(g,r,c,d)=s      <=>  E[p,q](rowsBefore3(g,r)=p & rowsFrom3(g,r+1)=q &
                                E[rw,rw2](row3(g,r)=rw & putRow3(rw,c,d)=rw2 & s=p+[rw2]+q))

// ---- constraint: d allowed at (r,c)? bb = box base row/column --------
bb3(r)=x          <=>  x=0 if r<3,  x=3 if r>=3 & r<6,  x=6 if r>=6
ok3(g,r,c,d)      <=>  !E[rw](row3(g,r)=rw & d in rw) &
                      !E[i](i in {0,1,2,3,4,5,6,7,8} & cell3(g,i,c)=d) &
                      !E[x,y](bb3(r)=x & bb3(c)=y & E[i,j](i in {0,1,2} & j in {0,1,2} & cell3(g,x+i,y+j)=d))
fits3(g,r,c)=cs   <=>  cs = {1,2,3,4,5,6,7,8,9} where [d](ok3(g,r,c,d))
csize3(cs)=x      <=>  |cs|=x

// ---- search ---------------------------------------------------------
// solveS: scan for a blank with <=1 candidates; place it (or fail on 0)
// and rescan; a full scan without one hands over to solveB
solveS3(g,r,c)=s  <=>  s=solveB3(g,0,0)    if r=9,
                      s=solveS3(g,r+1,0)  if r<9 & c=9,
                      s=solveS3(g,r,c+1)  if r<9 & c<9 & cell3(g,r,c)!=0,
                      s=solveS3(g,r,c+1)  if r<9 & c<9 & cell3(g,r,c)=0 & !E[cs,x](fits3(g,r,c)=cs & csize3(cs)=x & x<2),
                      E[d,g2](d in {1,2,3,4,5,6,7,8,9} & ok3(g,r,c,d) & put3(g,r,c,d)=g2 & s=solveS3(g2,0,0)) if r<9 & c<9 & cell3(g,r,c)=0 & E[cs,x](fits3(g,r,c)=cs & csize3(cs)=x & x<2)
// solveB: guess at the first blank; no blank at all means solved
solveB3(g,r,c)=s  <=>  s=g                if r=9,
                      s=solveB3(g,r+1,0)  if r<9 & c=9,
                      s=solveB3(g,r,c+1)  if r<9 & c<9 & cell3(g,r,c)!=0,
                      E[d,g2](d in {1,2,3,4,5,6,7,8,9} & ok3(g,r,c,d) & put3(g,r,c,d)=g2 & s=solveS3(g2,0,0)) if r<9 & c<9 & cell3(g,r,c)=0

// ==== copy 4 ====
// ---- access ---------------------------------------------------------
at4(l,i)=x        <=>  at i in l = x
row4(g,r)=rw      <=>  at r in g = rw
cell4(g,r,c)=x    <=>  x = at4(row4(g,r),c)

// ---- update (functional) -------------------------------------------
sel4(j,i,d,x)=y      <=>  y=d if j=i,  y=x if j!=i
putRow4(l,i,d)=rw    <=>  rw = [0,1,2,3,4,5,6,7,8] map [j](sel4(j,i,d,at4(l,j)))
rowsBefore4(g,r)=s   <=>  s=[]                                                       if r=0,
                         E[rw,p](row4(g,r-1)=rw & rowsBefore4(g,r-1)=p & s=p+[rw])    if r>0
rowsFrom4(g,r)=s     <=>  s=[]                                                       if r=9,
                         E[rw,q](row4(g,r)=rw & rowsFrom4(g,r+1)=q & s=[rw]+q)        if r<9
put4(g,r,c,d)=s      <=>  E[p,q](rowsBefore4(g,r)=p & rowsFrom4(g,r+1)=q &
                                E[rw,rw2](row4(g,r)=rw & putRow4(rw,c,d)=rw2 & s=p+[rw2]+q))

// ---- constraint: d allowed at (r,c)? bb = box base row/column --------
bb4(r)=x          <=>  x=0 if r<3,  x=3 if r>=3 & r<6,  x=6 if r>=6
ok4(g,r,c,d)      <=>  !E[rw](row4(g,r)=rw & d in rw) &
                      !E[i](i in {0,1,2,3,4,5,6,7,8} & cell4(g,i,c)=d) &
                      !E[x,y](bb4(r)=x & bb4(c)=y & E[i,j](i in {0,1,2} & j in {0,1,2} & cell4(g,x+i,y+j)=d))
fits4(g,r,c)=cs   <=>  cs = {1,2,3,4,5,6,7,8,9} where [d](ok4(g,r,c,d))
csize4(cs)=x      <=>  |cs|=x

// ---- search ---------------------------------------------------------
// solveS: scan for a blank with <=1 candidates; place it (or fail on 0)
// and rescan; a full scan without one hands over to solveB
solveS4(g,r,c)=s  <=>  s=solveB4(g,0,0)    if r=9,
                      s=solveS4(g,r+1,0)  if r<9 & c=9,
                      s=solveS4(g,r,c+1)  if r<9 & c<9 & cell4(g,r,c)!=0,
                      s=solveS4(g,r,c+1)  if r<9 & c<9 & cell4(g,r,c)=0 & !E[cs,x](fits4(g,r,c)=cs & csize4(cs)=x & x<2),
                      E[d,g2](d in {1,2,3,4,5,6,7,8,9} & ok4(g,r,c,d) & put4(g,r,c,d)=g2 & s=solveS4(g2,0,0)) if r<9 & c<9 & cell4(g,r,c)=0 & E[cs,x](fits4(g,r,c)=cs & csize4(cs)=x & x<2)
// solveB: guess at the first blank; no blank at all means solved
solveB4(g,r,c)=s  <=>  s=g                if r=9,
                      s=solveB4(g,r+1,0)  if r<9 & c=9,
                      s=solveB4(g,r,c+1)  if r<9 & c<9 & cell4(g,r,c)!=0,
                      E[d,g2](d in {1,2,3,4,5,6,7,8,9} & ok4(g,r,c,d) & put4(g,r,c,d)=g2 & s=solveS4(g2,0,0)) if r<9 & c<9 & cell4(g,r,c)=0

// ---- queries ---------------------------------------------------------
solveS1([[0,0,0,0,0,0,0,0,0],[4,5,6,7,8,9,1,2,3],[7,8,0,1,2,3,4,5,6],[2,3,1,5,6,4,8,9,7],[5,6,4,8,9,7,2,3,1],[8,9,7,2,3,1,5,6,4],[3,1,2,6,4,5,9,7,8],[6,4,5,9,7,8,3,1,2],[9,7,8,3,1,2,6,4,5]],0,0)=s ? [(s=[[1,2,3,4,5,6,7,8,9],[4,5,6,7,8,9,1,2,3],[7,8,9,1,2,3,4,5,6],[2,3,1,5,6,4,8,9,7],[5,6,4,8,9,7,2,3,1],[8,9,7,2,3,1,5,6,4],[3,1,2,6,4,5,9,7,8],[6,4,5,9,7,8,3,1,2],[9,7,8,3,1,2,6,4,5]])][..]
solveS2([[0,0,0,0,0,0,0,0,0],[4,5,6,7,8,9,1,2,3],[7,8,0,1,2,3,4,5,6],[2,3,1,5,6,4,8,9,7],[5,6,4,8,9,7,2,3,1],[8,9,7,2,3,1,5,6,4],[3,1,2,6,4,5,9,7,8],[6,4,5,9,7,8,3,1,2],[9,7,8,3,1,2,6,4,5]],0,0)=s ? [(s=[[1,2,3,4,5,6,7,8,9],[4,5,6,7,8,9,1,2,3],[7,8,9,1,2,3,4,5,6],[2,3,1,5,6,4,8,9,7],[5,6,4,8,9,7,2,3,1],[8,9,7,2,3,1,5,6,4],[3,1,2,6,4,5,9,7,8],[6,4,5,9,7,8,3,1,2],[9,7,8,3,1,2,6,4,5]])][..]
solveS3([[0,0,0,0,0,0,0,0,0],[4,5,6,7,8,9,1,2,3],[7,8,0,1,2,3,4,5,6],[2,3,1,5,6,4,8,9,7],[5,6,4,8,9,7,2,3,1],[8,9,7,2,3,1,5,6,4],[3,1,2,6,4,5,9,7,8],[6,4,5,9,7,8,3,1,2],[9,7,8,3,1,2,6,4,5]],0,0)=s ? [(s=[[1,2,3,4,5,6,7,8,9],[4,5,6,7,8,9,1,2,3],[7,8,9,1,2,3,4,5,6],[2,3,1,5,6,4,8,9,7],[5,6,4,8,9,7,2,3,1],[8,9,7,2,3,1,5,6,4],[3,1,2,6,4,5,9,7,8],[6,4,5,9,7,8,3,1,2],[9,7,8,3,1,2,6,4,5]])][..]
solveS4([[0,0,0,0,0,0,0,0,0],[4,5,6,7,8,9,1,2,3],[7,8,0,1,2,3,4,5,6],[2,3,1,5,6,4,8,9,7],[5,6,4,8,9,7,2,3,1],[8,9,7,2,3,1,5,6,4],[3,1,2,6,4,5,9,7,8],[6,4,5,9,7,8,3,1,2],[9,7,8,3,1,2,6,4,5]],0,0)=s ? [(s=[[1,2,3,4,5,6,7,8,9],[4,5,6,7,8,9,1,2,3],[7,8,9,1,2,3,4,5,6],[2,3,1,5,6,4,8,9,7],[5,6,4,8,9,7,2,3,1],[8,9,7,2,3,1,5,6,4],[3,1,2,6,4,5,9,7,8],[6,4,5,9,7,8,3,1,2],[9,7,8,3,1,2,6,4,5]])][..]
