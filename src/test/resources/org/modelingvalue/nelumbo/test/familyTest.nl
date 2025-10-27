
    <Person>    :: <Node>
    <Male>      :: <Person>
    <Female>    :: <Person>

    <Relation>  ::= pc(<Person>,<Person>)   // parent-child

    <Person>    ::= p(<Person>),   // parent
                    c(<Person>),   // child
                    a(<Person>),   // ancestor
                    d(<Person>),   // descendant
                    m(<Person>),   // mother
                    f(<Person>)    // father

    <Person> a, b, c
    <Male>   y
    <Female> x
               
    c(a)=b  <=>  pc(a,b)
    p(a)=b  <=>  pc(b,a)
    m(a)=b  <=>  c(x)=a & b=x
    f(a)=b  <=>  c(y)=a & b=y
    
    a(a)=b  <=>  d(b)=a
    d(a)=c  <=>  c(a)=c |
                 d(a)=b & c(b)=c

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
    
    a(Amalia)=a         ? [a(Amalia)=Beatrix,a(Amalia)=Maxima,a(Amalia)=Hendrik,a(Amalia)=Bernhard,a(Amalia)=Juliana,a(Amalia)=Claus,a(Amalia)=Willem,a(Amalia)=Wilhelmina][..]
    
    m(Amalia)=Maxima    ? [m(Amalia)=Maxima][]
    m(Amalia)=Willem    ? [][m(Amalia)=Willem]
    m(Amalia)=a         ? [m(Amalia)=Maxima][..]
    f(Amalia)=a         ? [f(Amalia)=Willem][..]
    f(m(f(Amalia)))=a   ? [f(m(f(Amalia)))=Bernhard][..]

    


