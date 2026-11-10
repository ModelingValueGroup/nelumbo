// Bug: a recursive relation whose `if` guards compare two of its head variables (d>=n, d<n)
// does not terminate once it recurses: ok(3,1) needs two steps to reach d=3 and runs forever,
// while ok(3,3) answers at once. With a constant bound (true if d>=3, ok(n,d+1) if d<3), or as a
// function rule (f(n,d) == 1 if d>=n, f(n,d+1) if d<n), the same recursion answers at once.
import nelumbo.integers

Boolean ::= ok(<Integer>, <Integer>)
Integer n, d

ok(n, d) <=> true if d>=n, ok(n, d+1) if d<n

ok(3, 3) ? [()][]
ok(3, 1) ? [()][]
