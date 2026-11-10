import nelumbo.integers
Person     :: Object
Person     ::= Anna, Lisa, Henk, Kees, Marie, Sven, Eva, Piet, Bram
Membership :: Object
Membership ::= regular, weekday, volunteer
Boolean    ::= <Person> is <one Integer> years old        #30,
               <Person> has a <one Membership> membership #30,
               <Person> is a junior                       #30,
               <Person> is an adult                       #30,
               <Person> is a senior                       #30
Integer    ::= <Person>'s base fee #60,
               <Person>'s fee      #60
Person     p
Integer    n
p is a junior <=> E[n](p is n years old & n<18)
p is an adult <=> E[n](p is n years old & n>=18 & n<65)
p is a senior <=> E[n](p is n years old & n>=65)
p's base fee == 50 if p is a junior, 120 if p is an adult, 84 if p is a senior
p's fee == p's base fee      if p has a regular membership,
           p's base fee - 20 if p has a weekday membership,
           0                 if p has a volunteer membership

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
