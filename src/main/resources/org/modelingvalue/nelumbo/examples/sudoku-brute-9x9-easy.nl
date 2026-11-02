// 9x9 sudoku examples: every puzzle of sudoku/puzzles9x9.nl solved and checked
// against its solution. sudoku-{brute,smart,csp}-9x9-easy.nl are identical
// except for the solver import: sudoku/brute9x9.nl (naive brute force),
// smart9x9.nl (singles-first) or csp9x9.nl (Norvig candidate-set CSP).
// CLI: java -DMAX_LOGIC_MEMOIZ=100000 -jar nelumbo-cli-*.jar <this file>

import org.modelingvalue.nelumbo.sudoku.brute9x9
import org.modelingvalue.nelumbo.sudoku.puzzles9x9

List<List<Integer>> s

E[s](sudoku(puzzle(oneBlankPerUnit))=s & solution(oneBlankPerUnit)=s) ? [()][]
E[s](sudoku(puzzle(firstRowEmpty))=s & solution(firstRowEmpty)=s) ? [()][]
E[s](sudoku(puzzle(real))=s & solution(real)=s) ? [()][]
E[s](sudoku(puzzle(guessing))=s & solution(guessing)=s) ? [()][]
