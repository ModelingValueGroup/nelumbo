
    import      nelumbo.logic

    Person    :: Object
    Male      :: Person
    Female    :: Person

    FactType  ::= pc(<Person>,<Person>)   // parent-child

    Person    ::= p(<Person>),   // parent
                  c(<Person>),   // child
                  a(<Person>),   // ancestor
                  d(<Person>),   // descendant
                  m(<Person>),   // mother
                  f(<Person>)    // father

    Person a, b, c
    Male   y
    Female x
               
    c(a)=b  <=>  pc(a,b)
    p(a)=b  <=>  pc(b,a)
    m(a)=b  <=>  E[x](c(x)=a & b=x)
    f(a)=b  <=>  E[y](c(y)=a & b=y)
    
    a(a)=b  <=>  d(b)=a
    d(a)=c  <=>  c(a)=c |
                 E[b](d(a)=b & c(b)=c)

    Male   ::= Hendrik, Bernhard, Claus, Willem
    Female ::= Wilhelmina, Juliana, Beatrix, Maxima, Amalia

    fact pc(Hendrik, Juliana),
         pc(Wilhelmina, Juliana),
         pc(Juliana, Beatrix),
         pc(Bernhard, Beatrix),
         pc(Beatrix, Willem),
         pc(Claus, Willem),
         pc(Willem, Amalia),
         pc(Maxima, Amalia)
    
    a(Amalia)=a         ? [(a=Beatrix),(a=Maxima),(a=Hendrik),(a=Bernhard),(a=Juliana),(a=Claus),(a=Willem),(a=Wilhelmina)][..]
    
    m(Amalia)=Maxima    ? [()][]
    m(Amalia)=Willem    ? [][()]
    m(Amalia)=a         ? [(a=Maxima)][..]
    f(Amalia)=a         ? [(a=Willem)][..]
    f(m(f(Amalia)))=a   ? [(a=Bernhard)][..]

    


