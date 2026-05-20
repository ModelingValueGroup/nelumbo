 
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
 RootNamespace :: Root, Namespace
 
 Namespace     ::= <BEGINOFFILE> <(> <Root> <NEWLINE> <)*> <ENDOFFILE>  @nelumbo.lang.Namespace
 RootNamespace ::= { <(> <Root> <NEWLINE> <)*> }                        @nelumbo.lang.Namespace
 
 Pattern       ::= <NAME>                 @nelumbo.patterns.TokenTextPattern,
                   <STRING>               @nelumbo.patterns.TokenTextPattern,
                   <OPERATOR>             @nelumbo.patterns.TokenTextPattern,
                   <SEMICOLON>            @nelumbo.patterns.TokenTextPattern,
                   <SINGLEQUOTE>          @nelumbo.patterns.TokenTextPattern,
                   <COMMA>                @nelumbo.patterns.TokenTextPattern,
                   "<" <Variable#100> ">" @nelumbo.patterns.TokenTextPattern,
                   "<" "(" ">" <(> <(> <Pattern#100> <)+> <,> "<" "|" ">" <)+> "<" ")" ">"                                     @nelumbo.patterns.AlternationPattern,
                   "<" "(" ">" <(> <Pattern#100> <)+> <(> "<" "," ">" <(> <Pattern#100> <)+> <)?>  "<" ")" <(> * <|> + <)> ">" @nelumbo.patterns.RepetitionPattern,
                   "<" "(" ">" <(> <Pattern#100> <)+> "<" ")" "?" ">"                                                          @nelumbo.patterns.OptionalPattern,
                   <LEFT> <(> <Pattern#100> <)+> <RIGHT>                                                                       @nelumbo.patterns.SequencePattern,
                   "<" <(> <(> "visible" <|> "hidden" <)> <)?> <Type#100> <(> # <NUMBER> <)?> ">"                              @nelumbo.patterns.NodeTypePattern

 Root          ::= "import" <(> <(> <NAME> <,> . <)+> <,> , <)+>                                 @nelumbo.lang.Import,
                   <Root#0> ::> <RootNamespace>                                                  @nelumbo.lang.Transform,
                   <(> "hidden" <)?> <Type#100> <(> <NAME> <,> , <)+>                            @nelumbo.lang.Variable,
                   <NAME> <(> < <Type#100> > <)?> :: <(> <Type#100> <,> , <)+> <(> # <NAME> <)?> @nelumbo.lang.Type,
                   <(> "private" <)?> <Type#100> ::= <(> <(> <Pattern#100> <)+> <(> # <NUMBER> <)?> <(> @ <(> <NAME> <,> . <)+> <)?> <,> , <)+>  @nelumbo.lang.Functor

 Type T
 
 T ::= (<T>)       @nelumbo.lang.Parenthesized
