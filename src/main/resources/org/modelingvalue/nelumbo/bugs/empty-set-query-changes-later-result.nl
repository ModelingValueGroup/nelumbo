// Bug: a query's result depends on earlier, unrelated queries. Each fN query (aN an
// empty set, a call as the second argument) gives [(sN={})][..] when it is the only
// query in the file, but after `{}={}` and the queries over the other element types
// most give [..][..]. Hash-dependent, hence 8 copies over distinct element types
// (copies over one element type all pass or all fail together).
import nelumbo.collections

T1 :: Object
T2 :: Object
T3 :: Object
T4 :: Object
T5 :: Object
T6 :: Object
T7 :: Object
T8 :: Object

T1 ::= K1
T2 ::= K2
T3 ::= K3
T4 ::= K4
T5 ::= K5
T6 ::= K6
T7 ::= K7
T8 ::= K8

Set<T1> ::= f1(<Set<T1>>,<Set<T1>>)
Set<T2> ::= f2(<Set<T2>>,<Set<T2>>)
Set<T3> ::= f3(<Set<T3>>,<Set<T3>>)
Set<T4> ::= f4(<Set<T4>>,<Set<T4>>)
Set<T5> ::= f5(<Set<T5>>,<Set<T5>>)
Set<T6> ::= f6(<Set<T6>>,<Set<T6>>)
Set<T7> ::= f7(<Set<T7>>,<Set<T7>>)
Set<T8> ::= f8(<Set<T8>>,<Set<T8>>)

Set<T1> a1, b1, s1
Set<T2> a2, b2, s2
Set<T3> a3, b3, s3
Set<T4> a4, b4, s4
Set<T5> a5, b5, s5
Set<T6> a6, b6, s6
Set<T7> a7, b7, s7
Set<T8> a8, b8, s8

f1(a1,b1)=s1 <=> s1=a1
f2(a2,b2)=s2 <=> s2=a2
f3(a3,b3)=s3 <=> s3=a3
f4(a4,b4)=s4 <=> s4=a4
f5(a5,b5)=s5 <=> s5=a5
f6(a6,b6)=s6 <=> s6=a6
f7(a7,b7)=s7 <=> s7=a7
f8(a8,b8)=s8 <=> s8=a8

{}={} ? [()][]
E[a1](a1={K1}-{K1} & f1(a1,a1||a1)=s1) ? [(s1={})][..]
E[a2](a2={K2}-{K2} & f2(a2,a2||a2)=s2) ? [(s2={})][..]
E[a3](a3={K3}-{K3} & f3(a3,a3||a3)=s3) ? [(s3={})][..]
E[a4](a4={K4}-{K4} & f4(a4,a4||a4)=s4) ? [(s4={})][..]
E[a5](a5={K5}-{K5} & f5(a5,a5||a5)=s5) ? [(s5={})][..]
E[a6](a6={K6}-{K6} & f6(a6,a6||a6)=s6) ? [(s6={})][..]
E[a7](a7={K7}-{K7} & f7(a7,a7||a7)=s7) ? [(s7={})][..]
E[a8](a8={K8}-{K8} & f8(a8,a8||a8)=s8) ? [(s8={})][..]
