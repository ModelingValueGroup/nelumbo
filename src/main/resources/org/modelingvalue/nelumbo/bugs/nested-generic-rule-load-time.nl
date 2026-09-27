// Bug: LOAD time of rules over deeply nested generic types explodes. This
// file gives the CORRECT result, but merely DECLARING the three rules below
// (no query needed) costs ~80s on the CLI; every other repro loads in ~2s.
// The time is spent in the type hierarchy computation, not in inference:
// jstack samples during the load all sit in
//   lang/Type.initSupers (Type.java:358-388, isAssignableFrom in the
//   many()-loop at :364) <- Type$TypeInfo.supers <- Type.initAllSupersList
//   (Type.java:390-415) <- allSupersMap <- Type.isAssignableFrom (:595)
// i.e. supers()/allSupersList() recomputed per generic instantiation with no
// sharing, exponential in the nesting depth of the argument types.
// Measured 2026-09-25 (cliJar at f52a88cf; IDENTICAL on a cliJar built at
// f7638724, so not caused by 8f116edd's Type.java change):
//   rowsBefore alone over List<List<Integer>>       2s
//   rowsBefore alone over List<List<Set<Integer>>> 12s  (List<List<List<Integer>>> also 12s)
//   rowsBefore alone over List<Set<Integer>>        1s  (depth 2 is fine)
//   rowsBefore + rowsFrom                          13s
//   rowsBefore + rowsFrom + put                    82s  (this file)
// The non-recursive variant of rowsBefore loads in 2s, so recursion of the
// rule over the depth-3 type is part of the trigger. This file has NO
// @KnownBug twin in KnownBugsTest (a slowness can only be asserted with the
// 60s preemptive timeout, which would add a minute to every test run);
// ./run-all-tests-with-CLI reports a repro that takes >= 60s as FAIL.
// Correct: the query below in ~2s. Actual: correct result after ~80s.
// Found 2026-09-25 in examples/sudoku-4x4-csp.nl (Norvig CSP solver): the
// put/rowsBefore/rowsFrom rules there are this shape over List<List<Set<Digit>>>.
import nelumbo.collections

Integer                  r, c
Set<Integer>             ns
List<Set<Integer>>       rw, rw2
List<List<Set<Integer>>> g, s, pre, suf

List<Set<Integer>>       ::= crow(<List<List<Set<Integer>>>>,<Integer>),
                             putRow(<List<Set<Integer>>>,<Integer>,<Set<Integer>>)
List<List<Set<Integer>>> ::= rowsBefore(<List<List<Set<Integer>>>>,<Integer>),
                             rowsFrom(<List<List<Set<Integer>>>>,<Integer>),
                             put(<List<List<Set<Integer>>>>,<Integer>,<Integer>,<Set<Integer>>)

crow(g,r)=rw        <=> rw pos g = r if r>=0 & r<4
putRow(rw,c,ns)=rw2 <=> rw2=[ns]
rowsBefore(g,r)=s   <=> s=[] if r=0, E[rw,pre](crow(g,r-1)=rw & rowsBefore(g,r-1)=pre & s=pre+[rw]) if r>0
rowsFrom(g,r)=s     <=> s=[] if r=4, E[rw,suf](crow(g,r)=rw & rowsFrom(g,r+1)=suf & s=[rw]+suf)     if r<4
put(g,r,c,ns)=s     <=> E[pre,suf](rowsBefore(g,r)=pre & rowsFrom(g,r+1)=suf &
                                   E[rw,rw2](crow(g,r)=rw & putRow(rw,c,ns)=rw2 & s=pre+[rw2]+suf))

put([[{1}],[{2}],[{3}],[{4}]],1,0,{9})=s ? [(s=[[{1}],[{9}],[{3}],[{4}]])][..]
