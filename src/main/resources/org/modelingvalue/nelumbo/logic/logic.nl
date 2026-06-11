import  nelumbo.lang

Boolean         :: Object
FactType        :: Boolean
Function        :: Object
Literal         :: Object

private Boolean ::= eq(<Literal>,<Literal>)                 @nelumbo.logic.Equal

Boolean         ::= true                                          @nelumbo.logic.NBoolean,
                    false                                         @nelumbo.logic.NBoolean,
                    unknown                                       @nelumbo.logic.NBoolean,
                    ! <Boolean>               #25                 @nelumbo.logic.Not,
                    <Boolean> & <Boolean>     #22                 @nelumbo.logic.And,
                    <Boolean> | <Boolean>     #20                 @nelumbo.logic.Or,
                    E[<(> <Variable#100> <,> , <)+>](<Boolean#0>) @nelumbo.logic.ExistentialQuantifier,
                    A[<(> <Variable#100> <,> , <)+>](<Boolean#0>) @nelumbo.logic.UniversalQuantifier,
                    <Object> = <Object>       #30                 @nelumbo.logic.NIs,
                    <Object> != <Object>      #30,
                    <Boolean> -> <Boolean>    #18,
                    <Boolean> "<->" <Boolean> #16

pattern BINDING ::= [ <(> <(> ( <(> <Variable#100> = <Object#100> <,> , <)*> ) <|> .. <)> <,> , <)*> ]

Root            ::= "fact" <(> <Boolean#0> <,> , <)+>                                      @nelumbo.logic.Fact,
                    <Boolean#0> "<=>" <(> <Boolean#0> <(> "if" <Boolean#0> <)?> <,> , <)+> @nelumbo.logic.Rule,
                    <Boolean#0> ? <(> <BINDING> <BINDING> <)?>                             @nelumbo.logic.Query

Boolean p1, p2

p1->p2  <=>  !p1|p2
p1<->p2 <=>  (p1->p2)&(p2->p1)

Literal  l1, l2
Function f1, f2
Object   n1, n2

l1=l2  <=>  eq(l1, l2)
l1=f1  <=>  f1=l1
n1!=n2 <=>  !(n1=n2)
