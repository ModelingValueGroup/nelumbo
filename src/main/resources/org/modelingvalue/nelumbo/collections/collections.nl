import    nelumbo.integers

Type E

Collection<E>   :: Object
Set<E>          :: Collection<E>
List<E>         :: Collection<E>

private Boolean ::= build(<Lambda1<E,Boolean>>, <Set<E>>)                  @nelumbo.collections.BuildSet,
                    size(<Collection<E>>, <Integer>)                       @nelumbo.collections.Collections,
                    indexOf(<List<E>>, <E>, <Integer>)                     @nelumbo.collections.Collections,
                    elementOf(<Set<E>>, <E>)                               @nelumbo.collections.Collections,
                    subset(<Set<E>>, <Set<E>>)                             @nelumbo.collections.Collections,
                    intersection(<Set<E>>, <Set<E>>, <Set<E>>)             @nelumbo.collections.Collections,
                    union(<Set<E>>, <Set<E>>, <Set<E>>)                    @nelumbo.collections.Collections,
                    diff(<Set<E>>, <Set<E>>, <Set<E>>)                     @nelumbo.collections.Collections,
                    concat(<List<E>>, <List<E>>, <List<E>>)                @nelumbo.collections.Collections,
                    setFilter(<Set<E>>, <Lambda1<E,Boolean>>, <Set<E>>)    @nelumbo.collections.Collections,
                    listFilter(<List<E>>, <Lambda1<E,Boolean>>, <List<E>>) @nelumbo.collections.Collections

Boolean         ::= <Set<E>> "<"  <Set<E>>        #30,
                    <Set<E>> ">"  <Set<E>>        #30,
                    <Set<E>> "<=" <Set<E>>        #30,
                    <Set<E>> ">=" <Set<E>>        #30,
                    <E>      "in" <Collection<E>> #30

Set<E>          ::= { <(> <E> <,> , <)*> }   @nelumbo.collections.NSet,
                    { <Lambda1<E,Boolean>> },
                    <Set<E>> where <Lambda1<E,Boolean>> #37,
                    <Set<E>> && <Set<E>>                #60,
                    <Set<E>> || <Set<E>>                #60,
                    <Set<E>> - <Set<E>>                 #50

Integer         ::= | <Collection<E>> | #35,
                    <E> "pos" <List<E>> #40

List<E>         ::= [ <(> <E> <,> , <)*> ]   @nelumbo.collections.NList,
                    <List<E>> + <List<E>>                #50,
                    <List<E>> where <Lambda1<E,Boolean>> #37

E                  e
Lambda1<E,Boolean> la
Boolean            b
Integer            i
Collection<E>      c
Set<E>             s, s1, s2, s3
List<E>            l, l1, l2, l3

|c|=i          <=>  size(c,i)

{la}=s         <=>  build(la, s)
e in s         <=>  elementOf(s, e)
s1 < s2        <=>  subset(s1, s2)
s1 > s2        <=>  subset(s2, s1)
s1 && s2 = s3  <=>  intersection(s1, s2, s3)
s1 || s2 = s3  <=>  union(s1, s2, s3)
s1 - s2 = s3   <=>  diff(s1, s2, s3)

e pos l = i    <=>  indexOf(l, e, i)
l1 + l2 = l3   <=>  concat(l1, l2, l3)

s1 <= s2       <=>  s1 < s2 | s1 = s2
s1 >= s2       <=>  s1 > s2 | s1 = s2
e in l         <=>  E[i](e pos l = i)

s1 where la=s2 <=>  setFilter(s1, la, s2)
l1 where la=l2 <=>  listFilter(l1, la, l2)
