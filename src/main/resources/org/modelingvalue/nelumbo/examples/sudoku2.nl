import nelumbo.collections

// Faster sudoku solver than sudoku.nl: singles-first search.
// sudoku.nl branches at the first blank with up to 9 digits, which explodes on
// puzzles that need guessing. Here scanF walks the grid looking for a forced
// blank (at most one fitting digit): exactly one is a forced move (placed
// without branching), zero is a dead end (the placing E fails, backtracking
// immediately). Only when no forced blank exists solveBX guesses at the first
// blank. The "real" puzzle of sudoku.nl (2+ hours there) solves here as a
// chain of 41 forced moves without any backtracking, in seconds.
//
// Engine notes (hard-won, each verified in bug-repros/*.nl - see the
// 2026-09-10 section of bug-repros/README.md): no `where`-filters in the
// recursion (setFilter over an ok-lambda NPEs in deep recursion), so "forced
// cell" is a pure E/!E pair-check over ok; E takes at most 3 variables (a
// 4th parses in rule bodies and then crashes the run - nest E's instead);
// at/row are bounds-guarded because guards are probed speculatively and an
// out-of-range pos crashes; an alternative's `if` guard must never sit
// alone on a continuation line (it is silently dropped - break only inside
// unbalanced parens); and results can depend on unrelated file content and
// on run-to-run scheduling, so the rule shapes here stick closely to the
// ones sudoku.nl proves out (guard-pruned alternatives, direct recursion).

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

// ---- forced: a blank where at most one digit fits (no two distinct
// ---- fitting digits) -------------------------------------------------
forced(g,r,c)    <=>  cell(g,r,c)=0 &
                      !E[x,y](x in {1,2,3,4,5,6,7,8} & y in {2,3,4,5,6,7,8,9} & x<y & ok(g,r,c,x) & ok(g,r,c,y))

// ---- search: scanF walks filled cells, scanB decides each blank (scan on
// ---- vs forced move), solveBX guesses at the first blank when no blank is
// ---- forced. scanB's guard pair-check is written verbatim like forced's
// ---- inner E: during development, near-identical variants of the same
// ---- check correlated with inconsistent results (see bug-repros) -------
List<List<Integer>> ::= scanF(<List<List<Integer>>>,<Integer>,<Integer>),
                        scanB(<List<List<Integer>>>,<Integer>,<Integer>),
                        solveBX(<List<List<Integer>>>,<Integer>,<Integer>),
                        sudoku2(<List<List<Integer>>>)
scanF(g,r,c)=s   <=>  s=solveBX(g,0,0)   if r=9,
                      s=scanF(g,r+1,0)   if r<9 & c=9,
                      s=scanF(g,r,c+1)   if r<9 & c<9 & cell(g,r,c)!=0,
                      s=scanB(g,r,c)     if r<9 & c<9 & cell(g,r,c)=0
scanB(g,r,c)=s   <=>  s=scanF(g,r,c+1)   if E[x,y](x in {1,2,3,4,5,6,7,8} & y in {2,3,4,5,6,7,8,9} & x<y &
                                                   ok(g,r,c,x) & ok(g,r,c,y)),
                      E[d,g2](forced(g,r,c) & d in {1,2,3,4,5,6,7,8,9} & ok(g,r,c,d) &
                              put(g,r,c,d)=g2 & s=scanF(g2,0,0))
solveBX(g,r,c)=s <=>  s=g                 if r=9,
                      s=solveBX(g,r+1,0)  if r<9 & c=9,
                      s=solveBX(g,r,c+1)  if r<9 & c<9 & cell(g,r,c)!=0,
                      E[d,g2](d in {1,2,3,4,5,6,7,8,9} & ok(g,r,c,d) &
                              put(g,r,c,d)=g2 & s=scanF(g2,0,0))  if r<9 & c<9 & cell(g,r,c)=0

sudoku2(g)=s     <=>  s=scanF(g,0,0)

