import    nelumbo.integers

Type E, F

Collection<E>    :: Object
Set<E>           :: Collection<E>
List<E>          :: Collection<E>

private Boolean  ::= build(<Lambda1<E,Boolean>>, <Set<E>>)                    @nelumbo.collections.BuildSet,
                     size(<Collection<E>>, <Integer>)                         @nelumbo.collections.Collections,
                     indexOf(<List<E>>, <E>, <Integer>)                       @nelumbo.collections.Collections,
                     elementOf(<Set<E>>, <E>)                                 @nelumbo.collections.Collections,
                     subset(<Set<E>>, <Set<E>>)                               @nelumbo.collections.Collections,
                     intersection(<Set<E>>, <Set<E>>, <Set<E>>)               @nelumbo.collections.Collections,
                     union(<Set<E>>, <Set<E>>, <Set<E>>)                      @nelumbo.collections.Collections,
                     diff(<Set<E>>, <Set<E>>, <Set<E>>)                       @nelumbo.collections.Collections,
                     concat(<List<E>>, <List<E>>, <List<E>>)                  @nelumbo.collections.Collections,
                     setFilter(<Set<E>>, <Lambda1<E,Boolean>>, <Set<E>>)      @nelumbo.collections.Collections,
                     listFilter(<List<E>>, <Lambda1<E,Boolean>>, <List<E>>)   @nelumbo.collections.Collections,
                     map(<Collection<F>>, <Lambda1<F,E>>, <List<E>>)          @nelumbo.collections.Collections,
                     sort(<Collection<E>>, <Lambda2<E,E,Boolean>>, <List<E>>) @nelumbo.collections.Collections

Boolean          ::= <Set<E>> "<"  <Set<E>>        #30,
                     <Set<E>> ">"  <Set<E>>        #30,
                     <Set<E>> "<=" <Set<E>>        #30,
                     <Set<E>> ">=" <Set<E>>        #30,
                     <E>      "in" <Collection<E>> #30

{Struct,Set<E>}  ::= { <(> <E> <,> , <)*> }  @nelumbo.collections.NSet
Set<E>           ::= { <Lambda1<E,Boolean>> },
                     <Set<E>> where <Lambda1<E,Boolean>> #37,
                     <Set<E>> && <Set<E>>                #60,
                     <Set<E>> || <Set<E>>                #60,
                     <Set<E>> - <Set<E>>                 #50

Integer          ::= | <Set<E>> |        #35,
                     | <List<E>> |       #35,
                     <E> "pos" <List<E>> #40

{Struct,List<E>} ::= [ <(> <E> <,> , <)*> ]  @nelumbo.collections.NList
List<E>          ::= <List<E>> + <List<E>>                 #50,
                     <List<E>> where <Lambda1<E,Boolean>>  #37,
                     <Set<F>> map <Lambda1<F,E>>           #37,
                     <Set<E>> sort <Lambda2<E,E,Boolean>>  #37,
                     <List<F>> map <Lambda1<F,E>>          #37,
                     <List<E>> sort <Lambda2<E,E,Boolean>> #37

Integer              i
E                    e
F                    f
Set<E>               s, s1, s2, s3
List<E>              l, l1, l2, l3
List<F>              lf
Lambda1<E,Boolean>   leb
Lambda2<E,E,Boolean> leeb
Lambda1<E,F>         lef

|s|=i             <=>  size(s,i)
|l|=i             <=>  size(l,i)

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

s sort leeb = l   <=>  sort(s, leeb, l)
s map lef = lf    <=>  map(s, lef, lf)

l sort leeb = l1  <=>  sort(l, leeb, l1)
l map lef = lf    <=>  map(l, lef, lf)
