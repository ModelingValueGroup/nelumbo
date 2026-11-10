import nelumbo.lang
Person   :: Object
Dog      :: Object
Person   ::= Piet, Marie
Sentence :: Root
Sentence ::= <Person> reads #0,
             <Dog> barks    #0
// these two functors clash: restrict each to the sentence that uses it
Person   ::= friend(<Person>)
Dog      ::= friend(<Person>)
friend(Marie) reads
friend(Piet) barks
