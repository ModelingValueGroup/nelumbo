import nelumbo.lang
Expr     :: Object
Expr     ::= one, two, three
// give minus a precedence, the same as plus
Expr     ::= <Expr> plus <Expr>  #40,
             <Expr> minus <Expr>,
             <Expr> times <Expr> #50
Sentence :: Root
Sentence ::= compute <Expr> #0
compute three minus one times two
