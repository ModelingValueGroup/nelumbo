import nelumbo.logic
Person   :: Object
Male     :: Person
Female   :: Person
Boolean ::= pc(<Person>,<Person>), m(<Person>,<Person>)
Person a, b
Female x
m(a,b) <=> E[x](pc(x,a) & b=x)
Female ::= Juliana, Beatrix
fact pc(Juliana, Beatrix)
Person who
m(Beatrix,who) ?
