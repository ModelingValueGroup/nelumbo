import nelumbo.lang
Expr     :: Object
Expr     ::= one, two, three
Expr     ::= <Expr> plus <Expr>  #40,
             <Expr> times <Expr> #50
Person   :: Object
Person   ::= Piet, Marie
Person   ::= father(<Person>)
Sentence :: Root
Sentence ::= compute <Expr>       #0,
             twice <Expr#45>      #0,
             <Person> knows <Person> #0
compute (one plus two) times three
twice (one plus two)
twice ((one))
(father(Marie)) knows (Piet)
