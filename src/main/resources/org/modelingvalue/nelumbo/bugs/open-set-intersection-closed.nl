// Bug: the intersection of an open and a closed set is closed even when an element of the
// closed set may be in the open one: {1,..} && {2} is {}, so 2 in ({1,..} && {2}) is false
// and the two sets count as disjoint. Difference keeps such a result open ({2} - {1,..} is {..}).
import nelumbo.collections

Set<Integer> s

{1,..} && {2}   = s   ? [(s={..})][..]
{2} && {1,..}   = s   ? [(s={..})][..]
{1,..} && {1,2} = s   ? [(s={1,..})][..]
2 in ({1,..} && {2})  ? [..][..]
{1,2,..} && {2} = s   ? [(s={2})][..]
