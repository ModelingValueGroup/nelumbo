// Bug: a pattern whose only argument is a unique hole (winner(<one Person>)) is never
// closed: after fact winner(Piet), winner(Jan) is unknown instead of false and
// winner(p) is unknown instead of the single, closed answer p=Piet.
import nelumbo.logic

Person  :: Object

Boolean ::= winner(<one Person>)

Person  ::= Piet, Jan

Person  p

fact winner(Piet)

winner(Piet) ? [()][]
winner(Jan)  ? [][()]
winner(p)    ? [(p=Piet)][..]
