import nelumbo.lang
Expr     :: Object
Expr     ::= one, two, three
Expr     ::= <Expr> plus <Expr>  #40,
             <Expr> times <Expr> #50
Sentence :: Root
Sentence ::= twice <Expr#35> #0
twice two times three
twice one plus two
