 
 // Token Types
 SINGLEQUOTE   :: NATIVE // '
 SEMICOLON     :: NATIVE // ;
 COMMA         :: NATIVE // ,
 LEFT          :: NATIVE // [\(\[\{]
 RIGHT         :: NATIVE // [\)\]\}]
 STRING        :: NATIVE // "([^"\\]|\\[\s\S])*"
 DECIMAL       :: NATIVE // -?[0-9]+\.[0-9]+
 NUMBER        :: NATIVE // -?[0-9]+(#[0-9a-zA-Z]+)?
 NAME          :: NATIVE // [a-zA-Z_][0-9a-zA-Z_]*
 OPERATOR      :: NATIVE // (?!//)[~!@#$%^&*=+|:<>.?/-]+
 NEWLINE       :: NATIVE // \R
 BEGINOFFILE   :: NATIVE 
 ENDOFFILE     :: NATIVE 

 // Object Types
 Object        :: NATIVE
 Type          :: Object
 Variable      :: Object
 Root          :: Object          // An object in the top of the hierarchy 
 Functor       :: Root            // Language pattern with a type, e.g. a function or an operator
 Pattern       :: Object #PATTERN // Syntaxtual pattern
 Namespace     :: Object          // Local scope type
 RootNamespace :: Root, Namespace // 
 
 Namespace     ::= <BEGINOFFILE> <(> <(> <List<Root>> <|> <Root> <)> <NEWLINE> <)*> <ENDOFFILE>  @nelumbo.lang.Namespace
 RootNamespace ::= { <(> <(> <List<Root>> <|> <Root> <)> <NEWLINE> <)*> }                        @nelumbo.lang.Namespace
 
 Root          ::= "import" <(> <(> <NAME> <,> . <)+> <,> , <)+>                                 @nelumbo.lang.Import,
                   <Root#0> ::> <RootNamespace>                                                  @nelumbo.lang.Transform,
                   <(> "hidden" <)?> <Type#100> <(> <NAME> <,> , <)+>                            @nelumbo.lang.Variable,
                   <NAME> <(> < <Type#100> > <)?> :: <(> <Type#100> <,> , <)+> <(> # <NAME> <)?> @nelumbo.lang.Type

 Type T
 
 T ::= (<T>)       @nelumbo.lang.Parenthesized
