
    <Person>    :: <Node>
    <Male>      :: <Person>
    <Female>    :: <Person>

    <Relation>  ::= pc(<Person>,<Person>)

    <Predicate> ::= ad(<Person>,<Person>)

    <Person>    ::= p(<Person>),
                    c(<Person>),
                    a(<Person>),
                    d(<Person>)
                    
    <Female>    ::= m(<Person>)
    <Male>      ::= f(<Person>)

    <Person> a, b, c
    <Male>   y
    <Female> x

    ad(a,c) <==  pc(a,c),
                 ad(a,b) & pc(b, c)
               
    c(a)=b  <==  pc(a,b)
    p(a)=b  <==  pc(b,a)
    d(a)=b  <==  ad(a,b)
    a(a)=b  <==  ad(b,a)
    m(a)=b  <==  pc(x,a) & b=x
    f(a)=b  <==  pc(y,a) & b=y

    <Male>   ::= Hendrik, Bernhard, Claus, Willem
    <Female> ::= Wilhelmina, Juliana, Beatrix, Maxima, Amalia

    pc(Hendrik, Juliana)
    pc(Wilhelmina, Juliana)
    pc(Juliana, Beatrix)
    pc(Bernhard, Beatrix)
    pc(Beatrix, Willem)
    pc(Claus, Willem)
    pc(Willem, Amalia)
    pc(Maxima, Amalia)
    
    ? m(Amalia)=a
    ? a(Amalia)=a


