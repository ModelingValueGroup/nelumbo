import nelumbo.strings

{
  hidden Integer a
  >10 & <20 & =15   ?   [(a=15),..][(a=10),(a=20),..]
}

{
  Integer ::= <hidden Integer> && <Integer> #35
  Integer ::= <visible Integer> & <Integer> #35

  hidden Integer x
  Integer y,z

  x&y=z <=> x*100+y=z
  &&y=z <=> x&y=z

  &&11=2211 ? [(x=22)][..]
  x&11=2211 ? [(x=22)][..]

}