
import    org.modelingvalue.nelumbo.test.friendsTest

<Boolean> ::= <Person> is <Person>   #30
<Person>  ::= a friend of <Person>   #35

<Person> X, Y, Who

X is Y             <=> X=Y
a friend of X = Y  <=> friend(X)=Y

Who is a friend of Piet   ?   [(Who=Jan),(Who=Klaas),(Who=Piet)][..]

