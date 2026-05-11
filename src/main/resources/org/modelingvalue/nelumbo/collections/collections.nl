
  import    nelumbo.integers
  
  Type E
  
  Collection<E>  :: Object
  Set<E>         :: Collection<E>
  List<E>        :: Collection<E>  

  Set<E>  ::= { <(> <E> <,> , <)*> }  @nelumbo.collections.NSet 
  List<E> ::= [ <(> <E> <,> , <)*> ]  @nelumbo.collections.NList 
  