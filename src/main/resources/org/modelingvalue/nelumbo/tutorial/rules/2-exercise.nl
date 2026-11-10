import nelumbo.integers
Integer ::= factorial(<Integer>)
Integer n
// fix this rule so factorial(5)=120  (hint: 1 if n<=0, else n*factorial(n-1))
factorial(n) == n
factorial(5) ? [(120)][..]
