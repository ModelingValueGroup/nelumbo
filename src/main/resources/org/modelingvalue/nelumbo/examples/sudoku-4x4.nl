import nelumbo.collections

// 4x4 debug copy of sudoku-9x9.nl: naive brute-force solver over digits 1-4 with
// 2x2 boxes. Small enough to trace by hand. See sudoku-9x9.nl for the 9x9 original.

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
putRow(l,i,d)=rw       <=>  rw = [0,1,2,3] map [j](sel(j,i,d,at(l,j)))
rowsBefore(g,r)=s      <=>  s=[]                                                    if r=0,
                            E[rw,p](row(g,r-1)=rw & rowsBefore(g,r-1)=p & s=p+[rw])    if r>0
rowsFrom(g,r)=s        <=>  s=[]                                                    if r=4,
                            E[rw,q](row(g,r)=rw & rowsFrom(g,r+1)=q & s=[rw]+q)        if r<4
put(g,r,c,d)=s         <=>  E[p,q](rowsBefore(g,r)=p & rowsFrom(g,r+1)=q &
                                   E[rw,rw2](row(g,r)=rw & putRow(rw,c,d)=rw2 & s=p+[rw2]+q))

// ---- 4x4 boxes are 2 rows x 2 cols ---------------------------------
boxRows(r)=bs    <=>  bs={0,1} if r<2,  bs={2,3} if r>=2
boxCols(c)=bs    <=>  bs={0,1} if c<2,  bs={2,3} if c>=2

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

sudoku(g)=s      <=>  s=solve(g,0,0)

// ---- tests ----------------------------------------------------------
at([4,1,2,3],1)=x                            ? [(x=1)][..]
putRow([1,0,3,4],1,2)=l                      ? [(l=[1,2,3,4])][..]
boxRows(3)=bs                                ? [(bs={2,3})][..]
boxCols(1)=bs                                ? [(bs={0,1})][..]
ok([[1,2,3,4],[0,0,0,0],[0,0,0,0],[0,0,0,0]],1,0,3) ? [()][]
ok([[1,2,3,4],[0,0,0,0],[0,0,0,0],[0,0,0,0]],1,0,1) ? [][()]
ok([[1,2,3,4],[0,0,0,0],[0,0,0,0],[0,0,0,0]],3,0,1) ? [][()]

// one blank per row, unique solution
sudoku([[0,2,3,4],[3,4,1,0],[2,0,4,3],[4,3,0,1]])=s ? [(s=[[1,2,3,4],[3,4,1,2],[2,1,4,3],[4,3,2,1]]),..][..]

// first row empty plus one blank per remaining row, unique solution
sudoku([[0,0,0,0],[0,4,1,2],[2,1,0,3],[4,3,2,0]])=s ? [(s=[[1,2,3,4],[3,4,1,2],[2,1,4,3],[4,3,2,1]]),..][..]

// a 'real' puzzle, formatted in a more intuitive way:
sudoku([
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
    [2,1,/**/3,4]]),..][..]
