
    <String> a, b, c
    <Integer> d

    ? "a"="a"                   ["a"="a"][]
    ? "a"!="a"                  []["a"!="a"]

	? "a"="b"                   []["a"="b"]
	? "a"!="b"                  ["a"!="b"][]

    ? "foo"+"bar"="baz"         []["foo"+"bar"="baz"]
    ? "foo"+"bar"="foobar"      ["foo"+"bar"="foobar"][]
    ? a+"bar"="foobar"          ["foo"+"bar"="foobar"][..]
    ? "foo"+a="foobar"          ["foo"+"bar"="foobar"][..]
    ? "foo"+"bar"=a             ["foo"+"bar"="foobar"][..]

    ? string_length("foo",0)             [][string_length("foo",0)]
    ? string_length("foo",3)             [string_length("foo",3)][]
    ? string_length(a,3)                 [..][..]
    ? string_length("foo",d)             [string_length("foo",3)][..]
