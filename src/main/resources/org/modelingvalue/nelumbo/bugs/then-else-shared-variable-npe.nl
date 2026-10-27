// Bug: a conditional expression whose condition and branch share a variable crashes
// with a NullPointerException instead of answering the query.
import nelumbo.integers

Integer a

(a<0 then a else 2)=-1 ? [(a=-1)][..]
