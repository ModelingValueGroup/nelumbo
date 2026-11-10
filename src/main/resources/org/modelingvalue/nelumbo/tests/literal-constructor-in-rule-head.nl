// Regression test (2026-11-10).
// A rule head can take apart a constructor of a user `:: Literal` type whose holes are
// Integers: `n inside range(lo, hi)` matches the parts lo and hi, in a relation and in a
// function rule, also for a constructor with a connected-token pattern (9:30 to 11:00).
// And a Literal is a value: equal exactly when its parts are, so it can be compared and
// collected in a set (with `:: Object` it is a function call, and all of that is unknown).
import nelumbo.collections

pattern TIME ::= <[> <Integer#100> : <Integer#100> <]>

Range   :: Literal
Range   ::= range(<Integer>, <Integer>)
Slot    :: Literal
Slot    ::= <TIME> to <TIME> #35
Boolean ::= <Integer> inside <Range> #30,
            <Integer> falls in <Slot> #30
Integer ::= clamp(<Integer>, <Range>)

Integer n, lo, hi, h1, m1, h2, m2

n inside range(lo, hi)        <=> lo <= n & n <= hi
clamp(n, range(lo, hi))       ==  lo if n < lo, hi if n > hi, n if lo <= n & n <= hi
n falls in h1:m1 to h2:m2     <=> h1*60+m1 <= n & n < h2*60+m2

3 inside range(1, 5)          ? [()][]
7 inside range(1, 5)          ? [][()]
clamp(7, range(1, 5))         ? [(5)][..]
clamp(0, range(1, 5))         ? [(1)][..]
clamp(3, range(1, 5))         ? [(3)][..]
600 falls in 9:30 to 11:00    ? [()][]
660 falls in 9:30 to 11:00    ? [][()]

range(1, 5) = range(1, 5)     ? [()][]
range(1, 5) = range(1, 6)     ? [][()]
range(1, 5) != range(1, 6)    ? [()][]
range(1, 5) in {range(1, 5)}  ? [()][]
range(1, 6) in {range(1, 5)}  ? [][()]
|{range(1, 5), range(1, 5)}|  ? [(1)][..]
9:30 to 11:00 != 9:30 to 12:00 ? [()][]
