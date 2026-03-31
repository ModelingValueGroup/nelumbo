import nelumbo.logic

Type T
T ::= <Boolean#5> ? <T> : <T>  
T t,f,r
Boolean b

(b?t:f)=r <=> t=r if  b,
              f=r if !b

import nelumbo.strings

String s
(3>2?"a":"b")=s ? [(s="a")][..]
