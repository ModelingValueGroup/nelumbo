import nelumbo.lang
Person   :: Object
Person   ::= Piet, Marie, Klaas
// declare the named pattern TIME, for a time like 9:30
Sentence :: Root
Sentence ::= <Person> arrives at <TIME>          #0,
             <Person> works from <TIME> to <TIME> #0
Marie arrives at 9:30
Klaas works from 8:00 to 17:30
