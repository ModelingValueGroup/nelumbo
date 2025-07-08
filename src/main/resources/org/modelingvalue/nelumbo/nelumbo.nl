
    <Predicate> ::= true                             @org.modelingvalue.nelumbo.Boolean,
                    false                            @org.modelingvalue.nelumbo.Boolean,
                    ! <Predicate>               #25  @org.modelingvalue.nelumbo.Not,
                    <Predicate> & <Predicate>   #20  @org.modelingvalue.nelumbo.And,
                    <Predicate> | <Predicate>   #20  @org.modelingvalue.nelumbo.Or,
                    <Predicate> --> <Predicate> #15  @org.modelingvalue.nelumbo.Collect,
                    eq(<Node>,<Node>)                @org.modelingvalue.nelumbo.Equal,
                    <Node> = <Node>             #30,
                    <Node> != <Node>            #30

    <Literal>  l, l1, l2
    <Function> f1, f2
    <Node>     n1, n2

    l1=l2  <==  eq(l1, l2)
    f1=f2  <==  f1=l & f2=l
    l1=f1  <==  f1=l1
    n1!=n2 <==  !(n1=n2)

