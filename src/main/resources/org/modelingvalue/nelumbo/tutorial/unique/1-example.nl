import nelumbo.logic
Person  :: Object
Person  ::= Piet, Jan
City    :: Object
City    ::= Amsterdam, Utrecht
Boolean ::= <Person> lives in <one City> #30,
            <Person> likes <City>        #30
City   c
Person p
fact Piet lives in Amsterdam
fact Piet likes Amsterdam
Piet lives in c ?
Piet lives in Utrecht ?
Jan lives in c ?
p lives in Amsterdam ?
Piet likes c ?
Piet likes Utrecht ?
