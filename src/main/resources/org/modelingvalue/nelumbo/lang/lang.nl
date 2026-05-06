
 TopNamespace  :: List<Root>, Root, Namespace #TOP
 RootNamespace :: Root, Namespace
 
 TopNamespace  ::= <BEGINOFFILE> <(> <(> <List<Root>> <|> <Root> <)> <NEWLINE> <)*> <ENDOFFILE>  @org.modelingvalue.nelumbo.lang.Namespace
 RootNamespace ::= { <(> <(> <List<Root>> <|> <Root> <)> <NEWLINE> <)*> }                        @org.modelingvalue.nelumbo.lang.Namespace
 
 Root          ::= "import" <(> <(> <NAME> <,> . <)+> <,> , <)+>                                 @org.modelingvalue.nelumbo.lang.Import,
                   <Root#0> ::> <RootNamespace>                                                  @org.modelingvalue.nelumbo.lang.Transform,
                   <(> "hidden" <)?> <Type#100> <(> <NAME> <,> , <)+>                            @org.modelingvalue.nelumbo.lang.Variable,
                   <NAME> <(> < <Type#100> > <)?> :: <(> <Type#100> <,> , <)+> <(> # <NAME> <)?> @org.modelingvalue.nelumbo.lang.Type

 Type T
 
 T ::= ( <T> )     @org.modelingvalue.nelumbo.lang.Parenthesized
