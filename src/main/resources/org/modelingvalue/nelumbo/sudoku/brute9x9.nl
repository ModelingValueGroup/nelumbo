import nelumbo.collections

// Naive brute-force sudoku solver: solve walks the grid row by row and tries every
// digit that fits at each blank (all solutions). Puzzles: examples/sudoku-brute-9x9-easy.nl,
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
List<List<Integer>> ::= rowsBefore(<List<List<Integer>>>,<Integer>),
                        rowsFrom(<List<List<Integer>>>,<Integer>),
                        put(<List<List<Integer>>>,<Integer>,<Integer>,<Integer>),
                        solve(<List<List<Integer>>>,<Integer>,<Integer>),
                        sudoku(<List<List<Integer>>>)
Boolean             ::= ok(<List<List<Integer>>>,<Integer>,<Integer>,<Integer>)

List<List<Integer>> g, g2, s, p, q
List<Integer>       l, rw, rw2
Integer             r, c, d, x, y, i, j
Set<Integer>        bs, cs

// ---- access ---------------------------------------------------------
at(l,i)=x        <=>  at i in l = x
row(g,r)=rw      <=>  at r in g = rw
cell(g,r,c)      ==  at(row(g,r),c)

// ---- update (functional) -------------------------------------------
sel(j,i,d,x)           ==  d if j=i,  x if j!=i
putRow(l,i,d)          ==  [0,1,2,3,4,5,6,7,8] map [j](sel(j,i,d,at(l,j)))
rowsBefore(g,r)=s      <=>  s=[]                                                    if r=0,
                            E[rw,p](row(g,r-1)=rw & rowsBefore(g,r-1)=p & s=p+[rw])    if r>0
rowsFrom(g,r)=s        <=>  s=[]                                                    if r=9,
                            E[rw,q](row(g,r)=rw & rowsFrom(g,r+1)=q & s=[rw]+q)        if r<9
put(g,r,c,d)=s         <=>  E[p,q](rowsBefore(g,r)=p & rowsFrom(g,r+1)=q &
                                   E[rw,rw2](row(g,r)=rw & putRow(rw,c,d)=rw2 & s=p+[rw2]+q))

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

