// Bug: `pos` with an index past the end of the list (7 in a 3-element list) crashes
// the whole evaluation (IndexOutOfBoundsException) instead of yielding a falsehood.
import nelumbo.collections

Integer i

i pos [1,2,3] = 7 ? [][..]
