// expect-error-per-query
// Bug: a list index outside the int range wraps around to its low 32 bits: 2^32 reads
// element 0 and 2^32+1 element 1, so these queries succeed instead of reporting an index
// outside the list, as `at 7 in [1,2,3] = i` does.
// Correct: every query reports an error and has no result, like any index outside the list.
import nelumbo.collections

Integer i

at 4294967296 in [1,2,3] = i           ?
at 4294967297 in [1,2,3] = i           ?
at -4294967295 in [1,2,3] = i          ?
at 18446744073709551617 in [1,2,3] = i ?
at 4294967296 in [1,2,3] = 1           ?
