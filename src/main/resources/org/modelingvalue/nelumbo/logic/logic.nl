
    <Predicate> ::= true                                              @org.modelingvalue.nelumbo.logic.Boolean,
                    false                                             @org.modelingvalue.nelumbo.logic.Boolean,
                    unknown                                           @org.modelingvalue.nelumbo.logic.Boolean,
                    ! <Predicate>                             #25     @org.modelingvalue.nelumbo.logic.Not,
                    <Predicate> & <Predicate>                 #22     @org.modelingvalue.nelumbo.logic.And,
                    <Predicate> | <Predicate>                 #20     @org.modelingvalue.nelumbo.logic.Or,
                    <Predicate> -> <Predicate>                #18,
                    <Predicate> <-> <Predicate>               #16,
                    E[<(> <Variable> <,> , <)+>](<Predicate>) #100 #0 @org.modelingvalue.nelumbo.logic.ExistentialQuantifier,
                    A[<(> <Variable> <,> , <)+>](<Predicate>) #100 #0 @org.modelingvalue.nelumbo.logic.UniversalQuantifier,
                    eq(<Object>,<Object>)                                 @org.modelingvalue.nelumbo.logic.Equal,
                    <Object> != <Object>                          #30

    <Predicate> p1, p2
    
    p1->p2  <=> !p1|p2
    p1<->p2 <=> (p1->p2)&(p2->p1)
     
    <Literal>  l1, l2
    <Function> f1, f2
    <Object>     n1, n2

    l1=l2  <=> eq(l1, l2)
    l1=f1  <=> f1=l1
    n1!=n2 <=> !(n1=n2)

