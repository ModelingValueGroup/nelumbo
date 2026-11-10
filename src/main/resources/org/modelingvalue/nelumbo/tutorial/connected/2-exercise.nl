import nelumbo.lang
Person   :: Object
Person   ::= Piet, Marie
Time     :: Object
Time     ::= <[> <NUMBER> : <NUMBER> <]>
// declare the type Date, with connected tokens like Time
Sentence :: Root
Sentence ::= <Person> arrives at <Time>   #0,
             <Person> was born on <Date> #0
Piet arrives at 9:30
Marie was born on 1990-05-17
