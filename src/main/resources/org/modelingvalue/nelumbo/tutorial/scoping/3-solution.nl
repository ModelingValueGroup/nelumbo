import nelumbo.lang
Person   :: Object
Dog      :: Object
Sentence :: Root
Sentence ::= <Person> reads #0,
             <Dog> barks    #0
{
    private Person ::= Max
    Max reads
}
{
    Dog ::= Rex
    Rex barks
}
Rex barks
