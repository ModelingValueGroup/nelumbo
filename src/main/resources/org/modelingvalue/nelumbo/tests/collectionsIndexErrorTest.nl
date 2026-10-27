// An index outside the list is not false but an error (fail fast): the built-in
// throws and that query has no result. Combined with a decided operand, three-valued
// logic absorbs the error: true | error is true, false & error is false.
// NelumboTest.collectionsIndexErrorTest checks the errors.
import nelumbo.collections

Integer i, x

at 3 in [5,6,7]                     ?
at -1 in [5,6,7]                    ?
at 3 in [5,6,7] = 5                 ?
at 0 in []                          ?
i first index [1,2] = 5             ?
i last index [1,2] = -1             ?

at 2 in [5,6,7]                     ? [(7)][..]
i first index [1,2] = 1             ? [(i=2)][..]
i last index [1,2] = 0              ? [(i=1)][..]

at 3 in [5,6,7] = 5 | 1 in [1]      ? [()][]
1 in [1] | i first index [1,2] = 5  ? [()][]
at 3 in [5,6,7] = 5 & 4 in [1]      ? [][()]
4 in [1] & i last index [1,2] = -1  ? [][()]
