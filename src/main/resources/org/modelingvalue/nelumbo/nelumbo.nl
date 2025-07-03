
    <Fact>      ::= eq(<Literal>,<Literal>)          @org.modelingvalue.nelumbo.Equal

    <Relation>  ::= <Node> = <Node>             #30,
                    <Node> != <Node>            #30  

    <Predicate> ::= true                             @org.modelingvalue.nelumbo.Boolean,
                    false                            @org.modelingvalue.nelumbo.Boolean,
                    ! <Predicate>               #25  @org.modelingvalue.nelumbo.Not,
                    <Predicate> & <Predicate>   #20  @org.modelingvalue.nelumbo.And,
                    <Predicate> | <Predicate>   #20  @org.modelingvalue.nelumbo.Or,
                    <Predicate> --> <Predicate> #15  @org.modelingvalue.nelumbo.Collect

    <Literal>  L1, L2
    <Function> F1, F2
    <Node>     N1, N2

    L1=L2  <==  eq(L1, L2)
    F1=F2  <==  F1=L1 & F2=L1
    L1=F1  <==  F1=L1
    N1!=N2 <==  !(N1=N2)

