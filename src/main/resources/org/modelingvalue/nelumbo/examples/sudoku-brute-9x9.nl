// 9x9 sudokus: every puzzle of sudoku/puzzles9x9.nl solved by sudoku/brute9x9.nl
// (naive brute force) and checked against its solution; sudoku-{brute,smart,csp}-9x9.nl
// differ only in the solver import and in singlesOnly, which only the CSP runs. The time
// of each sudoku is its query's micros in the --json output.
// CLI: java -DMAX_LOGIC_MEMOIZ=100000 -jar nelumbo-cli-*.jar --json <this file>

import org.modelingvalue.nelumbo.sudoku.brute9x9
import org.modelingvalue.nelumbo.sudoku.puzzles9x9

List<List<Integer>> s

E[s](sudoku(puzzle(oneBlankPerUnit))=s & solution(oneBlankPerUnit)=s) ? [()][]
E[s](sudoku(puzzle(firstRowEmpty))=s & solution(firstRowEmpty)=s) ? [()][]
E[s](sudoku(puzzle(real))=s & solution(real)=s) ? [()][]
E[s](sudoku(puzzle(guessing))=s & solution(guessing)=s) ? [()][]
// singlesOnly: > 2 hours with this solver (the CSP needs ~190 s), so not run
// E[s](sudoku(puzzle(singlesOnly))=s & solution(singlesOnly)=s) ? [()][]
