
    <String>   :: <Node>

    <String>   ::= <STRING>                           @org.modelingvalue.nelumbo.strings.String

    <Relation> ::= string_concat(<String>,<String>,<String>)   @org.modelingvalue.nelumbo.strings.Concat

    <String>   ::=  <String> +  <String>  #40

    <String>  a, b, c

    a+b=c <==  string_concat(a,b,c)
