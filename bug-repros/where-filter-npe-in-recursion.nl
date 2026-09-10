// Bug: a `where` set-filter whose lambda calls a user Boolean rule (ok) NPEs
// the whole evaluation once it runs inside deep recursion:
//   NullPointerException: ... InferResult.allFalsehoods() is null
//   at InferResult.isTrueCC (InferResult.java:72)
//   at lang.Lambda.isComplete/resolve/test (Lambda.java:142/127/110)
//   at collections.Collections.setFilter (Collections.java:157)
// The same fits() evaluates correctly standalone (see the fits-style queries
// in examples/sudoku-9x9-smart.nl's history); only the recursive context crashes it.
// This file is a singles-first sudoku solver whose scan guard uses
// fits(g,r,c) = {1..9} where [d](ok(g,r,c,d)); solving the 64-clue puzzle
// below crashes. The shipped examples/sudoku-9x9-smart.nl avoids `where` entirely
// (pair-check over ok) for this reason.
// Correct: the solved grid. Actual: NullPointerException.
// Found 2026-09-10 while writing examples/sudoku-9x9-smart.nl.
import nelumbo.collections

// ---- types ----------------------------------------------------------
Grid :: Object        // placeholder so we can name the nested list type
Integer             ::= at(<List<Integer>>,<Integer>),
                        cell(<List<List<Integer>>>,<Integer>,<Integer>),
                        sel(<Integer>,<Integer>,<Integer>,<Integer>),
                        bb(<Integer>),
                        csize(<Set<Integer>>)
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
Integer             r, c, d, x, y, i, j, n

// ---- access (bounds-guarded: the engine probes rule guards speculatively,
// ---- an unguarded out-of-range pos crashes instead of failing) -------
at(l,i)=x        <=>  x pos l = i   if i>=0 & i<9
row(g,r)=rw      <=>  rw pos g = r  if r>=0 & r<9
cell(g,r,c)=x    <=>  x = at(row(g,r),c)

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

List<List<Integer>> ::= solveV(<List<List<Integer>>>,<Integer>,<Integer>), solveBV(<List<List<Integer>>>,<Integer>,<Integer>)
solveV(g,r,c)=s  <=>  s=solveBV(g,0,0)   if r=9,
                      s=solveV(g,r+1,0)  if r<9 & c=9,
                      s=solveV(g,r,c+1)  if r<9 & c<9 & !E[cs,x](cell(g,r,c)=0 & fits(g,r,c)=cs & csize(cs)=x & x<2),
                      E[d,g2](E[cs,x](cell(g,r,c)=0 & fits(g,r,c)=cs & csize(cs)=x & x<2) &
                              d in {1,2,3,4,5,6,7,8,9} & ok(g,r,c,d) &
                              put(g,r,c,d)=g2 & s=solveV(g2,0,0))
solveBV(g,r,c)=s <=>  s=g                 if r=9,
                      s=solveBV(g,r+1,0)  if r<9 & c=9,
                      s=solveBV(g,r,c+1)  if r<9 & c<9 & cell(g,r,c)!=0,
                      E[d,g2](d in {1,2,3,4,5,6,7,8,9} & ok(g,r,c,d) &
                              put(g,r,c,d)=g2 & s=solveV(g2,0,0))  if r<9 & c<9 & cell(g,r,c)=0

solveV([[0,2,3,4,5,6,7,8,9],[4,5,6,0,8,9,1,2,3],[7,8,9,1,2,3,0,5,6],[2,0,1,5,6,4,8,9,7],[5,6,4,8,0,7,2,3,1],[8,9,7,2,3,1,5,0,4],[3,1,0,6,4,5,9,7,8],[6,4,5,9,7,0,3,1,2],[9,7,8,3,1,2,6,4,0]],0,0)=s ? [(s=[[1,2,3,4,5,6,7,8,9],[4,5,6,7,8,9,1,2,3],[7,8,9,1,2,3,4,5,6],[2,3,1,5,6,4,8,9,7],[5,6,4,8,9,7,2,3,1],[8,9,7,2,3,1,5,6,4],[3,1,2,6,4,5,9,7,8],[6,4,5,9,7,8,3,1,2],[9,7,8,3,1,2,6,4,5]]),..][..]
solveV([[0,0,0,0,0,0,0,0,0],[0,5,6,7,8,9,1,2,3],[7,0,9,1,2,3,4,5,6],[2,3,0,5,6,4,8,9,7],[5,6,4,0,9,7,2,3,1],[8,9,7,2,0,1,5,6,4],[3,1,2,6,4,0,9,7,8],[6,4,5,9,7,8,0,1,2],[9,7,8,3,1,2,6,0,5]],0,0)=s ? [(s=[[1,2,3,4,5,6,7,8,9],[4,5,6,7,8,9,1,2,3],[7,8,9,1,2,3,4,5,6],[2,3,1,5,6,4,8,9,7],[5,6,4,8,9,7,2,3,1],[8,9,7,2,3,1,5,6,4],[3,1,2,6,4,5,9,7,8],[6,4,5,9,7,8,3,1,2],[9,7,8,3,1,2,6,4,5]]),..][..]
