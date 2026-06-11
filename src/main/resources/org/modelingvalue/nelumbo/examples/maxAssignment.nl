import nelumbo.logic

Type T
T ::= <Boolean#5> ? <T> : <T>
T       t,f,r
Boolean b
(b?t:f)=r <=>  t=r if b, f=r if !b

import nelumbo.integers

Integer ::= max(<Integer>,<Integer>)

Integer x

// Rules here

x=max(14,16) ? [(x=16)][..]
