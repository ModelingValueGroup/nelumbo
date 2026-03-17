// sum of even fibonacci numbers

import nelumbo.integers

Boolean ::= even(<Integer>)
Integer ::= fib(<Integer>)
Integer ::= evenFib(<Integer>)
Integer ::= evenFibSum(<Integer>)

Integer a,b,n,f,m

even(a) <=> E[n](a/2=n)

fib(n)=f <=> f=n                 if n<=1,
             f=fib(n-1)+fib(n-2) if n>1  

evenFib(a)=b <=> b=fib(a) if even(fib(a)),
                 b=0      if !even(fib(a))

evenFibSum(a)=n <=> n=0                          if a<=1,
                    n=evenFibSum(a-1)+evenFib(a) if a>1 

evenFibSum(100)=n ? [(n=36#1oh95l3hiwndk2)][..]

