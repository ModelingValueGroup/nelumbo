import nelumbo.collections

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
at(l,i)=x        <=>  x pos l = i
row(g,r)=rw      <=>  rw pos g = r
cell(g,r,c)=x    <=>  x = at(row(g,r),c)

// ---- update (functional) -------------------------------------------
sel(j,i,d,x)=y         <=>  y=d if j=i,  y=x if j!=i
putRow(l,i,d)=rw       <=>  rw = [0,1,2,3,4,5,6,7,8] map [j](sel(j,i,d,at(l,j)))
rowsBefore(g,r)=s      <=>  s=[]                                                    if r=0,
                            E[rw,p](row(g,r-1)=rw & rowsBefore(g,r-1)=p & s=p+[rw])    if r>0
rowsFrom(g,r)=s        <=>  s=[]                                                    if r=9,
                            E[rw,q](row(g,r)=rw & rowsFrom(g,r+1)=q & s=[rw]+q)        if r<9
put(g,r,c,d)=s         <=>  E[p,q](rowsBefore(g,r)=p & rowsFrom(g,r+1)=q &
                                   E[rw,rw2](row(g,r)=rw & putRow(rw,c,d)=rw2 & s=p+[rw2]+q))

// ---- 9x9 boxes are 3 rows x 3 cols ---------------------------------
boxRows(r)=bs    <=>  bs={0,1,2} if r<3,  bs={3,4,5} if r>=3 & r<6,  bs={6,7,8} if r>=6
boxCols(c)=bs    <=>  bs={0,1,2} if c<3,  bs={3,4,5} if c>=3 & c<6,  bs={6,7,8} if c>=6

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

sudoku(g)=s      <=>  s=solve(g,0,0)

// ---- tests ----------------------------------------------------------
at([4,5,6],1)=x                              ? [(x=5)][..]
putRow([1,0,3,0,5,6,7,0,9],1,8)=l            ? [(l=[1,8,3,0,5,6,7,0,9])][..]
boxRows(3)=bs                                ? [(bs={3,4,5})][..]
boxCols(7)=bs                                ? [(bs={6,7,8})][..]
ok([[1,2,3,4,5,6,7,8,9],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0]],1,0,4) ? [()][]
ok([[1,2,3,4,5,6,7,8,9],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0]],1,0,1) ? [][()]
ok([[1,2,3,4,5,6,7,8,9],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0]],8,0,1) ? [][()]

// 72 clues, one blank per row/column/box, unique solution
sudoku([[0,2,3,4,5,6,7,8,9],[4,5,6,0,8,9,1,2,3],[7,8,9,1,2,3,0,5,6],[2,0,1,5,6,4,8,9,7],[5,6,4,8,0,7,2,3,1],[8,9,7,2,3,1,5,0,4],[3,1,0,6,4,5,9,7,8],[6,4,5,9,7,0,3,1,2],[9,7,8,3,1,2,6,4,0]])=s ? [(s=[[1,2,3,4,5,6,7,8,9],[4,5,6,7,8,9,1,2,3],[7,8,9,1,2,3,4,5,6],[2,3,1,5,6,4,8,9,7],[5,6,4,8,9,7,2,3,1],[8,9,7,2,3,1,5,6,4],[3,1,2,6,4,5,9,7,8],[6,4,5,9,7,8,3,1,2],[9,7,8,3,1,2,6,4,5]]),..][..]

// 64 clues: first row empty plus one blank per remaining row, unique solution
sudoku([[0,0,0,0,0,0,0,0,0],[0,5,6,7,8,9,1,2,3],[7,0,9,1,2,3,4,5,6],[2,3,0,5,6,4,8,9,7],[5,6,4,0,9,7,2,3,1],[8,9,7,2,0,1,5,6,4],[3,1,2,6,4,0,9,7,8],[6,4,5,9,7,8,0,1,2],[9,7,8,3,1,2,6,0,5]])=s ? [(s=[[1,2,3,4,5,6,7,8,9],[4,5,6,7,8,9,1,2,3],[7,8,9,1,2,3,4,5,6],[2,3,1,5,6,4,8,9,7],[5,6,4,8,9,7,2,3,1],[8,9,7,2,3,1,5,6,4],[3,1,2,6,4,5,9,7,8],[6,4,5,9,7,8,3,1,2],[9,7,8,3,1,2,6,4,5]]),..][..]
