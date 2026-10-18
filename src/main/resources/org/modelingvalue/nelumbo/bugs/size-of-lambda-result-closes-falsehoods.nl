// Bug: the size of a `where` or `map` result reports a closed, empty falsehoods side,
// claiming no other size is false; the size of a literal reports [..].
import nelumbo.collections

Integer i

|{1,2,3}|=i                  ? [(i=3)][..]
|[1,2,3] where [i](i>1)|=i   ? [(i=2)][..]
|{1,2,3} where [i](i>1)|=i   ? [(i=2)][..]
|[1,2,3] map [i](i+1)|=i     ? [(i=3)][..]
