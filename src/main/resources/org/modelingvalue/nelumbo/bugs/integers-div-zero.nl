// Bug: integer division by zero (also when solving 0 * x = 6) crashes the whole
// evaluation (ArithmeticException) instead of yielding no solution.
import nelumbo.integers

Integer x

6 / 0 = x ? [][..]
0 * x = 6 ? [][..]
