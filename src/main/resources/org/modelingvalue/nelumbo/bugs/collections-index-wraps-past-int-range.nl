// expect-error-per-query
// Bug: a `pos` index outside the int range wraps around to its low 32 bits: 2^32 reads
// element 0 and 2^32+1 element 1, so these queries succeed instead of reporting an index
// outside the list, as `i pos [1,2,3] = 7` does.
// Correct: every query reports an error and has no result, like any index outside the list.
import nelumbo.collections

Integer i

i pos [1,2,3] = 4294967296           ?
i pos [1,2,3] = 4294967297           ?
i pos [1,2,3] = -4294967295          ?
i pos [1,2,3] = 18446744073709551617 ?
1 pos [1,2,3] = 4294967296           ?
