// Regression test (fixed 2026-11-17; was bugs/computed-collection-element-equality.nl).
// `=` between a collection literal with a computed element and an equal literal is true:
// {1+1} = {2}. (Was: [][()], the comparison treated {1+1} as a different value.)
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
