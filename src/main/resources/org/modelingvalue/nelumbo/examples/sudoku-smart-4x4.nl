// 4x4 sudokus: every puzzle of sudoku/puzzles4x4.nl solved by sudoku/smart4x4.nl
// (singles first) and checked against its solution; sudoku-{brute,smart,csp}-4x4.nl
// differ only in the solver import. The time of each sudoku is its query's micros in
// the --json output.
// CLI: java -jar nelumbo-cli-*.jar --json <this file>

import org.modelingvalue.nelumbo.sudoku.smart4x4
import org.modelingvalue.nelumbo.sudoku.puzzles4x4

List<List<Integer>> s

E[s](sudoku(puzzle(oneBlankPerRow))=s & solution(oneBlankPerRow)=s) ? [()][]
E[s](sudoku(puzzle(firstRowEmpty))=s & solution(firstRowEmpty)=s) ? [()][]
E[s](sudoku(puzzle(real))=s & solution(real)=s) ? [()][]
