import nelumbo.lang
Expr     :: Object
Expr     ::= one, two, three
Expr     ::= <Expr> plus <Expr>  #40,
             <Expr> times <Expr> #50
Sentence :: Root
// change only the precedence of the hole so both sentences parse
Sentence ::= twice <Expr#45> #0
twice two times three
twice one plus two
