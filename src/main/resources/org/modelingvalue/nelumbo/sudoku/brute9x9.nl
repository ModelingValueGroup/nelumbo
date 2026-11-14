import nelumbo.collections

// Naive brute-force sudoku solver: solve walks the grid row by row and tries every
// digit that fits at each blank (all solutions). Puzzles: examples/sudoku-brute-9x9.nl,
// unit tests: tests/sudokuBrute9x9Test.nl.
// Needs -DMAX_LOGIC_MEMOIZ=100000 for puzzles that make it search.

// ---- types ----------------------------------------------------------
Grid :: Object        // placeholder so we can name the nested list type
Integer             ::= at(<List<Integer>>,<Integer>),
                        cell(<List<List<Integer>>>,<Integer>,<Integer>),
                        sel(<Integer>,<Integer>,<Integer>,<Integer>)
Set<Integer>        ::= boxRows(<Integer>), boxCols(<Integer>)
List<Integer>       ::= row(<List<List<Integer>>>,<Integer>),
                        putRow(<List<Integer>>,<Integer>,<Integer>)
List<List<Integer>> ::= put(<List<List<Integer>>>,<Integer>,<Integer>,<Integer>),
                        solve(<List<List<Integer>>>,<Integer>,<Integer>),
                        sudoku(<List<List<Integer>>>)
Boolean             ::= ok(<List<List<Integer>>>,<Integer>,<Integer>,<Integer>)

List<List<Integer>> g, g2, s
List<Integer>       l, rw
Integer             r, c, d, i, j
Set<Integer>        bs, cs

// ---- access ---------------------------------------------------------
at(l,i)          ==  at i in l
row(g,r)         ==  at r in g
cell(g,r,c)      ==  at(row(g,r),c)

// ---- update (functional) -------------------------------------------
putRow(l,i,d)          ==  set d at i in l
put(g,r,c,d)           ==  set putRow(row(g,r),c,d) at r in g

// ---- 9x9 boxes are 3 rows x 3 cols ---------------------------------
boxRows(r)       ==  {0,1,2} if r<3,  {3,4,5} if r>=3 & r<6,  {6,7,8} if r>=6
boxCols(c)       ==  {0,1,2} if c<3,  {3,4,5} if c>=3 & c<6,  {6,7,8} if c>=6

// ---- constraint -----------------------------------------------------
ok(g,r,c,d)      <=>  !E[rw](row(g,r)=rw & d in rw) &
                      !E[i](i in {0,1,2,3,4,5,6,7,8} & cell(g,i,c)=d) &
                      !E[bs,cs](boxRows(r)=bs & boxCols(c)=cs & E[i,j](i in bs & j in cs & cell(g,i,j)=d))

// ---- search: scan cells row-major, branch on blanks (0) --------------
solve(g,r,c)=s   <=>  s=g                                          if r=9,
                      s=solve(g,r+1,0)                             if r<9 & c=9,
                      s=solve(g,r,c+1)                             if r<9 & c<9 & cell(g,r,c)!=0,
                      E[d,g2](d in {1,2,3,4,5,6,7,8,9} & ok(g,r,c,d) &
                              put(g,r,c,d)=g2 & s=solve(g2,r,c+1))  if r<9 & c<9 & cell(g,r,c)=0

sudoku(g)        ==  solve(g,0,0)

