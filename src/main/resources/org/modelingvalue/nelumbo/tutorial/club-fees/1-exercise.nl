import nelumbo.integers

// build your language here: types, members, sentences, variables and rules



// ---- given: the club's rules, its members, and the answers they should get ----
// Juniors (younger than 18) pay 50 euros, adults 120 euros, seniors (65 or older) 84 euros.
// Weekday members pay 20 euros less. Volunteers pay nothing.
fact Anna is 30 years old, Lisa is 16 years old, Henk is 70 years old, Kees is 12 years old,
     Marie is 45 years old, Sven is 18 years old, Eva is 65 years old
fact Anna has a regular membership, Lisa has a weekday membership, Henk has a weekday membership,
     Kees has a regular membership, Marie has a volunteer membership, Sven has a regular membership,
     Eva has a regular membership, Piet has a weekday membership, Bram has a volunteer membership

Lisa is a junior              ? [()][]
Henk is a junior              ? [][()]
Sven is an adult              ? [()][]
Eva is a senior               ? [()][]
Anna is 31 years old          ? [][()]
Anna has a weekday membership ? [][()]
Anna's fee                    ? [(120)][..]
Lisa's fee                    ? [(30)][..]
Henk's fee                    ? [(64)][..]
Kees's fee                    ? [(50)][..]
Marie's fee                   ? [(0)][..]
Sven's fee                    ? [(120)][..]
Eva's fee                     ? [(84)][..]
Piet's fee                    ? [..][..]
Bram's fee                    ? [(0)][..]
