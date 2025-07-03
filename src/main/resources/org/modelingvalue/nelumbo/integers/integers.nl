
    <Integer>    :: <Node>
    <IntegerLit> :: <Integer>, <Literal>
    <IntegerFun> :: <Integer>, <Function>
    
    <IntegerLit> ::= <NUMBER>                                      @org.modelingvalue.nelumbo.integers.Integer
  
    <Relation>   ::= gt(<IntegerLit>,<IntegerLit>)                 @org.modelingvalue.nelumbo.integers.GreaterThan,
                     add(<IntegerLit>,<IntegerLit>,<IntegerLit>)   @org.modelingvalue.nelumbo.integers.Add,
                     mult(<IntegerLit>,<IntegerLit>,<IntegerLit>)  @org.modelingvalue.nelumbo.integers.Multiply

    <Relation>   ::= <Integer> <  <Integer>  #30,
                     <Integer> >  <Integer>  #30,
                     <Integer> <= <Integer>  #30,
                     <Integer> >= <Integer>  #30

    <IntegerFun> ::= <Integer> -  <Integer>  #40,
                     <Integer> +  <Integer>  #40,
                               -  <Integer>  #80,
                     <Integer> *  <Integer>  #50,
                     <Integer> /  <Integer>  #50,
                     abs(<Integer>)

    <IntegerLit> x, y, z
    <Integer>    a, b, c
    
    a>b      <==  a=x & b=y & gt(x,y)
    a<b      <==  b>a
    a<=b     <==  a<b | a=b
    a>=b     <==  !(a<b)
    -a=b     <==  0-a=b
    abs(a)=b <==  (a>=0 & b=a) | (a<0 & b=-a)
    
    
    a+b=c <==  a=x & b=y & c=z & add(x,y,z)
    a-b=c <==  c+b=a
    a*b=c <==  a=x & b=y & c=z & mult(x,y,z)
    a/b=c <==  c*b=a
    