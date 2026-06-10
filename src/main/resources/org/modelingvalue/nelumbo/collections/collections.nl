
  import    nelumbo.strings
  
  Type E
  
  Collection<E>  :: Object
  Set<E>         :: Collection<E>
  List<E>        :: Collection<E>
  
  private Boolean ::= build(<E>, <Boolean#0>, <Set<E>>)                      @nelumbo.collections.BuildSet,
                      size(<Collection<E>>, <Integer>)                       @nelumbo.collections.CollectionSize,
                      indexOf(<List<E>>, <E>, <Integer>)                     @nelumbo.collections.IndexOf,
                      elementOf(<Set<E>>, <E>)                               @nelumbo.collections.ElementOf,
                      binarySet(<String>, <Set<E>>, <Set<E>>)                @nelumbo.collections.BinarySet,
                      ternarySet(<String>, <Set<E>>, <Set<E>>, <Set<E>>)     @nelumbo.collections.TernarySet,
                      ternaryList(<String>, <List<E>>, <List<E>>, <List<E>>) @nelumbo.collections.TernaryList
                      
  Boolean ::= <Set<E>> "<"  <Set<E>> #30,
              <Set<E>> ">"  <Set<E>> #30,
              <Set<E>> "<=" <Set<E>> #30,
              <Set<E>> ">=" <Set<E>> #30,
              <E>      "in" <Collection<E>> #30

  Set<E>  ::= { <(> <E> <,> , <)*> }       @nelumbo.collections.NSet,
              { [ <E> ] ( <Boolean#0> ) }  @nelumbo.collections.SetBuilder,
              <Set<E>> & <Set<E>>    #60,
              <Set<E>> | <Set<E>>    #60,
              <Set<E>> - <Set<E>>    #50
              
  Integer ::= | <Collection<E>> |    #35,
              <E> "pos" <List<E>>    #40
  
  List<E> ::= [ <(> <E> <,> , <)*> ]       @nelumbo.collections.NList,
              <List<E>> + <List<E>>  #50
  
  E e
  Boolean b
  Integer i
  Collection<E> c
  Set<E> s, s1, s2, s3
  List<E> l, l1, l2, l3
  
  |c|=i        <=>  size(c,i)
  
  {[e](b)}=s   <=>  build(e,b,s)
  e in s       <=>  elementOf(s,e)
  s1 < s2      <=>  binarySet("<", s1, s2)
  s1 > s2      <=>  binarySet("<", s2, s1)
  s1 <= s2     <=>  s1 < s2 | s1 = s2
  s1 >= s2     <=>  s1 > s2 | s1 = s2
  s1 & s2 = s3 <=>  ternarySet("&", s1, s2, s3)
  s1 | s2 = s3 <=>  ternarySet("|", s1, s2, s3)
  s1 - s2 = s3 <=>  ternarySet("-", s1, s2, s3)
  
  e pos l = i  <=>  indexOf(l, e, i)
  l1 + l2 = l3 <=>  ternaryList("+", l1, l2, l3)
  e in l       <=>  E[i](e pos l = i)
 