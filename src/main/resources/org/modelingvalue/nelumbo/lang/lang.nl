
 Type          :: Object
 Global        :: Object
 Namespace     :: Object
 Root          :: Object
 Variable      :: Object
 Pattern       :: Object #PATTERN
 Functor       :: Root
 RootNamespace :: List<Root>, Root, Namespace
 
 RootNamespace ::= <BEGINOFFILE> <(> <(> <List<Root>> <|> <Root> <)> <NEWLINE> <)*> <ENDOFFILE>  @nelumbo.lang.Namespace,
                   { <(> <(> <List<Root>> <|> <Root> <)> <NEWLINE> <)*> }                        @nelumbo.lang.Namespace
 
 Root          ::= "import" <(> <(> <NAME> <,> . <)+> <,> , <)+>                                 @nelumbo.lang.Import,
                   <Root#0> ::> <RootNamespace>                                                  @nelumbo.lang.Transform,
                   <(> "hidden" <)?> <Type#100> <(> <NAME> <,> , <)+>                            @nelumbo.lang.Variable,
                   <NAME> <(> < <Type#100> > <)?> :: <(> <Type#100> <,> , <)+> <(> # <NAME> <)?> @nelumbo.lang.Type

 Type T
 
 T ::= (<T>)       @nelumbo.lang.Parenthesized
