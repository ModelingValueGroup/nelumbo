import nelumbo.lang
Person   :: Object
Person   ::= Piet, Marie
Time     :: Object
Time     ::= <[> <NUMBER> : <NUMBER> <]>
Sentence :: Root
Sentence ::= <Person> arrives at <Time> #0
Piet arrives at 9:30
Marie arrives at 14:05
// Piet arrives at 9 : 30   does not parse
