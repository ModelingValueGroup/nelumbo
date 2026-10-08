// Regression test (fixed 2026-10-06; was bugs/four-var-quantifier-crash.nl).
// E/!E with more than three variables must behave the same in every position:
// in a rule body and in a plain query. (Was: quantifiers were built on
// Lambda1..3, so max 3 variables. In a plain query `E[a,b,i,j](...)` was a
// parse error ("Unexpected token ',', expected ']'"), but the SAME construct
// inside a rule body parsed fine and then crashed the whole evaluation at
// runtime: IllegalArgumentException "Error during argument extraction for
// {Lambda3<...>,Literal}" (lang/Lambda argument extraction). The workaround
// was nesting quantifiers (E[x,y](... & E[i,j](...))). Found 2026-09-10 while
// writing examples/sudoku-9x9-smart.nl's box check; fixed by adding
// Lambda4..Lambda6 to logic/logic.nl, so lambdas and quantifiers take up to 6
// variables now. A 7th is a parse error in a query AND in a rule body - not
// asserted here, a parse error cannot be an expectation.)
import nelumbo.collections

R :: Object
Boolean ::= chk(<List<List<Integer>>>,<Integer>,<Integer>,<Integer>),
            chk6(<List<List<Integer>>>,<Integer>)
Integer ::= cell(<List<List<Integer>>>,<Integer>,<Integer>), at(<List<Integer>>,<Integer>)
List<Integer> ::= row(<List<List<Integer>>>,<Integer>)

List<List<Integer>> g
List<Integer>       l, rw
Integer             r, c, d, x, y, i, j, a, b

at(l,i)=x     <=>  x pos l = i
row(g,r)=rw   <=>  rw pos g = r
cell(g,r,c)=x <=>  x = at(row(g,r),c)
chk(g,r,c,d)  <=>  !E[x,y,i,j](x in {0} & y in {0} & i in {0,1,2} & j in {0,1,2} & cell(g,x+i,y+j)=d)
chk6(g,d)     <=>  !E[x,y,i,j,a,b](x in {0} & y in {0} & i in {0,1} & j in {0,1} & a in {0,1} & b in {0,1} & cell(g,x+i+a,y+j+b)=d)

// 4 variables in a rule body: 77 appears nowhere in the grid, 5 does (was the runtime crash)
chk([[1,2,3],[4,5,6],[7,8,9]],0,0,77) ? [()][]
chk([[1,2,3],[4,5,6],[7,8,9]],0,0,5)  ? [][()]

// 4 variables in a plain query (was the parse error)
E[x,y,i,j](x in {0} & y in {0} & i in {0,1,2} & j in {0,1,2} & cell([[1,2,3],[4,5,6],[7,8,9]],x+i,y+j)=5)   ? [()][]
!E[x,y,i,j](x in {0} & y in {0} & i in {0,1,2} & j in {0,1,2} & cell([[1,2,3],[4,5,6],[7,8,9]],x+i,y+j)=77) ? [()][]

// the new maximum, 6 variables, in both positions
chk6([[1,2,3],[4,5,6],[7,8,9]],77) ? [()][]
chk6([[1,2,3],[4,5,6],[7,8,9]],9)  ? [][()]
E[x,y,i,j,a,b](x in {0} & y in {0} & i in {0,1} & j in {0,1} & a in {0,1} & b in {0,1} & cell([[1,2,3],[4,5,6],[7,8,9]],x+i+a,y+j+b)=9) ? [()][]
