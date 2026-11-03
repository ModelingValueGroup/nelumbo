// Unit tests of the sudoku solver sudoku/csp4x4.nl.

import org.modelingvalue.nelumbo.sudoku.csp4x4

List<List<Set<Digit>>> g
Set<Digit>             sc

dig(2)    ? [(D2)][..]
undig(D3) ? [(3)][..]
icell([[1,0,0,0],[0,0,1,0],[0,3,0,0],[0,0,0,4]],2,1) ? [(3)][..]
cell([[{D1,D2},{D3},{D4},{D1}]],0,0) ? [({D1,D2})][..]
E[g](fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]])=g & cell(g,1,2)=sc) ? [(sc={D1,D2,D3,D4})][..]
peer(0,0,1,1) ? [()][]
peer(0,0,2,2) ? [][()]
peer(0,0,0,0) ? [][()]
cell(put(fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]]),0,0,{D2}),0,0) ? [({D2})][..]
cell(put(fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]]),0,0,{D2}),0,1) ? [({D1,D2,D3,D4})][..]
// eliminate
cell(elim(fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]]),0,0,D1),0,0) ? [({D2,D3,D4})][..]
cell(elim(put(fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]]),0,0,{D2}),0,0,D1),0,0) ? [({D2})][..]
// [][..] alone, [..][..] after the first probe above: bugs/empty-set-query-changes-later-result.nl
//elim(put(fullGrid([[0,0,0,0],[0,0,0,0],[0,0,0,0],[0,0,0,0]]),0,0,{D2}),0,0,D2) ? [][..]
// two solutions: search keeps the first (D1 at (0,0))
sudoku([[0,0,3,4],[3,4,1,2],[0,0,4,3],[4,3,2,1]]) ? [([[1,2,3,4],[3,4,1,2],[2,1,4,3],[4,3,2,1]])][..]
