
  import    nelumbo.integers
  
  Type E
  
  Collection<E>  :: Object
  Set<E>         :: Collection<E>
  List<E>        :: Collection<E>  

  Set<E>  ::= { <(> <E> <,> , <)*> }  @org.modelingvalue.nelumbo.collections.NSet 
  List<E> ::= [ <(> <E> <,> , <)*> ]  @org.modelingvalue.nelumbo.collections.NList 
  