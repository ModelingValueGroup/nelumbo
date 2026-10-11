// Regression test (fixed 2026-10-11).
// Importing several modules that add facts to the same fact type must keep all of
// their facts.
// (Was: KnowledgeBase.merge copied the fact index with Map.addAll. Two modules with
// facts under one index key - every fact type with two or more arguments since the
// per-functor key, and r(X,_) for a.nl and c.nl even before - made addAll throw
// (InferResult is not Mergeable): the import failed with "null -> [...]" and the rest
// of that module was not merged. Fixed: merge unites the facts per key.)
import org.modelingvalue.nelumbo.tests.importmerge.a
import org.modelingvalue.nelumbo.tests.importmerge.b
import org.modelingvalue.nelumbo.tests.importmerge.c

Person p, q

r(p,q) ? [(p=X,q=Y),(p=Z,q=W),(p=X,q=W)][..]
r(X,q) ? [(q=Y),(q=W)][..]
r(Z,q) ? [(q=W)][..]
r(p,W) ? [(p=Z),(p=X)][..]
r(X,Y) ? [()][]
