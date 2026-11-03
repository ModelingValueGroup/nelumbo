// A 9x9 sudoku where the CSP solver wins (sudoku/puzzles9x9.nl: singlesOnly):
// sudoku-{brute,smart,csp}-9x9-hard.nl are identical except for the solver
// import. Timings in SudokuExamplesTest.
// CLI: java -DMAX_LOGIC_MEMOIZ=100000 -jar nelumbo-cli-*.jar <this file>

import org.modelingvalue.nelumbo.sudoku.csp9x9
import org.modelingvalue.nelumbo.sudoku.puzzles9x9

List<List<Integer>> s

E[s](sudoku(puzzle(singlesOnly))=s & solution(singlesOnly)=s) ? [()][]
