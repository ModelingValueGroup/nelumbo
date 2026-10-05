import  nelumbo.lang

Boolean  :: Object
FactType :: Boolean
Function :: Object
Literal  :: Object
Struct   :: Object

Type A1, A2, A3, R

Lambda              :: Object
Lambda<R>           :: Lambda

Lambda1<A1,R>       :: Lambda<R>
Lambda2<A1,A2,R>    :: Lambda<R>
Lambda3<A1,A2,A3,R> :: Lambda<R>

Lambda1<A1,R>       ::= [<{Variable,A1}>](<R#0>)                                 @nelumbo.logic.Lambda
Lambda2<A1,A2,R>    ::= [<{Variable,A1}>,<{Variable,A2}>](<R#0>)                 @nelumbo.logic.Lambda
Lambda3<A1,A2,A3,R> ::= [<{Variable,A1}>,<{Variable,A2}>,<{Variable,A3}>](<R#0>) @nelumbo.logic.Lambda

private Boolean     ::= eq(<Literal>,<Literal>) @nelumbo.logic.Equal

Boolean             ::= true                          @nelumbo.logic.NBoolean,
                        false                         @nelumbo.logic.NBoolean,
                        unknown                       @nelumbo.logic.NBoolean,
                        ! <Boolean>               #25 @nelumbo.logic.Not,
                        <Boolean> & <Boolean>     #22 @nelumbo.logic.And,
                        <Boolean> | <Boolean>     #20 @nelumbo.logic.Or,
                        E<Lambda<Boolean>>            @nelumbo.logic.ExistentialQuantifier,
                        A<Lambda<Boolean>>            @nelumbo.logic.UniversalQuantifier,
                        <Object> = <Object>       #30 @nelumbo.logic.NIs,
                        <Object> != <Object>      #30,
                        <Boolean> -> <Boolean>    #18,
                        <Boolean> "<->" <Boolean> #16

pattern BINDING     ::= [ <(> <(> ( <(> <Variable#100> = <Object#0> <,> , <)*> ) <|> .. <)> <,> , <)*> ]

Root                ::= "fact" <(> <FactType#0> <,> , <)+>                                     @nelumbo.logic.Fact,
                        <Boolean#0> "<=>" <(> <Boolean#0> <(> "if" <Boolean#0> <)?> <,> , <)+> @nelumbo.logic.Rule,
                        <Boolean#0> ? <(> <BINDING> <BINDING> <)?>                             @nelumbo.logic.Query

Boolean p1, p2

p1->p2  <=>  !p1|p2
p1<->p2 <=>  (p1->p2)&(p2->p1)

Type E
E    n1, n2
{E,Literal}  l1, l2
{E,Function} f1, f2

l1=l2  <=>  eq(l1, l2)
l1=f1  <=>  f1=l1
n1!=n2 <=>  !(n1=n2)
f1=f2  <=>  E[l1](f1=l1 & f2=l1)
