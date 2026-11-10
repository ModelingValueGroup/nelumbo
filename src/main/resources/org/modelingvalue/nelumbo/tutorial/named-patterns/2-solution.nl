import nelumbo.lang
Person   :: Object
Person   ::= Piet, Marie, Klaas
pattern TIME ::= <[> <NUMBER> : <NUMBER> <]>
Sentence :: Root
Sentence ::= <Person> arrives at <TIME>          #0,
             <Person> works from <TIME> to <TIME> #0
Marie arrives at 9:30
Klaas works from 8:00 to 17:30
