import nelumbo.integers
Expr        :: Object
Var         :: Expr
Var         ::= x, y, z
struct Expr ::= # <Integer>     #90,
                <Expr> + <Expr> #40,
                <Expr> * <Expr> #50
Integer     ::= eval <Expr>      #80
Boolean     ::= <Var> := <one Integer> #20
Expr    e, f
Var     v
Integer i
eval #i      ==  i
eval v = i   <=>  v := i
eval (e + f) ==  eval e + eval f
eval (e * f) ==  eval e * eval f

// ---- given: the variables, and the questions with the answers they should get ----
fact x := 3, y := 4

eval #5             ? [(5)][..]
eval x              ? [(3)][..]
eval (x + #2)       ? [(5)][..]
eval (x + y * #2)   ? [(11)][..]
eval ((x + y) * #2) ? [(14)][..]
eval (z + #1)       ? [..][..]
x + #2 = x + #2     ? [()][]
x + #2 = #2 + x     ? [][()]
