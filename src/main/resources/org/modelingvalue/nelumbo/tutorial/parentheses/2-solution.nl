import nelumbo.lang
Expr     :: Object
Expr     ::= one, two, three
Expr     ::= <Expr> plus <Expr>  #40,
             <Expr> times <Expr> #50
Expr     ::= [<Expr>] @nelumbo.lang.Parenthesized
Sentence :: Root
Sentence ::= compute <Expr> #0
compute (one plus two) times three
compute [one plus two] times three
