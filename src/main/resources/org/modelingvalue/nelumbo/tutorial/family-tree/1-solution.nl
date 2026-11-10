import nelumbo.logic
Person  :: Object
Male    :: Person
Female  :: Person
Male    ::= Bernhard, Claus, Alexander, Friso
Female  ::= Juliana, Beatrix, Irene, Margriet, Amalia
Boolean ::= <one Person> is the mother of <Person> #30,
            <one Person> is the father of <Person> #30,
            <Person> is female                     #30,
            <Person> is a parent of <Person>       #30,
            <Person> is a grandparent of <Person>  #30,
            <Person> is a grandchild of <Person>   #30,
            <Person> is an ancestor of <Person>    #30,
            <Person> is a sister of <Person>       #30,
            <Person> is a brother of <Person>      #30,
            <Person> is an aunt of <Person>        #30
Person  p, c, g, a, s, x, Who
Female  f
Male    m
f is female             <=> true
m is female             <=> false
p is a parent of c      <=> p is the mother of c | p is the father of c
g is a grandparent of c <=> E[p](g is a parent of p & p is a parent of c)
c is a grandchild of g  <=> g is a grandparent of c
a is an ancestor of c   <=> a is a parent of c | E[p](a is a parent of p & p is an ancestor of c)
s is a sister of x      <=> s is female & s != x & E[p](p is a parent of x & p is a parent of s)
s is a brother of x     <=> !(s is female) & s != x & E[p](p is a parent of x & p is a parent of s)
s is an aunt of c       <=> E[p](p is a parent of c & s is a sister of p)

// ---- given: make these facts parse and these questions give the expected answers ----
fact Juliana   is the mother of Beatrix, Juliana  is the mother of Irene, Juliana  is the mother of Margriet
fact Bernhard  is the father of Beatrix, Bernhard is the father of Irene, Bernhard is the father of Margriet
fact Beatrix   is the mother of Alexander, Beatrix is the mother of Friso
fact Claus     is the father of Alexander, Claus   is the father of Friso
fact Alexander is the father of Amalia

Who is the mother of Beatrix   ? [(Who=Juliana)][..]
Irene is the mother of Beatrix ? [][()]
Who is a parent of Beatrix     ? [(Who=Bernhard),(Who=Juliana)][..]
Who is a grandparent of Amalia ? [(Who=Beatrix),(Who=Claus),..][..]
Who is a grandchild of Juliana ? [(Who=Alexander),(Who=Friso),..][..]
Who is an ancestor of Amalia   ? [(Who=Alexander),(Who=Beatrix),(Who=Bernhard),(Who=Claus),(Who=Juliana),..][..]
Who is a sister of Beatrix     ? [(Who=Irene),(Who=Margriet),..][(Who=Beatrix),..]
Who is a brother of Alexander  ? [(Who=Friso),..][(Who=Alexander),..]
Friso is a sister of Alexander ? [][()]
Who is an aunt of Alexander    ? [(Who=Irene),(Who=Margriet),..][..]
