
 Transform :: Root, Namespace
 
 Transform ::= <Root#0> ::> { <(> <(> <List<Root>> <|> <Root> <)> <NEWLINE> <)*> } @org.modelingvalue.nelumbo.lang.Transform
 
 Root ::= "import" <(> <(> <NAME> <,> . <)+> <,> , <)+>                            @org.modelingvalue.nelumbo.lang.Import
