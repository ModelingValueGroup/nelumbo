
    <String> a, b, c
    <Integer> d
    
    "a"="a"                   ? [()][]
    "a"!="a"                  ? [][()]

    "a"="b"                   ? [][()]
    "a"!="b"                  ? [()][]

    "foo"+"bar"="baz"         ? [][()]
    "foo"+"bar"="foobar"      ? [()][]
     a+"bar"="foobar"         ? [(a="foo")][..]
    "foo"+a="foobar"          ? [(a="bar")][..]
    "foo"+"bar"=a             ? [(a="foobar")][..]

    string_length("foo",0)            ? [][()]
    string_length("foo",3)            ? [()][]
    string_length(a,3)                ? [..][..]
    string_length("foo",d)            ? [(d=3)][..]

    integer_string(123456,"123456")   ? [()][]
    integer_string(123456,a)          ? [(a="123456")][..]
    integer_string(0000123456,a)      ? [(a="123456")][..]
    integer_string(d,"123456")        ? [(d=123456)][..]
    integer_string(d,"0000123456")    ? [(d=123456)][..]
    integer_string(d,"    123456")    ? [][..]
    integer_string(d,"123456    ")    ? [][..]
    integer_string(d,"NaN")           ? [][..]
    integer_string(d,"Hello, World!") ? [][..]
