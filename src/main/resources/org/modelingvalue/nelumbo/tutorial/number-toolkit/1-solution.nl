import nelumbo.integers
Integer ::= <Integer> !                 #70,
            <Integer#61> ^ <Integer#59> #60,
            <Integer> mod <Integer>     #50,
            gcd(<Integer>, <Integer>)
Boolean ::= <Integer> divides <Integer>          #30,
            prime <Integer>                      #30,
            no divisor of <Integer> up to <Integer> #30
Integer n, k, a, b, d
n!       == 1 if n=0, n*(n-1)! if n>0
a^k      == 1 if k=0, a*a^(k-1) if k>0
a mod b  == a if a<b, (a-b) mod b if a>=b
gcd(a,b) == a if b=0, gcd(b, a mod b) if b>0
d divides n             <=> n mod d = 0
no divisor of n up to d <=> true  if d<=1,
                            false if d>1 & d divides n,
                            no divisor of n up to d-1 if d>1 & !(d divides n)
prime n                 <=> n>1 & no divisor of n up to n-1

// ---- given: the questions and the answers they should get ----
5!             ? [(120)][..]
0!             ? [(1)][..]
2 * 3!         ? [(12)][..]
2^10           ? [(1024)][..]
2^3^2          ? [(512)][..]
2^3 * 2        ? [(16)][..]
17 mod 5       ? [(2)][..]
2 + 10 mod 4   ? [(4)][..]
(2 + 10) mod 4 ? [(0)][..]
gcd(12, 18)    ? [(6)][..]
gcd(17, 5)     ? [(1)][..]
3 divides 12   ? [()][]
5 divides 12   ? [][()]
prime 2        ? [()][]
prime 97       ? [()][]
prime 91       ? [][()]
prime 1        ? [][()]
