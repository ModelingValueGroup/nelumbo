// Bug: KnowledgeBase.addFact only indexes generalizations one argument at a time, so
// the diagonal shape r(a,a) has no index entry and getFacts (KnowledgeBase.java:560)
// answers "complete, no facts" for it.
// Correct: r(X,X) is a fact, so X witnesses E[a](r(a,a)) - it must be true.
// Actual: definitively false. The last two queries have no expectation on purpose
// (they illustrate; their exact correct completeness is debatable).
import nelumbo.logic

Person   :: Object

FactType ::= r(<Person>,<Person>)

Person   ::= X, Y

Person a, b

fact r(X,X)

r(X,X)             ? [()][]
E[a](r(a,a))       ? [()][..]
E[a](E[b](r(a,b))) ?
