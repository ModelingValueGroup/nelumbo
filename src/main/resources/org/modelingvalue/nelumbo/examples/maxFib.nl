import nelumbo.integers

Integer ::= fib(<Integer>)
Integer ::= maxFib(<Integer>, <Integer>)
Integer ::= maxFib(<Integer>)

Integer n,f,m

fib(n)       ==  n                 if n<=1,
                 fib(n-1)+fib(n-2) if n>1

maxFib(n, m) ==  fib(n)        if fib(n)<m & fib(n+1)>=m,
                 maxFib(n+1,m) if fib(n+1)<m

maxFib(m)    ==  maxFib(0,m)

fib(50)=f         ? [(f=12586269025)][..]

maxFib(1000000)=f ? [(f=832040)][..]
