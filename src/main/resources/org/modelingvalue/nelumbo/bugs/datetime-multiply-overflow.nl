// Bug: multiplying a Period by an integer outside the int range crashes the whole
// evaluation (ArithmeticException) instead of yielding a result or a falsehood.
import nelumbo.datetime

Period y
P1D * 9999999999 = y ?
