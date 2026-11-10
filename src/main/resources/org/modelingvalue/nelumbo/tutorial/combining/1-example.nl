import nelumbo.logic
Person  :: Object
Person  ::= Piet
Food    :: Object
Food    ::= Apple, Pear, Nut, Fish, Bread
Boolean ::= likes(<Person>,<Food>), allergic(<Person>,<Food>), eats(<Person>,<Food>)
Person  p
Food    f
fact likes(Piet, Apple), likes(Piet, Pear)
fact allergic(Piet, Nut), allergic(Piet, Fish)
eats(p,f) <=> likes(p,f)
eats(Piet,f) ?
eats(p,f) <=> false if allergic(p,f)
eats(Piet,f) ?
eats(Piet,Bread) ?
