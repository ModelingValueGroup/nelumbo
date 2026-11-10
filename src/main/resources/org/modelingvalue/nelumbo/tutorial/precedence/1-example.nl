import nelumbo.lang
Expr     :: Object
Expr     ::= one, two, three
Expr     ::= <Expr> plus <Expr>  #40,
             <Expr> times <Expr> #50
Sentence :: Root
Sentence ::= compute <Expr>  #0,
             twice <Expr#45> #0
// one plus (two times three)
compute one plus two times three
// (one times two) plus three
compute one times two plus three
twice two times three
