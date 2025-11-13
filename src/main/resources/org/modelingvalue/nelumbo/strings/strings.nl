
    <String>    :: <Node>

    <String>    ::= <STRING>                                        @org.modelingvalue.nelumbo.strings.String

    <Predicate> ::= string_concat(<String>,<String>,<String>)       @org.modelingvalue.nelumbo.strings.Concat,
                    string_length(<String>,<Integer>)               @org.modelingvalue.nelumbo.strings.Length,
                    integer_string(<Integer>,<String>)              @org.modelingvalue.nelumbo.strings.Integer

    <String>    ::=  <String> + <String>  #40

    <String>    a, b, c

    a+b=c       <=> string_concat(a,b,c)
    a.length=b  <=> string_length(a,b)
    (int)a      <=> integer_string(a)
