import nelumbo.lang
Expr     :: Object
Expr     ::= one, two, three
Expr     ::= <Expr> plus <Expr>  #40,
             <Expr> minus <Expr> #40,
             <Expr> times <Expr> #50
Sentence :: Root
Sentence ::= compute <Expr> #0
compute three minus one times two
