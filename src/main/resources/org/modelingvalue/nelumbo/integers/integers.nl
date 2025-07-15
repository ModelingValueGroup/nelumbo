
    <Integer>   :: <Node>
   
    <Integer>   ::= <NUMBER>                                               @org.modelingvalue.nelumbo.integers.Integer
  
    <Relation>  ::= "gt"   "(" <Integer> "," <Integer> ")"                 @org.modelingvalue.nelumbo.integers.GreaterThan,
                    "add"  "(" <Integer> "," <Integer> "," <Integer> ")"   @org.modelingvalue.nelumbo.integers.Add,
                    "mult" "(" <Integer> "," <Integer> "," <Integer> ")"   @org.modelingvalue.nelumbo.integers.Multiply

    <Predicate> ::= <Integer> "<"  <Integer>  #30,
                    <Integer> ">"  <Integer>  #30,
                    <Integer> "<=" <Integer>  #30,
                    <Integer> ">=" <Integer>  #30

    <Integer>   ::= <Integer> "-"  <Integer>  #40,
                    <Integer> "+"  <Integer>  #40,
                              "-"  <Integer>  #80,
                    <Integer> "*"  <Integer>  #50,
                    <Integer> "/"  <Integer>  #50,
                    "abs" "(" <Integer> ")"

    <Integer>  a, b, c
    
    a>b      <==  gt(a,b)
    a<b      <==  gt(b,a)
    a<=b     <==  a<b | a=b
    a>=b     <==  a>b | a=b
    
    a+b=c <==  add(a,b,c)
    a-b=c <==  add(c,b,a)
    a*b=c <==  mult(a,b,c)
    a/b=c <==  mult(c,b,a)
    
    -a=b     <==  0-a=b
    abs(a)=b <==  (a>=0 & b=a) | (a<0 & b=-a)
    