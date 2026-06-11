import nelumbo.logic

Type T
T ::= <Boolean#5> ? <T> : <T>

// Rules here

import nelumbo.strings

String s
(3>2?"a":"b")=s ? [(s="a")][..]
