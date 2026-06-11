import nelumbo.logic

Type T
T ::= <Boolean#5> ?? <T#35> :: <T#35>
T       t,f,r
Boolean b
b ?? t :: f=r <=> t=r if b, f=r if !b

import nelumbo.integers

Integer ::= max(<Integer>,<Integer>)

Integer x,y,z
max(x,y)=z <=>  x>y ?? x :: y=z

z = max(14,16) ? [(z=16)][..]
