import nelumbo.integers

// build your language here: cities, sentences, variables and rules



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
