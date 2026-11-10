import nelumbo.integers

// build your language here: the vector type, operators, variables and rules



// ---- given: the questions and the answers they should get ----
vec(1,2) + vec(3,4)                     ? [(vec(4,6))][..]
vec(1,2) - vec(3,4)                     ? [(vec(-2,-2))][..]
2 * vec(1,2)                            ? [(vec(2,4))][..]
vec(1,2) + 2 * vec(1,1)                 ? [(vec(3,4))][..]
vec(1,2) . vec(3,4)                     ? [(11)][..]
vec(1+1, 2) = vec(2, 2)                 ? [()][]
vec(1,2) + vec(3,4) = vec(4,6)          ? [()][]
vec(1,2) = vec(2,1)                     ? [][()]
vec(2,1) is perpendicular to vec(-1,2)  ? [()][]
vec(2,1) is perpendicular to vec(1,2)   ? [][()]
