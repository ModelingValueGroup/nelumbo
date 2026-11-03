// 4x4 sudoku puzzles with their solutions (0 = blank), for the example files
// examples/sudoku-{brute,smart,csp}-4x4-easy.nl: puzzle(name) is the input, solution(name)
// the expected output of any solver's sudoku(puzzle(name)).
import nelumbo.collections

Puzzle :: Object
Puzzle ::= oneBlankPerRow, firstRowEmpty, real

List<List<Integer>> ::= puzzle(<Puzzle>), solution(<Puzzle>)

// one blank per row, unique solution
puzzle(oneBlankPerRow) == [
    [0,2,/**/3,4],
    [3,4,/**/1,0],
    //-----------
    [2,0,/**/4,3],
    [4,3,/**/0,1]]
solution(oneBlankPerRow) == [
    [1,2,/**/3,4],
    [3,4,/**/1,2],
    //-----------
    [2,1,/**/4,3],
    [4,3,/**/2,1]]

// first row empty plus one blank per remaining row, unique solution
puzzle(firstRowEmpty) == [
    [0,0,/**/0,0],
    [0,4,/**/1,2],
    //-----------
    [2,1,/**/0,3],
    [4,3,/**/2,0]]
solution(firstRowEmpty) == [
    [1,2,/**/3,4],
    [3,4,/**/1,2],
    //-----------
    [2,1,/**/4,3],
    [4,3,/**/2,1]]

// a 'real' puzzle
puzzle(real) == [
    [1,0,/**/0,0],
    [0,0,/**/1,0],
    //-----------
    [0,3,/**/0,0],
    [0,0,/**/0,4]]
solution(real) == [
    [1,2,/**/4,3],
    [3,4,/**/1,2],
    //-----------
    [4,3,/**/2,1],
    [2,1,/**/3,4]]
