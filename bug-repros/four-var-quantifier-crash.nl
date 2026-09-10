// Bug: E/!E with four variables is handled inconsistently. In a plain query
// `E[a,b,i,j](...)` is a parse error ("Unexpected token ',', expected ']'" -
// quantifiers are built on Lambda1..3, so max 3 variables). But the SAME
// construct inside a rule body parses fine and then crashes the whole
// evaluation at runtime: IllegalArgumentException "Error during argument
// extraction for {Lambda3<...>,Literal}" (lang/Lambda argument extraction).
// Correct: either support 4+ variables or reject them at parse time in every
// position. Actual: the query below crashes the run.
// Workaround: nest quantifiers (E[x,y](... & E[i,j](...))).
// Found 2026-09-10 while writing examples/sudoku2.nl's box check.
import nelumbo.collections

R :: Object
Boolean ::= chk(<List<List<Integer>>>,<Integer>,<Integer>,<Integer>)
Integer ::= cell(<List<List<Integer>>>,<Integer>,<Integer>), at(<List<Integer>>,<Integer>)
List<Integer> ::= row(<List<List<Integer>>>,<Integer>)

List<List<Integer>> g
List<Integer>       l, rw
Integer             r, c, d, x, y, i, j

at(l,i)=x     <=>  x pos l = i
row(g,r)=rw   <=>  rw pos g = r
cell(g,r,c)=x <=>  x = at(row(g,r),c)
// 77 appears nowhere in the grid, so chk must be true
chk(g,r,c,d)  <=>  !E[x,y,i,j](x in {0} & y in {0} & i in {0,1,2} & j in {0,1,2} & cell(g,x+i,y+j)=d)

chk([[1,2,3],[4,5,6],[7,8,9]],0,0,77) ? [()][]
