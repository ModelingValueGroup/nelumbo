// Bug: Collections.indexOf (Collections.java:63) calls coll.get(index) without a
// bounds check. Correct: falsehood (no position 7 in a 3-element list).
// Actual: IndexOutOfBoundsException kills the whole evaluation.
import nelumbo.collections

Integer i

i pos [1,2,3] = 7 ? [][..]