// ---- puzzles ------------------------------------------------------
// 72 clues, one blank per row/column/box, unique solution
//sudoku2([[0,2,3,4,5,6,7,8,9],[4,5,6,0,8,9,1,2,3],[7,8,9,1,2,3,0,5,6],[2,0,1,5,6,4,8,9,7],[5,6,4,8,0,7,2,3,1],[8,9,7,2,3,1,5,0,4],[3,1,0,6,4,5,9,7,8],[6,4,5,9,7,0,3,1,2],[9,7,8,3,1,2,6,4,0]])=s ? [(s=[[1,2,3,4,5,6,7,8,9],[4,5,6,7,8,9,1,2,3],[7,8,9,1,2,3,4,5,6],[2,3,1,5,6,4,8,9,7],[5,6,4,8,9,7,2,3,1],[8,9,7,2,3,1,5,6,4],[3,1,2,6,4,5,9,7,8],[6,4,5,9,7,8,3,1,2],[9,7,8,3,1,2,6,4,5]]),..][..]
//sudoku2([[0,0,0,0,0,0,0,0,0],[0,5,6,7,8,9,1,2,3],[7,0,9,1,2,3,4,5,6],[2,3,0,5,6,4,8,9,7],[5,6,4,0,9,7,2,3,1],[8,9,7,2,0,1,5,6,4],[3,1,2,6,4,0,9,7,8],[6,4,5,9,7,8,0,1,2],[9,7,8,3,1,2,6,0,5]])=s ? [(s=[[1,2,3,4,5,6,7,8,9],[4,5,6,7,8,9,1,2,3],[7,8,9,1,2,3,4,5,6],[2,3,1,5,6,4,8,9,7],[5,6,4,8,9,7,2,3,1],[8,9,7,2,3,1,5,6,4],[3,1,2,6,4,5,9,7,8],[6,4,5,9,7,8,3,1,2],[9,7,8,3,1,2,6,4,5]])][..]
//sudoku2([[1,0,0,8,5,7,0,9,0],[0,3,0,0,2,0,0,0,8],[0,0,9,6,0,0,5,0,0],[0,0,5,3,1,0,9,0,0],[0,1,0,0,8,0,0,0,2],[6,0,0,0,0,4,0,0,0],[3,5,6,4,7,8,0,1,0],[2,4,1,9,3,5,0,0,7],[0,9,7,2,6,1,3,0,0]])=s ? [(s=[[1,6,2,8,5,7,4,9,3],[5,3,4,1,2,9,6,7,8],[7,8,9,6,4,3,5,2,1],[4,7,5,3,1,2,9,8,6],[9,1,3,5,8,6,7,4,2],[6,2,8,7,9,4,1,3,5],[3,5,6,4,7,8,2,1,9],[2,4,1,9,3,5,8,6,7],[8,9,7,2,6,1,3,5,4]])][..]
// 26 clues, needs real guessing (unique solution, verified externally)
sudoku2([
    [3,0,0,/**/0,0,0,/**/0,0,0],
    [8,2,1,/**/0,0,0,/**/3,9,0],
    [0,0,7,/**/0,0,0,/**/0,8,5],
    //--------------------------
    [0,1,4,/**/0,9,5,/**/0,0,3],
    [9,0,0,/**/0,0,0,/**/5,0,0],
    [0,6,0,/**/0,0,0,/**/0,0,0],
    //--------------------------
    [2,3,0,/**/5,0,0,/**/1,0,0],
    [0,0,0,/**/6,0,0,/**/0,0,0],
    [4,0,5,/**/8,0,0,/**/0,0,7]])=s ?
 [(s=[
    [3,5,9,/**/4,2,8,/**/7,1,6],
    [8,2,1,/**/7,5,6,/**/3,9,4],
    [6,4,7,/**/9,3,1,/**/2,8,5],
    //--------------------------
    [7,1,4,/**/2,9,5,/**/8,6,3],
    [9,8,2,/**/3,6,4,/**/5,7,1],
    [5,6,3,/**/1,8,7,/**/4,2,9],
    //--------------------------
    [2,3,6,/**/5,7,9,/**/1,4,8],
    [1,7,8,/**/6,4,3,/**/9,5,2],
    [4,9,5,/**/8,1,2,/**/6,3,7]])][..]

// ---- unit tests (after the puzzles: earlier queries flip the
// ---- open/closed form of later results) -----------------------------
//t([4,5,6,7,8,9,1,2,3],1)=x           ? [(x=5)][..]
//utRow([1,0,3,0,5,6,7,0,9],1,8)=l     ? [(l=[1,8,3,0,5,6,7,0,9])][..]
//ut([[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0]],1,2,5)=g ? [(g=[[0,0,0,0,0,0,0,0,0],[0,0,5,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0]])][..]
//b(4)=x                               ? [(x=3)][..]
//k([[1,2,3,4,5,6,7,8,9],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0]],1,0,4) ? [()][]
//k([[1,2,3,4,5,6,7,8,9],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0]],1,0,1) ? [][()]
//k([[1,2,3,4,5,6,7,8,9],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0,0]],8,0,1) ? [][()]
//orced([[1,0,0,8,5,7,0,9,0],[0,3,0,0,2,0,0,0,8],[0,0,9,6,0,0,5,0,0],[0,0,5,3,1,0,9,0,0],[0,1,0,0,8,0,0,0,2],[6,0,0,0,0,4,0,0,0],[3,5,6,4,7,8,0,1,0],[2,4,1,9,3,5,0,0,7],[0,9,7,2,6,1,3,0,0]],2,4) ? [()][]
//orced([[1,0,0,8,5,7,0,9,0],[0,3,0,0,2,0,0,0,8],[0,0,9,6,0,0,5,0,0],[0,0,5,3,1,0,9,0,0],[0,1,0,0,8,0,0,0,2],[6,0,0,0,0,4,0,0,0],[3,5,6,4,7,8,0,1,0],[2,4,1,9,3,5,0,0,7],[0,9,7,2,6,1,3,0,0]],1,0) ? [][()]
