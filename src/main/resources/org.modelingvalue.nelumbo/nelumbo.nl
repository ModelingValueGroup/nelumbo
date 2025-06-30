

    <Literal>   :: <Node>
    <Function>  :: <Node>

    <Relation>  ::= eq(<Literal>,<Literal>)         @org.modelingvalue.nelumbo.Equal,
                    <Node>  =(30) <Node>,
                    <Node> !=(30) <Node>

    <Predicate> ::= true                            @org.modelingvalue.nelumbo.Boolean,
                    false                           @org.modelingvalue.nelumbo.Boolean,
                    !(50) <Predicate>               @org.modelingvalue.nelumbo.Not,
                    <Predicate> &(20) <Predicate>   @org.modelingvalue.nelumbo.And,
                    <Predicate> |(20) <Predicate>   @org.modelingvalue.nelumbo.Or,
                    <Predicate> -->(15) <Predicate> @org.modelingvalue.nelumbo.Collect

    <Literal>  L1, L2
    <Function> F1, F2
    <Node>     N1, N2

    L1=L2  <==  eq(L1, L2)
    F1=F2  <==  F1=L1 & F2=L1
    L1=F1  <==  F1=L1
    N1!=N2 <==  !(N1=N2)

