// Bug: a functor alternative that is a lone type hole (`Cell ::= blank, <Integer>`) is
// accepted, but from then on the parser crashes (NullPointerException) on every Integer
// expression, also unrelated ones like `1+1=v`.
// Correct: the coercion works, or (if a lone hole is illegal) a diagnostic at the declaration.
import nelumbo.integers

Cell    :: Object
Cell    ::= blank, <Integer>
Integer ::= num(<Cell>)
Integer k, v

num(blank)=v <=> v=0
num(k)=v     <=> v=k

1+1=v        ? [(v=2)][..]
num(blank)=v ? [(v=0)][..]
num(3)=v     ? [(v=3)][..]
