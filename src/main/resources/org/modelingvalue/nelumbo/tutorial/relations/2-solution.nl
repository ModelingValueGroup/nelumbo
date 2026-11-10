import nelumbo.logic
Person   :: Object
Male     :: Person
Female   :: Person
Boolean ::= pc(<Person>,<Person>), m(<Person>,<Person>), f(<Person>,<Person>)
Person a, b
Female x
Male   y
m(a,b) <=> E[x](pc(x,a) & b=x)
f(a,b) <=> E[y](pc(y,a) & b=y)
Female ::= Juliana, Beatrix
Male   ::= Bernhard
fact pc(Juliana, Beatrix),
     pc(Bernhard, Beatrix)
Person who
f(Beatrix,who) ? [(who=Bernhard),..][..]
