// Bug: a set-builder whose condition has no solutions does not yield the empty set:
// its result has no binding, so comparing it with {} is false.
import nelumbo.collections

Integer i

{[i](i in {1,2,3} & i>5)}={} ? [()][]
{[i](i in {})}={}            ? [()][]
