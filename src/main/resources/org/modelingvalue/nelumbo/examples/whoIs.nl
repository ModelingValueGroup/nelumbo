import    org.modelingvalue.nelumbo.examples.friends

Boolean ::= <Person> is a friend of <Person>  #35

Person x, y, Who

x is a friend of y <=>  friends(x, y)

Who is a friend of Piet  ? [(Who=Jan),(Who=Kees),(Who=Klaas),(Who=Piet),..][..]
Kees is a friend of Piet ? [()][]
