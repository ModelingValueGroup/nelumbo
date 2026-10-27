import    nelumbo.integers

Type E, F

Collection<E>    :: Object
Set<E>           :: Collection<E>
List<E>          :: Collection<E>

private Boolean  ::= build(<Lambda1<E,Boolean>>, <Set<E>>)                    @nelumbo.collections.BuildSet,
                     size(<Collection<E>>, <Integer>)                         @nelumbo.collections.Collections,
                     index(<List<E>>, <E>, <Integer>)                         @nelumbo.collections.Collections,
                     firstIndex(<List<E>>, <E>, <Integer>)                    @nelumbo.collections.Collections,
                     lastIndex(<List<E>>, <E>, <Integer>)                     @nelumbo.collections.Collections,
                     first(<List<E>>, <E>)                                    @nelumbo.collections.Collections,
                     last(<List<E>>, <E>)                                     @nelumbo.collections.Collections,
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

{Struct,Set<E>}  ::= { <(> <(> <E> <|> .. <)> <,> , <)*> }  @nelumbo.collections.NSet
Set<E>           ::= { <Lambda1<E,Boolean>> },
                     <Set<E>> where <Lambda1<E,Boolean>> #37,
                     <Set<E>> && <Set<E>>                #60,
                     <Set<E>> || <Set<E>>                #60,
                     <Set<E>> - <Set<E>>                 #50

Integer          ::= | <Collection<E>> |           #35,
                     <E> "first" "index" <List<E>> #40,
                     <E> "last" "index" <List<E>>  #40,
                     <E> "index" <List<E>>         #40

Set<Integer>     ::= <E> "indexes" <List<E>> #40

{Struct,List<E>} ::= [ <(> <(> <E> <|> .. <)> <,> , <)*> ]  @nelumbo.collections.NList
List<E>          ::= <List<E>> + <List<E>>                       #50,
                     <List<E>> where <Lambda1<E,Boolean>>        #37,
                     <Collection<F>> map <Lambda1<F,E>>          #37,
                     <Collection<E>> sort <Lambda2<E,E,Boolean>> #37

E                ::= "first" "in"<List<E>>         #40,
                     "last" "in" <List<E>>         #40,
                     "at" <Integer> "in" <List<E>> #40

Integer              i
Set<Integer>         si
E                    e
F                    f
Collection<E>        c
Set<E>               s, s1, s2, s3
List<E>              l, l1, l2, l3
List<F>              lf
Collection<F>        cf
Lambda1<E,Boolean>   leb
Lambda2<E,E,Boolean> leeb
Lambda1<F,E>         lfe

|c|=i               <=>  size(c,i)

{leb}=s             <=>  build(leb, s)
e in s              <=>  elementOf(s, e)
s1 < s2             <=>  subset(s1, s2)
s1 > s2             <=>  subset(s2, s1)
s1 && s2 = s3       <=>  intersection(s1, s2, s3)
s1 || s2 = s3       <=>  union(s1, s2, s3)
s1 - s2 = s3        <=>  diff(s1, s2, s3)

at i in l = e       <=>  index(l,e,i)
first in l = e      <=>  first(l,e)
last in l = e       <=>  last(l,e)
e first index l = i <=>  firstIndex(l, e, i)
e last index l = i  <=>  lastIndex(l, e, i)
l1 + l2 = l3        <=>  concat(l1, l2, l3)

e indexes l         ==  {[i](index(l, e, i))}
e index l           ==  e first index l

s1 <= s2            <=>  s1 < s2 | s1 = s2
s1 >= s2            <=>  s1 > s2 | s1 = s2
e in l              <=>  E[i](firstIndex(l, e, i))

s1 where leb = s2   <=>  setFilter(s1, leb, s2)
l1 where leb = l2   <=>  listFilter(l1, leb, l2)

cf map lfe = l      <=>  map(cf, lfe, l)
c sort leeb = l     <=>  sort(c, leeb, l)
