// Regression test (fixed 2026-10-11; was bugs/multi-unbound-fact-lookup-unknown.nl).
// A fact lookup with more than one open argument, like salary(Piet,m,s), must
// enumerate the matching facts. (Was: Predicate.infer answered unknown for more
// than one unbound argument, and once facts were looked up first, getFacts answered
// "complete, no facts" because KnowledgeBase.addFact only indexed one open argument
// at a time. Fixed: addFact also files every fact under a per-functor key, and
// getFacts filters that set for lookups with two or more open arguments.)
// Covers: two and three open arguments, a bound argument that filters, a variable
// of a subtype (Manager), no matching facts, a repeated variable (pair(a,a)) and a
// variable nested in an argument (stays unknown - not supported by the filter).
import nelumbo.integers

Person   :: Object
Manager  :: Person
Box      :: Struct

FactType ::= salary(<Person>,<Integer>,<Integer>),
             pair(<Person>,<Person>),
             owns(<Person>,<Box>),
             boss(<Person>)

Box      ::= box(<Integer>)

Person   ::= Piet, Jan
Manager  ::= Klaas

Person   p, a
Manager  k
Integer  m, s, n

fact salary(Piet, 1, 3000),
     salary(Piet, 7, 4000),
     salary(Klaas, 1, 5000),
     pair(Piet, Piet),
     pair(Piet, Klaas),
     owns(Piet, box(1)),
     boss(Klaas)

// one open argument: the existing index, unchanged
salary(Piet,1,s)    ? [(s=3000)][..]
salary(Piet,m,4000) ? [(m=7)][..]
salary(Jan,1,s)     ? [][..]

// two open arguments
salary(Piet,m,s)    ? [(m=1,s=3000),(m=7,s=4000)][..]
salary(Jan,m,s)     ? [][..]

// three open arguments
salary(p,m,s)       ? [(p=Piet,m=1,s=3000),(p=Piet,m=7,s=4000),(p=Klaas,m=1,s=5000)][..]

// variable of a subtype: only the Manager's facts
salary(k,m,s)       ? [(k=Klaas,m=1,s=5000)][..]

// repeated variable: only facts with equal arguments
pair(a,a)           ? [(a=Piet)][..]

// variable nested in an argument: not supported, must stay unknown
owns(p,box(n))      ? [..][..]
owns(Piet,box(n))   ? [..][..]

// unary fact with an unbound argument: enumerated (was unknown)
boss(p)             ? [(p=Klaas)][..]

// a fact conjunct binds the variable for the next conjunct
pair(Piet,a) & pair(a,a) ? [(a=Piet)][(a=Klaas),..]
