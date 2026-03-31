
  import    nelumbo.integers
  
  Type E
  
  private Boolean ::= build(<Variable>,<Boolean>,<Set<E>>)  @org.modelingvalue.nelumbo.collections.SetBuilder 
  
  Set<E>  ::= { <(> <E> <,> , <)*> }          @org.modelingvalue.nelumbo.collections.NSet,
              {[<Variable#100>] <Boolean#0>}
  List<E> ::= [ <(> <E> <,> , <)*> ]          @org.modelingvalue.nelumbo.collections.NList
  
