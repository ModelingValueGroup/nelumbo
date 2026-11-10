import nelumbo.integers
Person  :: Object
Person  ::= Piet, Jan
Integer ::= age(<Person>)
Boolean ::= adult(<Person>)
Person p
fact age(Piet)=40, age(Jan)=12
fact adult(Piet)
adult(Piet) ? [()][]
adult(Jan)  ? [..][..]          // unknown: no fact, no rule
adult(p) <=> false if age(p)<18
adult(Piet) ? [()][]            // unchanged
adult(Jan)  ? [][()]            // now false: only the falsehood side grew
