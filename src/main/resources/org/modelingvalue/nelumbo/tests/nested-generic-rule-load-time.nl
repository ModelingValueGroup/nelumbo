// Regression test (fixed 2026-10-02; was bugs/nested-generic-rule-load-time.nl).
// Declaring recursive rules over a depth-3 generic type (List<List<Set<Integer>>>)
// must load fast. (Was: correct result, but ~80s of LOAD time in the type hierarchy
// computation, lang/Type.initSupers/initAllSupersList via isAssignableFrom, recomputed
// per generic instantiation. Now 1.4s on the CLI; most likely fixed by the super/sub
// cache in Type.getAssigned, 9b239d83, not bisected.) The @Timeout on its
// RegressionTest method is the assertion for the load time; the query below checks
// the result.
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
