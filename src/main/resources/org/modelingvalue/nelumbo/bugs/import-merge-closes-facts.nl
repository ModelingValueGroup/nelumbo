// Bug: two imported modules asserting facts under the same lookup closes it:
// r(X,q) answers [(q=Y),(q=Z)][..] instead of an open result, and a set-builder
// over it becomes a closed set.
import nelumbo.collections
import org.modelingvalue.nelumbo.bugs.importmerge.a
import org.modelingvalue.nelumbo.bugs.importmerge.b

Person      q
Set<Person> s

r(X,q)          ? [(q=Y),(q=Z),..][..]
{[q](r(X,q))}=s ? [(s={Y,Z,..})][..]
