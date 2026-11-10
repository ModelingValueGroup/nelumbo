import nelumbo.logic
Person  :: Object
Person  ::= Piet, Jan
City    :: Object
City    ::= Amsterdam, Utrecht
Boolean ::= <Person> was born in <one City> #30
City   c
fact Jan was born in Utrecht
Jan was born in c         ? [(c=Utrecht)][..]
Jan was born in Amsterdam ? [][()]
