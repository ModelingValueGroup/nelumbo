
  import    nelumbo.logic
    
  Integer :: Object
   
  Integer ::= <NUMBER>                             @org.modelingvalue.nelumbo.integers.NInteger
  
  Boolean ::= add(<Integer>,<Integer>,<Integer>)   @org.modelingvalue.nelumbo.integers.Add,
              mult(<Integer>,<Integer>,<Integer>)  @org.modelingvalue.nelumbo.integers.Multiply,
              <Integer>  >   <Integer>   #30       @org.modelingvalue.nelumbo.integers.GreaterThan,
              <Integer> "<"  <Integer>   #30,
              <Integer> "<=" <Integer>   #30,
              <Integer>  >=  <Integer>   #30

  Integer ::= <Integer> - <Integer>   #40,
              <Integer> + <Integer>   #40,
                        - <Integer>   #80,
              <Integer> * <Integer>   #50,
              <Integer> / <Integer>   #50,
                        | <Integer> | #35

  Integer a, b, c
    
  a<b    <=>  b>a
  a<=b   <=>  a<b | a=b
  a>=b   <=>  a>b | a=b
    
  a+b=c  <=>  add(a,b,c)
  a-b=c  <=>  add(c,b,a)
  a*b=c  <=>  mult(a,b,c)
  a/b=c  <=>  mult(c,b,a)
    
  -a=b   <=>  0-a=b
    
  |a|=b  <=>  b=a   if a>=0,
              b=-a  if a<0
