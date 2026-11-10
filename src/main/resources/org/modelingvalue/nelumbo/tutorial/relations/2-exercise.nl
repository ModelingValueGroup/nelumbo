import nelumbo.logic
Person   :: Object
Male     :: Person
Female   :: Person
Boolean ::= pc(<Person>,<Person>), m(<Person>,<Person>), f(<Person>,<Person>)
Person a, b
Female x
Male   y
m(a,b) <=> E[x](pc(x,a) & b=x)
// fix this rule so f(Beatrix,who) finds Bernhard (mirror the mother rule using Male y)
f(a,b) <=> E[y](pc(a,y) & b=y)
Female ::= Juliana, Beatrix
Male   ::= Bernhard
fact pc(Juliana, Beatrix),
     pc(Bernhard, Beatrix)
Person who
f(Beatrix,who) ? [(who=Bernhard),..][..]
