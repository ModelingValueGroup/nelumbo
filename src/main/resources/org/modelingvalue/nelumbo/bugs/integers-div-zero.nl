// Bug: Integers.mult (Integers.java:74/77) divides by zero when a factor is 0.
// Correct: falsehood (6/0 has no solution) resp. unknown (0*x=0 holds for any x).
// Actual: uncaught ArithmeticException kills the whole evaluation.
import nelumbo.integers

Integer x

6 / 0 = x ? [][..]
0 * x = 6 ? [][..]
