// Regression test (fixed 2026-11-20; was bugs/open-set-intersection-closed.nl).
// The intersection of an open and a closed set is open when an element of the closed set
// may be in the open one: {1,..} && {2} is {..} (the bug: a closed {}, so 2 in it was false
// and the two sets counted as disjoint). It is closed only when the open set lists every
// element of the closed one. Difference does the same ({2} - {1,..} is {..}).
import nelumbo.collections

Set<Integer> s

{1,..} && {2}   = s   ? [(s={..})][..]
{2} && {1,..}   = s   ? [(s={..})][..]
{1,..} && {1,2} = s   ? [(s={1,..})][..]
2 in ({1,..} && {2})  ? [..][..]
{1,2,..} && {2} = s   ? [(s={2})][..]
{2} && {1,2,..} = s   ? [(s={2})][..]
{1,..} && {}    = s   ? [(s={})][..]
{1,..} && {2,..} = s  ? [(s={..})][..]
{1,2} && {2,3}  = s   ? [(s={2})][..]
