import      nelumbo.collections

Person      :: Object
Male        :: Person
Female      :: Person

Female      ::= m(<Person>)   // mother
Male        ::= f(<Person>)   // father

Set<Person> ::= p(<Person>),  // parents
                c(<Person>),  // childs
                a(<Person>),  // ancestors
                d(<Person>)   // descendants

Person      a, b, c

p(a) ==  {[b](b=m(a)|b=f(a))}
c(a) ==  {[b](a=m(b)|a=f(b))}

d(a) ==  {[b](b in c(a) | E[c](c in c(a) & b in d(c)))}
a(a) ==  {[b](b in p(a) | E[c](c in p(a) & b in a(c)))}

Male   ::= Hendrik, Bernhard, Claus, Willem
Female ::= Wilhelmina, Juliana, Beatrix, Maxima, Amalia

fact f(Juliana)=Hendrik,
     m(Juliana)=Wilhelmina,
     m(Beatrix)=Juliana,
     f(Beatrix)=Bernhard,
     m(Willem)=Beatrix,
     f(Willem)=Claus,
     f(Amalia)=Willem,
     m(Amalia)=Maxima

m(Amalia)=Maxima ? [()][]
m(Amalia)=Willem ? [][()]

m(Amalia)        ? [(Maxima)][..]
f(Amalia)        ? [(Willem)][..]

m(m(f(Amalia)))  ? [(Juliana)][..]

p(Amalia)        ? [({Willem,Maxima})][..]
c(Hendrik)       ? [({Juliana,..})][..]

d(Hendrik)       ? [({Juliana,Willem,Amalia,Beatrix,..})][..]
a(Amalia)        ? [({Juliana,Willem,Maxima,Beatrix,Hendrik,Wilhelmina,Bernhard,Claus,..})][..]
