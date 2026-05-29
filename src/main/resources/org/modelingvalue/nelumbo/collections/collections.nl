
  import    nelumbo.integers
  
  Type E
  
  Collection<E>  :: Object
  Set<E>         :: Collection<E>
  List<E>        :: Collection<E>
  
  private Boolean ::= build(<E>, <Boolean#0>, <Set<E>>) @nelumbo.collections.BuildSet

  Set<E>  ::= { <(> <E> <,> , <)*> }       @nelumbo.collections.NSet,
              { [ <E> ] ( <Boolean#0> ) }  @nelumbo.collections.SetBuilder
  List<E> ::= [ <(> <E> <,> , <)*> ]       @nelumbo.collections.NList 

  E e
  Boolean c
  Set<E> s
  
  {[e](c)}=s <=> build(e,c,s)
 