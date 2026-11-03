// 4x4 sudoku examples: every puzzle of sudoku/puzzles4x4.nl solved and checked
// against its solution. sudoku-{brute,smart,csp}-4x4-easy.nl are identical
// except for the solver import: sudoku/brute4x4.nl (naive brute force),
// smart4x4.nl (singles-first) or csp4x4.nl (Norvig candidate-set CSP).
// CLI: java -jar nelumbo-cli-*.jar <this file>

import org.modelingvalue.nelumbo.sudoku.brute4x4
import org.modelingvalue.nelumbo.sudoku.puzzles4x4

List<List<Integer>> s

E[s](sudoku(puzzle(oneBlankPerRow))=s & solution(oneBlankPerRow)=s) ? [()][]
E[s](sudoku(puzzle(firstRowEmpty))=s & solution(firstRowEmpty)=s) ? [()][]
E[s](sudoku(puzzle(real))=s & solution(real)=s) ? [()][]
