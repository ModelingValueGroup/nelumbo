import nelumbo.integers
Vec        :: Object
struct Vec ::= vec(<Integer>, <Integer>)
Vec        ::= <Vec> + <Vec>     #40,
               <Vec> - <Vec>     #40,
               <Integer> * <Vec> #50
Integer    ::= <Vec> . <Vec>      #50
Boolean    ::= <Vec> is perpendicular to <Vec> #30
Vec     u, v
Integer a, b, c, d, k
vec(a,b) + vec(c,d)     ==  vec(a+c, b+d)
vec(a,b) - vec(c,d)     ==  vec(a-c, b-d)
k * vec(a,b)            ==  vec(k*a, k*b)
vec(a,b) . vec(c,d)     ==  a*c + b*d
u is perpendicular to v <=>  u . v = 0

// ---- given: the questions and the answers they should get ----
vec(1,2) + vec(3,4)                    ? [(vec(4,6))][..]
vec(1,2) - vec(3,4)                    ? [(vec(-2,-2))][..]
2 * vec(1,2)                           ? [(vec(2,4))][..]
vec(1,2) + 2 * vec(1,1)                ? [(vec(3,4))][..]
vec(1,2) . vec(3,4)                    ? [(11)][..]
vec(1+1, 2) = vec(2, 2)                ? [()][]
vec(1,2) + vec(3,4) = vec(4,6)         ? [()][]
vec(1,2) = vec(2,1)                    ? [][()]
vec(2,1) is perpendicular to vec(-1,2) ? [()][]
vec(2,1) is perpendicular to vec(1,2)  ? [][()]
