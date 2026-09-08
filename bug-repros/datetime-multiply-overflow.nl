// Bug: period_multiply (datetime/Multiply.java:49) uses intValueExact() on the unbounded
// Integer multiplier. Correct: a result or falsehood. Actual: uncaught ArithmeticException.
import nelumbo.datetime

Period y
P1D * 9999999999 = y ?
