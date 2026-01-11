
import      nelumbo.integers

<Person> :: <Node>
 
<Relation>  ::= het inkomen van <Person> is <Integer> euro
<Relation>  ::= <Person> mag <Integer> euro aftrekken
<Predicate> ::= <Person> moet <Integer> euro belasting betalen
 
<Integer> x,i,a
<Person>  p
 
p moet x euro belasting betalen   <=> E[i,a]((het inkomen van p is i euro) & (p mag a euro aftrekken) & x=(i-a)/2)
 
<Person> ::= Piet
 
het inkomen van Piet is 50000 euro 
Piet mag 1000 euro aftrekken
 
het inkomen van Piet is x euro ?                                                       [(x=50000)][..]
Piet mag x euro aftrekken ?                                                            [(x=1000)][..]
 
E[i](het inkomen van Piet is i euro & i=a) ?                                           [(a=50000)][..]
E[i,a]((het inkomen van Piet is i euro) & (Piet mag a euro aftrekken) & x=(i-a)/2) ?   [(x=24500)][..]

Piet moet x euro belasting betalen ?                                                   [(x=24500)][..]
Piet moet 24500 euro belasting betalen ?                                               [()][]
 