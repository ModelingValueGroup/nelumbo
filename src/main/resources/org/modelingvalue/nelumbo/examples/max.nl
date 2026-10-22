import nelumbo.logic

Type T
T ::= <Boolean#5> ? <T#35> : <T#35>
T       t,f
Boolean b
b ? t : f ==  t if b, f if !b

import nelumbo.integers

Integer ::= max(<Integer>,<Integer>)

Integer x,y,z
max(x,y) ==  x>y ? x : y

z = max(14,16)      ? [(z=16)][..]
