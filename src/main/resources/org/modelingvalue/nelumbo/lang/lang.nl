// Token Types
SINGLEQUOTE :: NATIVE // '
SEMICOLON   :: NATIVE // ;
COMMA       :: NATIVE // ,
LEFT        :: NATIVE // [\(\[\{]
RIGHT       :: NATIVE // [\)\]\}]
STRING      :: NATIVE // "([^"\\]|\\[\s\S])*"
NUMBER      :: NATIVE // [0-9]+
NAME        :: NATIVE // [a-zA-Z_][0-9a-zA-Z_]*
OPERATOR    :: NATIVE // (?!//)[~!@#$%^&*=+|:<>.?/-]+
NEWLINE     :: NATIVE // \R
BEGINOFFILE :: NATIVE
ENDOFFILE   :: NATIVE

// Object Types
Object           :: NATIVE
Type             :: Object
PatternPart      :: Root
Variable         :: Object
Root             :: Object          // An object in the top of the hierarchy 
Functor          :: Root            // Language pattern with a type, e.g. a function or an operator
Pattern          :: Object #PATTERN // Syntaxtual pattern
Namespace        :: Object          // Local scope type
RootNamespace    :: Root, Namespace

pattern PATTERNS ::= <(> <Pattern#100> <)+>
pattern QNAME    ::= <[> <(> <NAME> <,> . <)+> <]>

Namespace        ::= <BEGINOFFILE> <(> <(> <List<Root>> <|> <Root> <)> <NEWLINE> <)*> <ENDOFFILE>  @nelumbo.lang.Namespace
RootNamespace    ::= { <(> <(> <List<Root>> <|> <Root> <)> <NEWLINE> <)*> }                        @nelumbo.lang.Namespace
PatternPart      ::= "pattern" <NAME> ::= <PATTERNS>                                               @nelumbo.lang.PatternPart

Pattern          ::= <NAME>                                                                                                       @nelumbo.patterns.TokenTextPattern,
                     <STRING>                                                                                                     @nelumbo.patterns.TokenTextPattern,
                     <OPERATOR>                                                                                                   @nelumbo.patterns.TokenTextPattern,
                     <SEMICOLON>                                                                                                  @nelumbo.patterns.TokenTextPattern,
                     <SINGLEQUOTE>                                                                                                @nelumbo.patterns.TokenTextPattern,
                     <COMMA>                                                                                                      @nelumbo.patterns.TokenTextPattern,
                     "<" <Variable#100> ">"                                                                                       @nelumbo.patterns.TokenTextPattern,
                     <[> "<" "(" ">" <]> <(> <PATTERNS> <,> <[> "<" "|" ">" <]> <)+> <[> "<" ")" ">" <]>                          @nelumbo.patterns.AlternationPattern,
                     <[> "<" "(" ">" <]> <PATTERNS> <(> <[> "<" "," ">"  <]> <PATTERNS> <)?> <[> "<" ")" <(> * <|> + <)> ">"  <]> @nelumbo.patterns.RepetitionPattern,
                     <[> "<" "(" ">" <]> <PATTERNS> <[> "<" ")" "?" ">" <]>                                                       @nelumbo.patterns.OptionalPattern,
                     <LEFT> <PATTERNS> <RIGHT>                                                                                    @nelumbo.patterns.SequencePattern,
                     <[> "<" "[" ">" <]> <PATTERNS> <[> "<" "]" ">" <]>                                                           @nelumbo.patterns.SequencePattern,
                     "<" <(> <(> "visible" <|> "hidden" <)> <)?> <Type#100> <(> # <NUMBER> <)?> ">"                               @nelumbo.patterns.NodeTypePattern,
                     "<" <PatternPart#100> ">"                                                                                    @nelumbo.patterns.PatternPartPattern

Type             ::= { <(> <Type> <,> & <)+> }  @nelumbo.lang.Type

Root             ::= "import" <(> <QNAME> <,> , <)+>                                                                        @nelumbo.lang.Import,
                     <Root#0> ::> <RootNamespace>                                                                           @nelumbo.lang.Transform,
                     <(> "hidden" <)?> <Type#100> <(> <NAME> <,> , <)+>                                                     @nelumbo.lang.Variable,
                     <[> <NAME> <(> < <Type#100> > <)?> <]> :: <(> <Type#100> <,> , <)+> <(> # <NAME> <)?>                  @nelumbo.lang.Type,
                     <(> "private" <)?> <Type#100> ::= <(> <PATTERNS> <(> "#" <NUMBER> <)?> <(> "@" <QNAME> <)?> <,> , <)+> @nelumbo.lang.Functor


Type P

P ::= (<P>)       @nelumbo.lang.Parenthesized
