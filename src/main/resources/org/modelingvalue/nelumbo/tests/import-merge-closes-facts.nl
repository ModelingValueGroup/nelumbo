// Regression test (fixed 2026-10-28; was bugs/import-merge-closes-facts.nl).
// Two imported modules asserting facts under the same lookup must not close it:
// r(X,q) is an open result, and a set-builder over it an open set (the bug:
// [(q=Y),(q=Z)][..] and a closed set).
import nelumbo.collections
import org.modelingvalue.nelumbo.tests.importmerge.a
import org.modelingvalue.nelumbo.tests.importmerge.b

Person      q
Set<Person> s

r(X,q)          ? [(q=Y),(q=Z),..][..]
{[q](r(X,q))}=s ? [(s={Y,Z,..})][..]
