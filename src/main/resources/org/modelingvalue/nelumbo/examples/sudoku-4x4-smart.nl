import nelumbo.collections

// 4x4 debug copy of sudoku-9x9-smart.nl: singles-first solver over digits 1-4 with
// 2x2 boxes. Small enough to trace by hand. See sudoku-9x9-smart.nl for the 9x9
// original and the engine war stories; the bounds guards / nested-E /
// guard-pruning shapes are kept identical here because they are what the engine
// is proven to accept.
//
// scanF walks the grid looking for a forced blank (at most one fitting digit):
// exactly one is a forced move (placed without branching), zero is a dead end.
// Only when no forced blank exists solveBX guesses at the first blank.

// ---- types ----------------------------------------------------------
Grid :: Object        // placeholder so we can name the nested list type
Integer             ::= at(<List<Integer>>,<Integer>),
                        cell(<List<List<Integer>>>,<Integer>,<Integer>),
                        sel(<Integer>,<Integer>,<Integer>,<Integer>),
                        bb(<Integer>)
Boolean             ::= ok(<List<List<Integer>>>,<Integer>,<Integer>,<Integer>),
                        forced(<List<List<Integer>>>,<Integer>,<Integer>)
List<Integer>       ::= row(<List<List<Integer>>>,<Integer>),
                        putRow(<List<Integer>>,<Integer>,<Integer>)
List<List<Integer>> ::= rowsBefore(<List<List<Integer>>>,<Integer>),
                        rowsFrom(<List<List<Integer>>>,<Integer>),
                        put(<List<List<Integer>>>,<Integer>,<Integer>,<Integer>)

List<List<Integer>> g, g2, s, p, q
List<Integer>       l, rw, rw2
Integer             r, c, d, x, y, i, j

// ---- access (bounds-guarded: the engine probes rule guards speculatively,
// ---- an unguarded out-of-range pos crashes instead of failing) -------
at(l,i)=x        <=>  x pos l = i   if i>=0 & i<4
row(g,r)=rw      <=>  rw pos g = r  if r>=0 & r<4
cell(g,r,c)=x    <=>  x = at(row(g,r),c)

// ---- update (functional) -------------------------------------------
sel(j,i,d,x)=y      <=>  y=d if j=i,  y=x if j!=i
putRow(l,i,d)=rw    <=>  rw = [0,1,2,3] map [j](sel(j,i,d,at(l,j)))
rowsBefore(g,r)=s   <=>  s=[]                                                       if r=0,
                         E[rw,p](row(g,r-1)=rw & rowsBefore(g,r-1)=p & s=p+[rw])    if r>0
rowsFrom(g,r)=s     <=>  s=[]                                                       if r=4,
                         E[rw,q](row(g,r)=rw & rowsFrom(g,r+1)=q & s=[rw]+q)        if r<4
put(g,r,c,d)=s      <=>  E[p,q](rowsBefore(g,r)=p & rowsFrom(g,r+1)=q &
                                E[rw,rw2](row(g,r)=rw & putRow(rw,c,d)=rw2 & s=p+[rw2]+q))

// ---- constraint: d allowed at (r,c)? bb = box base row/column --------
bb(r)=x          <=>  x=0 if r<2,  x=2 if r>=2
ok(g,r,c,d)      <=>  !E[rw](row(g,r)=rw & d in rw) &
                      !E[i](i in {0,1,2,3} & cell(g,i,c)=d) &
                      !E[x,y](bb(r)=x & bb(c)=y & E[i,j](i in {0,1} & j in {0,1} & cell(g,x+i,y+j)=d))

// ---- forced: a blank where at most one digit fits (no two distinct
// ---- fitting digits) -------------------------------------------------
forced(g,r,c)    <=>  cell(g,r,c)=0 &
                      !E[x,y](x in {1,2,3} & y in {2,3,4} & x<y & ok(g,r,c,x) & ok(g,r,c,y))

// ---- search: scanF walks filled cells, scanB decides each blank (scan on
// ---- vs forced move), solveBX guesses at the first blank when no blank is
// ---- forced --------------------------------------------------------
List<List<Integer>> ::= scanF(<List<List<Integer>>>,<Integer>,<Integer>),
                        scanB(<List<List<Integer>>>,<Integer>,<Integer>),
                        solveBX(<List<List<Integer>>>,<Integer>,<Integer>),
                        sudokuSmart(<List<List<Integer>>>)
scanF(g,r,c)=s   <=>  s=solveBX(g,0,0)   if r=4,
                      s=scanF(g,r+1,0)   if r<4 & c=4,
                      s=scanF(g,r,c+1)   if r<4 & c<4 & cell(g,r,c)!=0,
                      s=scanB(g,r,c)     if r<4 & c<4 & cell(g,r,c)=0
scanB(g,r,c)=s   <=>  s=scanF(g,r,c+1)   if E[x,y](x in {1,2,3} & y in {2,3,4} & x<y &
                                                   ok(g,r,c,x) & ok(g,r,c,y)),
                      E[d,g2](forced(g,r,c) & d in {1,2,3,4} & ok(g,r,c,d) &
                              put(g,r,c,d)=g2 & s=scanF(g2,0,0))
solveBX(g,r,c)=s <=>  s=g                 if r=4,
                      s=solveBX(g,r+1,0)  if r<4 & c=4,
                      s=solveBX(g,r,c+1)  if r<4 & c<4 & cell(g,r,c)!=0,
                      E[d,g2](d in {1,2,3,4} & ok(g,r,c,d) &
                              put(g,r,c,d)=g2 & s=scanF(g2,0,0))  if r<4 & c<4 & cell(g,r,c)=0

sudokuSmart(g)=s     <=>  s=scanF(g,0,0)

// ---- puzzles ------------------------------------------------------
// one blank per row, unique solution
//sudokuSmart([[0,2,3,4],[3,4,1,0],[2,0,4,3],[4,3,0,1]])=s ? [(s=[[1,2,3,4],[3,4,1,2],[2,1,4,3],[4,3,2,1]]),..][..]
// first row empty plus one blank per remaining row, unique solution
//sudokuSmart([[0,0,0,0],[0,4,1,2],[2,1,0,3],[4,3,2,0]])=s ? [(s=[[1,2,3,4],[3,4,1,2],[2,1,4,3],[4,3,2,1]]),..][..]
// a 'real' puzzle, needs guessing (unique solution)
sudokuSmart([
    [1,0,/**/0,0],
    [0,0,/**/1,0],
    //-----------
    [0,3,/**/0,0],
    [0,0,/**/0,4]])=s ?
 [(s=[
    [1,2,/**/4,3],
    [3,4,/**/1,2],
    //-----------
    [4,3,/**/2,1],
    [2,1,/**/3,4]])][..]
