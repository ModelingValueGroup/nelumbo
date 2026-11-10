import nelumbo.lang
Person   :: Object
Dog      :: Object
Person   ::= Piet, Marie
Sentence :: Root
Sentence ::= <Person> reads #0,
             <Dog> barks    #0
{
    private Person ::= friend(<Person>)
    friend(Marie) reads
}
{
    private Dog ::= friend(<Person>)
    friend(Piet) barks
}
