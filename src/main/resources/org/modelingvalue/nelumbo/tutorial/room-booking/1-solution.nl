import nelumbo.collections
pattern TIME ::= <[> <Integer#100> : <Integer#100> <]>
Slot      :: Literal
Slot      ::= <TIME> to <TIME> #35
Room      :: Object
Room      ::= Blue, Red, Green, Yellow
Set<Slot> ::= the bookings for <Room> #60
Boolean   ::= <Integer> falls in <Slot>                 #30,
              <Integer> is the start of <Slot>          #30,
              <Room> is free at <TIME>                  #30,
              <Room> is free between <TIME> and <TIME>  #30,
              <Room> has clashing bookings              #30
Integer   h1, m1, h2, m2, n
Slot      b, c
Room      r
n falls in h1:m1 to h2:m2         <=> h1*60+m1 <= n & n < h2*60+m2
n is the start of h1:m1 to h2:m2  <=> n = h1*60+m1
r is free at h1:m1                <=> A[b]((b in the bookings for r) -> !(h1*60+m1 falls in b))
r is free between h1:m1 and h2:m2 <=> r is free at h1:m1 &
                                      A[b]((b in the bookings for r) -> !E[n](n is the start of b & h1*60+m1 <= n & n < h2*60+m2))
r has clashing bookings           <=> E[b,c](b in the bookings for r & c in the bookings for r & b != c &
                                             E[n](n is the start of c & n falls in b))

// ---- given: today's bookings, complete per room, and the answers they should give ----
fact the bookings for Blue  = {9:30 to 11:00, 13:00 to 14:30}
fact the bookings for Red   = {9:00 to 10:30, 10:00 to 11:00}
fact the bookings for Green = {}

Blue is free at 10:00                ? [][()]
Blue is free at 12:00                ? [()][]
Blue is free at 11:00                ? [()][]
Blue is free between 11:00 and 13:00 ? [()][]
Blue is free between 10:30 and 12:00 ? [][()]
Blue is free between 12:00 and 15:00 ? [][()]
Green is free at 10:00               ? [()][]
Yellow is free at 10:00              ? [..][..]
Red has clashing bookings            ? [()][]
Blue has clashing bookings           ? [][()]
