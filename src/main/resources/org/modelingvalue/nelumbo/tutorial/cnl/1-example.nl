import nelumbo.logic
Article :: Object
Article ::= a, an
Root    ::= noun <Article> <NAME> #100,
            verb <NAME>           #100
Thing   :: Object
{
    Article ar
    NAME    n, v
    noun ar n ::> {
        Root            ::= <{Thing,Literal}> is <ar> n #0
        Root            ::= is <Thing> <ar> n ?         #0
        private Boolean ::= n(<Thing>)
        Thing x
        is x ar n ? ::> {
            n(x) ?
        }
        {Thing,Literal} t
        t is ar n ::> {
            fact n(t)
        }
    }
    verb v ::> {
        Root            ::= <{Thing,Literal}> v <{Thing,Literal}> #0
        Root            ::= who v <Thing> ?                       #0
        private Boolean ::= v(<Thing>,<Thing>)
        Thing x, y
        who v y ? ::> {
            v(x,y) ?
        }
        {Thing,Literal} s, o
        s v o ::> {
            fact v(s,o)
        }
    }
}

noun a dog
noun an owl
noun a cat
verb chases

Thing ::= Rex, Hoot, Tom
Rex is a dog
Hoot is an owl
Tom is a cat
Rex chases Tom
Tom chases Hoot

is Rex a dog ?
is Hoot an owl ?
is Tom a dog ?
who chases Hoot ?
