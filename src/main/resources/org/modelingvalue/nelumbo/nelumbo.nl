
    <Predicate> ::= true                             @org.modelingvalue.nelumbo.Boolean,
                    false                            @org.modelingvalue.nelumbo.Boolean,
                    unknown                          @org.modelingvalue.nelumbo.Boolean,
                    ! <Predicate>               #25  @org.modelingvalue.nelumbo.Not,
                    <Predicate> & <Predicate>   #22  @org.modelingvalue.nelumbo.And,
                    <Predicate> | <Predicate>   #20  @org.modelingvalue.nelumbo.Or,
                    eq(<Node>,<Node>)                @org.modelingvalue.nelumbo.Equal,
                    <Node> != <Node>            #30

    <Literal>  l, l1, l2
    <Function> f1, f2
    <Node>     n1, n2

    l1=l2  <=> eq(l1, l2)
    l1=f1  <=> f1=l1
    n1!=n2 <=> !(n1=n2)

