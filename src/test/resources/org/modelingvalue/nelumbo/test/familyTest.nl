
    <Person>    :: <Node>

    <Relation>  ::= pc(<Person>,<Person>)

    <Predicate> ::= ad(<Person>,<Person>)

    <Person>    ::= p(<Person>),
                    c(<Person>),
                    a(<Person>),
                    d(<Person>)

    <Person> a, b, c

    ad(a,c) <==  pc(a,c),
                 ad(a,b) & pc(b, c)

    c(a)=b  <==  pc(a,b)
    p(a)=b  <==  pc(b,a)
    d(a)=b  <==  ad(a,b)
    a(a)=b  <==  ad(b,a)

    <Person> ::= Piet, Jan, Hein

    pc(Piet,Jan)
    pc(Jan, Hein)

    ? p(Piet)=Jan
    ? p(Jan)=Hein

    ? p(Hein)=Jan
    ? p(Jan)=Piet

    ? p(Piet)=a
    ? p(Jan)=a
    ? p(Hein)=a

    ? c(Piet)=a
    ? c(Jan)=a
    ? c(Hein)=a

    ? a(Piet)=a
    ? a(Jan)=a
    ? a(Hein)=a

    ? d(Piet)=a
    ? d(Jan)=a
    ? d(Hein)=a
