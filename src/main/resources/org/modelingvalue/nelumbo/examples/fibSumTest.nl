
// sum of even fibonacci numbers

import nelumbo.integers

Integer ::= fib(<Integer>)
Integer ::= reg(<Integer>)
Integer ::= fibSum(<Integer>,<Integer>)

Integer a,b,n,f,m

fib(n)=f <=> f=n                 if n<=1,
             f=fib(n-1)+fib(n-2) if n>1  

reg(a)=b <=> b=fib(a) if  E[n](b/2=n),
             b=0      if !E[n](fib(a)/2=n)


fibSum(m,a)=n <=> n=reg(a)+fibSum(m,a+1) if reg(a)<m,
                  n=0                     if reg(a)>=m

fibSum(5,0)=n ? [(n=2)][..]
