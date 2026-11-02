// A function query whose given result is itself a call crashes the whole run with
// IllegalArgumentException "argument type mismatch" on the CSP solver:
// sudoku(p) = solution(0) ? [()][]. With the result as a literal it works.
import org.modelingvalue.nelumbo.sudoku.csp4x4

List<List<Integer>> ::= solution(<Integer>)

Integer i

solution(i) == [[1,2,4,3],[3,4,1,2],[4,3,2,1],[2,1,3,4]]

sudoku([[1,0,0,0],[0,0,1,0],[0,3,0,0],[0,0,0,4]]) = solution(0) ? [()][]
