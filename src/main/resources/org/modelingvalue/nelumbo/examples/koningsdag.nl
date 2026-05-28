
import      nelumbo.integers

DayOfWeek :: Object
DayOfWeek ::= Mon, Tue, Wed, Thu, Fri, Sat, Sun

FactType ::= weekdag van <Integer> april <Integer> is <DayOfWeek>
Boolean  ::= Koningsdag <Integer> is op <Integer> april

Integer    y, d
DayOfWeek  w

Koningsdag y is op d april <=> d=26 if weekdag van 27 april y is Sun,
                                d=27 if E[w](weekdag van 27 april y is w & w!=Sun)

fact weekdag van 27 april 2023 is Thu,
     weekdag van 27 april 2024 is Sat,
     weekdag van 27 april 2025 is Sun,
     weekdag van 27 april 2026 is Mon,
     weekdag van 27 april 2027 is Tue

Koningsdag 2023 is op d april   ? [(d=27)][..]
Koningsdag 2024 is op d april   ? [(d=27)][..]
Koningsdag 2025 is op d april   ? [(d=26)][..]
Koningsdag 2026 is op d april   ? [(d=27)][..]
Koningsdag 2027 is op d april   ? [(d=27)][..]

Koningsdag 2025 is op 26 april  ? [()][]
Koningsdag 2025 is op 27 april  ? [][()]
