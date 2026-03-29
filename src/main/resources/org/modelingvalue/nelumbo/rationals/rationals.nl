
  import    nelumbo.integers
    
  Rational :: Object
  
  private Boolean ::= add(<Rational>,<Rational>,<Rational>)   @org.modelingvalue.nelumbo.rationals.Add,
                      mult(<Rational>,<Rational>,<Rational>)  @org.modelingvalue.nelumbo.rationals.Multiply
   
  Boolean ::=  <Rational>  >   <Rational>   #30   @org.modelingvalue.nelumbo.rationals.GreaterThan,
               <Rational> "<"  <Rational>   #30,
               <Rational> "<=" <Rational>   #30,
               <Rational>  >=  <Rational>   #30

  Rational ::= <DECIMAL>                          @org.modelingvalue.nelumbo.rationals.Rational,
               <Rational> - <Rational>   #40,
               <Rational> + <Rational>   #40,
                          - <Rational>   #80,
               <Rational> * <Rational>   #50,
               <Rational> / <Rational>   #50,
                          | <Rational> | #35

  Rational a, b, c
    
  a<b    <=>  b>a
  a<=b   <=>  a<b | a=b
  a>=b   <=>  a>b | a=b
    
  a+b=c  <=>  add(a,b,c)
  a-b=c  <=>  add(c,b,a)
  a*b=c  <=>  mult(a,b,c)
  a/b=c  <=>  mult(c,b,a)
    
  -a=b   <=>  0.0-a=b
    
  |a|=b  <=>  b=a   if a>=0.0,
              b=-a  if a<0.0
