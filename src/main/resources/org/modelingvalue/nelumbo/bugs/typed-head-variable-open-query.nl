// Bug: a rule whose head variable has a narrower type than the query variable
// (`Female f`, `f is a sister of x <=> f is a sibling of x`) returns bindings of the wrong type
// for an open query: `Who is a sister of Piet ?` lists the male Jan. The ground query
// `Jan is a sister of Piet ?` is unknown when asked first, but true after the open query.
import nelumbo.logic

Person  :: Object
Male    :: Person
Female  :: Person
Male    ::= Jan, Piet
Female  ::= Marie
Boolean ::= <Person> is a sibling of <Person> #30,
            <Person> is a sister of <Person>  #30
Person  x, Who
Female  f

fact Jan is a sibling of Piet, Marie is a sibling of Piet

f is a sister of x <=> f is a sibling of x

Who is a sister of Piet   ? [(Who=Marie),..][..]
Jan is a sister of Piet   ? [..][..]
Marie is a sister of Piet ? [()][]
