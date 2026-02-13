
import nelumbo.strings

Type T
T ::= <Boolean#5> ? <T> : <T>  
T t,f,r
Boolean b
(b?t:f)=r <=> t=r if b, f=r if !b

String s
(3>2?"a":"b")=s ? [(s="a")][..]

Integer ::= max(<Integer>,<Integer>)

Integer x,y,z
max(x,y)=z <=> (x>y?x:y)=z

z = max(14,16) ? [(z=16)][..]
