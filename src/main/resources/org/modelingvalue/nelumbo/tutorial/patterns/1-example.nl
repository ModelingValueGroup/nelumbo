import nelumbo.lang
Person   :: Object
Person   ::= Piet, Jan, Marie, Klaas
Sentence :: Root
Sentence ::= <Person> <(> really <)?> <(> likes <|> loves <)> <Person> #0,
             <Person> is <NUMBER> years old                            #0,
             <Person> says <STRING>                                    #0,
             party with <(> <Person> <,> , <)+>                        #0
Piet likes Marie
Piet really loves Marie
Klaas is 42 years old
Marie says "hello"
party with Piet, Jan, Marie
