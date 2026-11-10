import nelumbo.lang
Person   :: Object
Sentence :: Root
Sentence ::= <Person> reads #0
{
    private Person ::= Max
    Person         ::= Piet
    Max reads
    {
        Max reads
    }
}
Piet reads
// Max reads   does not parse here: Max is private to the block
