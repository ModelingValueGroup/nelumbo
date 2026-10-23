import nelumbo.logic

Type T
T ::= <Boolean#5> ? <T> : <T>
T       t,f
Boolean b
(b?t:f) ==  t if b, f if !b

import nelumbo.integers

Integer ::= max(<Integer>,<Integer>)

// Rules here

max(14,16) ? [(16)][..]
