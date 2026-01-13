
  import    nelumbo.integers

  <String>  :: <Object>

  <String>  ::= <STRING>                                        @org.modelingvalue.nelumbo.strings.NString

  <Boolean> ::= string_concat(<String>,<String>,<String>)       @org.modelingvalue.nelumbo.strings.Concat,
                string_length(<String>,<Integer>)               @org.modelingvalue.nelumbo.strings.Length,
                integer_string(<Integer>,<String>)              @org.modelingvalue.nelumbo.strings.ToInteger

  <String>  ::=  <String> + <String>  #40

  <String>  a, b, c

  a+b=c     <=> string_concat(a,b,c)
