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
    private Dog ::= Rex
    Rex barks
}
// make this sentence parse, without moving it
Rex barks
