
    <Integer>    :: <Terminal>
    <IntegerLit> :: <Integer>, <Literal>
    
    <IntegerLit> ::= <NUMBER> @org.modelingvalue.nelumbo.integers.Integer
  
    <Relation>   ::= gt(<IntegerLit>,<IntegerLit>)     @org.modelingvalue.nelumbo.integers.GreaterThan,
                     add(<IntegerLit>,<IntegerLit>)    @org.modelingvalue.nelumbo.integers.Add,
                     mult(<IntegerLit>,<IntegerLit>)   @org.modelingvalue.nelumbo.integers.Multiply

    <Relation>   ::= <Integer> <(30)  <Integer>,
                     <Integer> >(30)  <Integer>,
                     <Integer> <=(30) <Integer>,
                     <Integer> >=(30) <Integer>

    <Integer>    ::= <Integer> -(40)  <Integer>,
                     <Integer> +(40)  <Integer>,
                               -(60)  <Integer>,
                     <Integer> *(50)  <Integer>,
                     <Integer> /(50)  <Integer>

    <IntegerLit> x, y
    <Integer>    a, b
    
    a>b   <==  a=x & b=y & gt(x,y)
    a<b   <==  b>a
    a<=b  <==  a<b | a=b
    a>=b  <==  !(a<b)
    -a=b  <==  0-a=b
