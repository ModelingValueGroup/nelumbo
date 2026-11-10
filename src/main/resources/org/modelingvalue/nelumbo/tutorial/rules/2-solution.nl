import nelumbo.integers
Integer ::= factorial(<Integer>)
Integer n
factorial(n) == 1 if n<=0, n*factorial(n-1) if n>0
factorial(5) ? [(120)][..]
