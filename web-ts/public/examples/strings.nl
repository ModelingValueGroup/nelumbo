
  import    nelumbo.integers

  String  :: Object

  String  ::= <STRING>                                          @org.modelingvalue.nelumbo.strings.NString

  private Boolean ::= string_concat(<String>,<String>,<String>) @org.modelingvalue.nelumbo.strings.Concat,
                      string_length(<String>,<Integer>)         @org.modelingvalue.nelumbo.strings.Length,
                      integer_string(<Integer>,<String>)        @org.modelingvalue.nelumbo.strings.ToInteger

  String  ::=  <String> + <String>  #40,
               str(<Integer>)
               
  Integer ::=  len(<String>),
               int(<String>)


  String  a, b, c
  Integer x

  a+b=c     <=> string_concat(a,b,c)
  a+b=c     <=> string_concat(a,b,c)
  len(a)=x  <=> string_length(a,x)
  int(a)=x  <=> integer_string(x,a)
  str(x)=a  <=> integer_string(x,a)
