import nelumbo.lang
Expr     :: Object
Expr     ::= one, two, three
Expr     ::= <Expr> plus <Expr>  #40,
             <Expr> times <Expr> #50
// add square brackets as a second way to group expressions
Sentence :: Root
Sentence ::= compute <Expr> #0
compute (one plus two) times three
compute [one plus two] times three
