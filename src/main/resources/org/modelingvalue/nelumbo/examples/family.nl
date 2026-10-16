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
Set<Person> s
Male        y
Female      x

p(a)=s <=>  {[b](b=m(a)|b=f(a))}=s
c(a)=s <=>  {[b](a=m(b)|a=f(b))}=s

d(a)=s <=>  {[b](b in c(a) | E[c](c in c(a) & b in d(c)))}=s
a(a)=s <=>  {[b](b in p(a) | E[c](c in p(a) & b in a(c)))}=s

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

m(Amalia)=Maxima  ? [()][]
m(Amalia)=Willem  ? [][()]

m(Amalia)=x       ? [(x=Maxima)][..]
f(Amalia)=y       ? [(y=Willem)][..]

m(m(f(Amalia)))=x ? [(x=Juliana)][..]

p(Amalia)=s       ? [(s={Willem,Maxima})][..]
c(Hendrik)=s      ? [(s={Juliana}),..][..]

d(Hendrik)=s      ? [(s={Juliana,Willem,Amalia,Beatrix}),..][..]
a(Amalia)=s       ? [(s={Juliana,Willem,Maxima,Beatrix,Hendrik,Wilhelmina,Bernhard,Claus}),..][..]
