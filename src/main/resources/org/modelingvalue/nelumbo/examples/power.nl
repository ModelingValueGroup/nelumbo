import nelumbo.rationals

Rational ::= <Rational> ** <Integer> #60

Rational a,b,c
Integer  x,y,z

a**x=b <=>  b=1.0         if x=0,
            b=a**(x-1)*a  if x>0

10.0**8=a ? [(a=100000000.0)][..]
3.0**3=a  ? [(a=27.0)][..]
