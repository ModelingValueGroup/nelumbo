import nelumbo.integers
Integer ::= fib(<Integer>)
Integer n
fib(n) == n if n<=1, fib(n-1)+fib(n-2) if n>1
fib(7) ?
