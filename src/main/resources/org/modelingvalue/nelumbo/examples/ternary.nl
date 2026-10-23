import nelumbo.logic

Type T
T ::= <Boolean#5> ? <T> : <T>
T       t,f
Boolean b

(b?t:f) ==  t if  b,
            f if !b

import nelumbo.strings

(3>2?"a":"b") ? [("a")][..]
