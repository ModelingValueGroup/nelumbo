import nelumbo.logic
Person :: Object
Boolean ::= friends(<Person>,<Person>)
Person ::= Piet, Jan, Klaas, Kees
// add a fact here
friends(Klaas, Kees) ? [()][]
