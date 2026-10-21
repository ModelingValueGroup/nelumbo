import nelumbo.strings

{
    hidden Integer a
    >10 & <20 & =15 ? [(a=15)][(a=10),(a=20),..]
}

{
    Integer ::= <hidden Integer> && <Integer> #35
    Integer ::= <visible Integer> & <Integer> #35

    hidden Integer x
    Integer y

    x&y ==  x*100+y
    &&y ==  x&y

    &&11=2211 ? [(x=22)][..]
    x&11=2211 ? [(x=22)][..]

}
