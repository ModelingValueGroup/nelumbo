import nelumbo.collections

// 4x4 debug copy of brute9x9.nl: naive brute-force solver over digits 1-4 with
// 2x2 boxes. Small enough to trace by hand. Puzzles: examples/sudoku-brute-4x4.nl,
// unit tests: tests/sudokuBrute4x4Test.nl.

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

// ---- 4x4 boxes are 2 rows x 2 cols ---------------------------------
boxRows(r)       ==  {0,1} if r<2,  {2,3} if r>=2
boxCols(c)       ==  {0,1} if c<2,  {2,3} if c>=2

// ---- constraint -----------------------------------------------------
ok(g,r,c,d)      <=>  !E[rw](row(g,r)=rw & d in rw) &
                      !E[i](i in {0,1,2,3} & cell(g,i,c)=d) &
                      !E[bs,cs](boxRows(r)=bs & boxCols(c)=cs & E[i,j](i in bs & j in cs & cell(g,i,j)=d))

// ---- search: scan cells row-major, branch on blanks (0) --------------
solve(g,r,c)=s   <=>  s=g                                          if r=4,
                      s=solve(g,r+1,0)                             if r<4 & c=4,
                      s=solve(g,r,c+1)                             if r<4 & c<4 & cell(g,r,c)!=0,
                      E[d,g2](d in {1,2,3,4} & ok(g,r,c,d) &
                              put(g,r,c,d)=g2 & s=solve(g2,r,c+1))  if r<4 & c<4 & cell(g,r,c)=0

sudoku(g)        ==  solve(g,0,0)

