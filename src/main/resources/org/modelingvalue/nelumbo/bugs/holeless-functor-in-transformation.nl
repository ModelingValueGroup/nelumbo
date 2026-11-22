// Bug: a functor without holes declared in a transformation body (`Thing ::= n`) clashes with
// the variable it is named by ("Non deterministic pattern merge") and makes the closing `}` of
// that body fail to parse ("Unexpected token '}'"). The same functor with a hole
// (`Thing ::= n(<Thing>)`) or in a plain `{ }` block parses.
import nelumbo.logic

Thing :: Object
Root  ::= noun <NAME> #100
{
    NAME n
    noun n ::> {
        Thing ::= n
    }
}

noun dog
Thing t

t = dog ? [(t=dog)][..]
