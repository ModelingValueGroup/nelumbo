import    nelumbo.integers

Type E, F, T

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
                    listFilter(<List<E>>, <Lambda1<E,Boolean>>, <List<E>>) @nelumbo.collections.Collections,
                    map(<Collection<F>>, <Lambda1<F,T>>, <List<T>>)        @nelumbo.collections.Collections

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
                    <List<E>> where <Lambda1<E,Boolean>> #37,
                    <Collection<F>> map <Lambda1<F,T>>   #37

Integer            i
E                  e
Collection<E>      c
Set<E>             s, s1, s2, s3
List<E>            l, l1, l2, l3
Lambda1<E,Boolean> leb

|c|=i             <=>  size(c,i)

{leb}=s           <=>  build(leb, s)
e in s            <=>  elementOf(s, e)
s1 < s2           <=>  subset(s1, s2)
s1 > s2           <=>  subset(s2, s1)
s1 && s2 = s3     <=>  intersection(s1, s2, s3)
s1 || s2 = s3     <=>  union(s1, s2, s3)
s1 - s2 = s3      <=>  diff(s1, s2, s3)

e pos l = i       <=>  indexOf(l, e, i)
l1 + l2 = l3      <=>  concat(l1, l2, l3)

s1 <= s2          <=>  s1 < s2 | s1 = s2
s1 >= s2          <=>  s1 > s2 | s1 = s2
e in l            <=>  E[i](e pos l = i)

s1 where leb = s2 <=>  setFilter(s1, leb, s2)
l1 where leb = l2 <=>  listFilter(l1, leb, l2)


F             f
T             t
Collection<F> cf
List<T>       lt
Lambda1<F,T>  lft

cf map lft = lt <=>  map(cf, lft, lt)
