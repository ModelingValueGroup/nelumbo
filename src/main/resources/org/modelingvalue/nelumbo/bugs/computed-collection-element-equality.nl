// Bug: `=` between a collection literal with a computed element and an equal literal is
// false: {1+1} = {2} gives [][()]. The element itself is computed ({1+1} ? gives {2},
// s = {1+1} binds s={2}), only the comparison treats {1+1} as a different value.
import nelumbo.collections

Set<Integer>  s
List<Integer> l

{1+1} = {2}       ? [()][]
{2} = {1+1}       ? [()][]
{1+1, 3} = {2, 3} ? [()][]
[1+1] = [2]       ? [()][]

// controls: these work
s = {1+1}         ? [(s={2})][..]
l = [1+1]         ? [(l=[2])][..]
2 in {1+1}        ? [()][]
