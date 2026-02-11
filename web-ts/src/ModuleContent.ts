/**
 * ModuleContent - embedded .nl library files for the import mechanism.
 * Content matches Java resources exactly.
 */

const LOGIC = `

  private Boolean ::= eq(<Object>,<Object>)                   @org.modelingvalue.nelumbo.logic.Equal

  Boolean ::= true                                            @org.modelingvalue.nelumbo.logic.NBoolean,
              false                                           @org.modelingvalue.nelumbo.logic.NBoolean,
              unknown                                         @org.modelingvalue.nelumbo.logic.NBoolean,
              ! <Boolean>                             #25     @org.modelingvalue.nelumbo.logic.Not,
              <Boolean> & <Boolean>                   #22     @org.modelingvalue.nelumbo.logic.And,
              <Boolean> | <Boolean>                   #20     @org.modelingvalue.nelumbo.logic.Or,
              <Boolean> -> <Boolean>                  #18,
              <Boolean> "<->" <Boolean>               #16,
              E[<(> <Variable#100> <,> , <)+>](<Boolean#0>)   @org.modelingvalue.nelumbo.logic.ExistentialQuantifier,
              A[<(> <Variable#100> <,> , <)+>](<Boolean#0>)   @org.modelingvalue.nelumbo.logic.UniversalQuantifier,
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
`;

const INTEGERS = `
  import    nelumbo.logic

  Integer :: Object

  private Boolean ::= add(<Integer>,<Integer>,<Integer>)   @org.modelingvalue.nelumbo.integers.Add,
                      mult(<Integer>,<Integer>,<Integer>)  @org.modelingvalue.nelumbo.integers.Multiply

  Boolean ::= <Integer>  >   <Integer>   #30  @org.modelingvalue.nelumbo.integers.GreaterThan,
              <Integer> "<"  <Integer>   #30,
              <Integer> "<=" <Integer>   #30,
              <Integer>  >=  <Integer>   #30

  Integer ::= <NUMBER>                @org.modelingvalue.nelumbo.integers.NInteger,
              <Integer> - <Integer>   #40,
              <Integer> + <Integer>   #40,
                        - <Integer>   #80,
              <Integer> * <Integer>   #50,
              <Integer> / <Integer>   #50,
                        | <Integer> | #35

  Integer a, b, c

  a<b    <=>  b>a
  a<=b   <=>  a<b | a=b
  a>=b   <=>  a>b | a=b

  a+b=c  <=>  add(a,b,c)
  a-b=c  <=>  add(c,b,a)
  a*b=c  <=>  mult(a,b,c)
  a/b=c  <=>  mult(c,b,a)

  -a=b   <=>  0-a=b

  |a|=b  <=>  b=a   if a>=0,
              b=-a  if a<0
`;

const STRINGS = `
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
`;

const COLLECTIONS = `
  import    nelumbo.integers

  Type E

  Set<E>  ::= { <(> <E> <,> , <)*> }  @org.modelingvalue.nelumbo.collections.NSet
  List<E> ::= [ <(> <E> <,> , <)*> ]  @org.modelingvalue.nelumbo.collections.NList
`;

const MODULES: Record<string, string> = {
  'nelumbo.logic': LOGIC,
  'nelumbo.integers': INTEGERS,
  'nelumbo.strings': STRINGS,
  'nelumbo.collections': COLLECTIONS,
};

let _externalResolver: ((name: string) => string | null) | null = null;

export function setExternalResolver(resolver: ((name: string) => string | null) | null): void {
  _externalResolver = resolver;
}

export function resolveModuleContent(name: string): string | null {
  const builtin = MODULES[name];
  if (builtin !== undefined) return builtin;
  if (_externalResolver !== null) return _externalResolver(name);
  return null;
}
