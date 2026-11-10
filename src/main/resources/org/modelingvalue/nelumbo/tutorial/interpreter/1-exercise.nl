import nelumbo.integers

// build your language here: expressions, eval, variables and rules



// ---- given: the variables, and the questions with the answers they should get ----
fact x := 3, y := 4

eval #5               ? [(5)][..]
eval x                ? [(3)][..]
eval (x + #2)         ? [(5)][..]
eval (x + y * #2)     ? [(11)][..]
eval ((x + y) * #2)   ? [(14)][..]
eval (z + #1)         ? [..][..]
x + #2 = x + #2       ? [()][]
x + #2 = #2 + x       ? [][()]
