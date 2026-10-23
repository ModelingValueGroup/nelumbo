import nelumbo.strings

{
    hidden Integer a
    >10 & <20 & =15 ? [(a=15)][(a=10),(a=20),..]
}

{
    Integer ::= <hidden Integer> $$ <Integer> #35
    Integer ::= <visible Integer> $ <Integer> #35

    hidden Integer x
    Integer y

    x$y ==  *100+y
    $$y ==  x$y

    $$11=2211 ? [(x=22)][..]
    x$11=2211 ? [(x=22)][..]
}

{
    Test   :: Object

    String ::= <Test>.name       #35,
               <hidden Test>name #35

    Test   ::= T1, T2

    fact T1.name = "T1", T2.name = "T2"

    hidden Test t

    name ==  t.name

    name="T1" ? [(t=T1),..][..]
}
