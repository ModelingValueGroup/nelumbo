import    nelumbo.logic

Integer              :: Object

private Boolean      ::= add(<Integer>,<Integer>,<Integer>)  @nelumbo.integers.Integers,
                         mult(<Integer>,<Integer>,<Integer>) @nelumbo.integers.Integers,
                         gt(<Integer>,<Integer>)             @nelumbo.integers.Integers

Boolean              ::= <Integer> ">"  <Integer> #30,
                         <Integer> "<"  <Integer> #30,
                         <Integer> "<=" <Integer> #30,
                         <Integer> ">=" <Integer> #30

pattern RADIX_NUMBER ::= <(> <(> <NUMBER> <|> <NAME> <)> <)+>

Integer              ::= <(> - <)?> <[> <NUMBER> <(> "#" <RADIX_NUMBER> <)?> <]>  @nelumbo.integers.NInteger,
                         <Integer> - <Integer> #40,
                         <Integer> + <Integer> #40,
                         - <Integer>           #80,
                         <Integer> * <Integer> #50,
                         <Integer> / <Integer> #50,
                         | <Integer> |         #35

Integer a, b, c

a>b   <=>  gt(a,b)
a<b   <=>  b>a
a<=b  <=>  a<b | a=b
a>=b  <=>  a>b | a=b

a+b=c <=>  add(a,b,c)
a-b=c <=>  add(c,b,a)
a*b=c <=>  mult(a,b,c)
a/b=c <=>  mult(c,b,a)

-a=b  <=>  0-a=b

|a|=b <=>  b=a  if a>=0,
           b=-a if a<0
