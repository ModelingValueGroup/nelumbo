
    <String> a, b, c

    ? "a"="a"                   ["a"="a"][]
    ? "a"!="a"                  []["a"!="a"]

	? "a"="b"                   []["a"="b"]
	? "a"!="b"                  ["a"!="b"][]

    ? "foo"+"bar"="baz"         []["foo"+"bar"="baz"]
    ? "foo"+"bar"="foobar"      ["foo"+"bar"="foobar"][]
    ? a+"bar"="foobar"          ["foo"+"bar"="foobar"][..]
    ? "foo"+a="foobar"          ["foo"+"bar"="foobar"][..]
    ? "foo"+"bar"=a             ["foo"+"bar"="foobar"][..]
