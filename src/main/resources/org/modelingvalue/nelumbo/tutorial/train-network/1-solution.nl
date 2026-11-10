import nelumbo.integers
City    :: Object
City    ::= Utrecht, Amsterdam, Haarlem, Leiden, Delft, Rotterdam, Gouda, Zwolle, Groningen
Boolean ::= there is a train between <City> and <City>                #30,
            <City> is connected to <City>                             #30,
            I can travel from <City> to <City>                        #30,
            I can travel from <City> to <City> in at most <Integer> steps #30
City    a, b, c, Which
Integer n
a is connected to b <=> there is a train between a and b | there is a train between b and a
I can travel from a to b <=> a is connected to b | E[c](a is connected to c & I can travel from c to b)
I can travel from a to b in at most n steps <=> n>0 & E[c](a is connected to c & (c=b | I can travel from c to b in at most n-1 steps))

// ---- given: the timetable and the answers it should give ----
fact there is a train between Utrecht and Amsterdam, there is a train between Amsterdam and Haarlem,
     there is a train between Haarlem and Leiden,    there is a train between Leiden and Delft,
     there is a train between Delft and Rotterdam,   there is a train between Rotterdam and Gouda,
     there is a train between Gouda and Utrecht,     there is a train between Zwolle and Groningen

Utrecht is connected to Amsterdam                      ? [()][]
Amsterdam is connected to Utrecht                      ? [()][]
Which is connected to Utrecht                          ? [(Which=Amsterdam),(Which=Gouda),..][..]
Utrecht is connected to Leiden                         ? [..][..]
I can travel from Utrecht to Leiden                    ? [()][]
I can travel from Utrecht to Groningen                 ? [..][..]
I can travel from Utrecht to Which                     ? [(Which=Amsterdam),(Which=Delft),(Which=Gouda),(Which=Haarlem),(Which=Leiden),(Which=Rotterdam),(Which=Utrecht),..][..]
I can travel from Utrecht to Leiden in at most 3 steps ? [()][]
I can travel from Utrecht to Leiden in at most 2 steps ? [..][..]
I can travel from Utrecht to Delft in at most 3 steps  ? [()][]
I can travel from Utrecht to Delft in at most 0 steps  ? [][()]
