import nelumbo.collections

// 4x4 debug copy of smart9x9.nl: singles-first solver over digits 1-4 with
// 2x2 boxes. Small enough to trace by hand. See smart9x9.nl for the 9x9
// original and the engine war stories; the bounds guards / nested-E /
// guard-pruning shapes are kept identical here because they are what the engine
// is proven to accept.
//
// scanF walks the grid looking for a forced blank (at most one fitting digit):
// exactly one is a forced move (placed without branching), zero is a dead end.
// Only when no forced blank exists solveBX guesses at the first blank.
// Puzzles: examples/sudoku-smart-4x4-easy.nl.

// ---- types ----------------------------------------------------------
Grid :: Object        // placeholder so we can name the nested list type
Integer             ::= at(<List<Integer>>,<Integer>),
                        cell(<List<List<Integer>>>,<Integer>,<Integer>),
                        bb(<Integer>)
Boolean             ::= ok(<List<List<Integer>>>,<Integer>,<Integer>,<Integer>),
                        forced(<List<List<Integer>>>,<Integer>,<Integer>)
List<Integer>       ::= row(<List<List<Integer>>>,<Integer>),
                        putRow(<List<Integer>>,<Integer>,<Integer>)
List<List<Integer>> ::= put(<List<List<Integer>>>,<Integer>,<Integer>,<Integer>)

List<List<Integer>> g, g2, s
List<Integer>       l, rw
Integer             r, c, d, x, y, i, j

// ---- access (bounds-guarded: the engine probes rule guards speculatively,
// ---- an unguarded out-of-range index is an error, not false) ---------
at(l,i)          ==  at i in l       if i>=0 & i<4
row(g,r)         ==  at r in g       if r>=0 & r<4
cell(g,r,c)      ==  at(row(g,r),c)

// ---- update (functional) -------------------------------------------
putRow(l,i,d)       ==  set d at i in l
put(g,r,c,d)        ==  set putRow(row(g,r),c,d) at r in g

// ---- constraint: d allowed at (r,c)? bb = box base row/column --------
bb(r)            ==  0 if r<2,  2 if r>=2
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
                        sudoku(<List<List<Integer>>>)
scanF(g,r,c)     ==  solveBX(g,0,0)   if r=4,
                     scanF(g,r+1,0)   if r<4 & c=4,
                     scanF(g,r,c+1)   if r<4 & c<4 & cell(g,r,c)!=0,
                     scanB(g,r,c)     if r<4 & c<4 & cell(g,r,c)=0
scanB(g,r,c)=s   <=>  s=scanF(g,r,c+1)   if E[x,y](x in {1,2,3} & y in {2,3,4} & x<y &
                                                   ok(g,r,c,x) & ok(g,r,c,y)),
                      E[d,g2](d in {1,2,3,4} & ok(g,r,c,d) &
                              put(g,r,c,d)=g2 & s=scanF(g2,0,0))  if !E[x,y](x in {1,2,3} & y in {2,3,4} & x<y &
                                                   ok(g,r,c,x) & ok(g,r,c,y))
solveBX(g,r,c)=s <=>  s=g                 if r=4,
                      s=solveBX(g,r+1,0)  if r<4 & c=4,
                      s=solveBX(g,r,c+1)  if r<4 & c<4 & cell(g,r,c)!=0,
                      E[d,g2](d in {1,2,3,4} & ok(g,r,c,d) &
                              put(g,r,c,d)=g2 & s=scanF(g2,0,0))  if r<4 & c<4 & cell(g,r,c)=0

sudoku(g)            ==  scanF(g,0,0)

