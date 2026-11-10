import nelumbo.logic
Person  :: Object
Person  ::= Piet, Jan
City    :: Object
City    ::= Amsterdam, Utrecht
// everybody was born in exactly one city: change the pattern so the expected results hold
Boolean ::= <Person> was born in <City> #30
City   c
fact Jan was born in Utrecht
Jan was born in c         ? [(c=Utrecht)][..]
Jan was born in Amsterdam ? [][()]
