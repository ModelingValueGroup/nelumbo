import    nelumbo.integers

Rational        :: Object

private Boolean ::= add(<Rational>,<Rational>,<Rational>)  @nelumbo.rationals.Rationals,
                    mult(<Rational>,<Rational>,<Rational>) @nelumbo.rationals.Rationals,
                    gt(<Rational>,<Rational>)              @nelumbo.rationals.Rationals,
                    iir(<Integer>,<Integer>,<Rational>)    @nelumbo.rationals.Rationals


Boolean  ::= <Rational> ">"  <Rational> #30,
             <Rational> "<"  <Rational> #30,
             <Rational> "<=" <Rational> #30,
             <Rational>  >=  <Rational> #30

Rational ::= <(> - <)?>  <[> <NUMBER> . <NUMBER> <]>  @nelumbo.rationals.Rational,
             <Rational> - <Rational> #40,
             <Rational> + <Rational> #40,
             - <Rational>            #80,
             <Rational> * <Rational> #50,
             <Rational> / <Rational> #50,
             | <Rational> |          #35,
             r(<Integer>),
             r(<Integer> / <Integer>)

Rational a, b, c

a>b   <=>  gt(a,b)
a<b   <=>  b>a
a<=b  <=>  a<b | a=b
a>=b  <=>  a>b | a=b

a+b=c <=>  add(a,b,c)
a-b=c <=>  add(c,b,a)
a*b=c <=>  mult(a,b,c)
a/b=c <=>  mult(c,b,a)

-a=b  <=>  0.0-a=b

|a|=b <=>  b=a  if a>=0.0,
           b=-a if a<0.0

Integer x, y

r(x)=a   <=>  iir(x,1,a)
r(x/y)=a <=>  iir(x,y,a)
