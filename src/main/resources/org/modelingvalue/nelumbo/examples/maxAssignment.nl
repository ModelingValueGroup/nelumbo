import nelumbo.logic

Type T
T ::= <Boolean#5> ? <T> : <T>
T       t,f
Boolean b
(b?t:f) ==  t if b, f if !b

import nelumbo.integers

Integer ::= max(<Integer>,<Integer>)

Integer x

// Rules here

x=max(14,16) ? [(x=16)][..]
