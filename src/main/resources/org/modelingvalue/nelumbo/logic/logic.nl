
 
  private Boolean ::= eq(<Object>,<Object>)                   @org.modelingvalue.nelumbo.logic.Equal
 
  Boolean ::= true                                            @org.modelingvalue.nelumbo.logic.NBoolean,
              false                                           @org.modelingvalue.nelumbo.logic.NBoolean,
              unknown                                         @org.modelingvalue.nelumbo.logic.NBoolean,
              ! <Boolean>                             #25     @org.modelingvalue.nelumbo.logic.Not,
              <Boolean> & <Boolean>                   #22     @org.modelingvalue.nelumbo.logic.And,
              <Boolean> | <Boolean>                   #20     @org.modelingvalue.nelumbo.logic.Or,
              <Boolean> -> <Boolean>                  #18,
              <Boolean> "<->" <Boolean>               #16,
              E[<(> <Variable> <,> , <)+>](<Boolean>) #100 #0 @org.modelingvalue.nelumbo.logic.ExistentialQuantifier,
              A[<(> <Variable> <,> , <)+>](<Boolean>) #100 #0 @org.modelingvalue.nelumbo.logic.UniversalQuantifier,
              <Object> != <Object>                    #30

  Boolean p1, p2
    
  p1->p2  <=> !p1|p2
  p1<->p2 <=> (p1->p2)&(p2->p1)
     
  Literal  l1, l2
  Function f1, f2
  Object   n1, n2

  l1=l2  <=> eq(l1, l2)
  l1=f1  <=> f1=l1
  n1!=n2 <=> !(n1=n2)
