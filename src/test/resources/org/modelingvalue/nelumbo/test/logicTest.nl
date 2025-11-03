
	<Test> :: <Node>
	<Test> ::= T1, T2, T3
    <Test> a, b, c

    a=b            ? [..][..]
    a=T1           ? [T1=T1][..]
    T1=a           ? [T1=T1][..]
    T1=T1          ? [T1=T1][]
    T1=T2          ? [][T1=T2]
    T1!=T1         ? [][T1!=T1]
    T1!=T2         ? [T1!=T2][]

    true&true      ? [true&true][]
    true&false     ? [][true&false]
    false&true     ? [][false&true]
    false&false    ? [][false&false]
    
    true|true      ? [true|true][]
    true|false     ? [true|false][]
    false|true     ? [false|true][]
    false|false    ? [][false|false]
    
    !true          ? [][!true]
    !false         ? [!false][]

	a=T1 | a=T2      ? [T1=T1|T1=T2,T2=T1|T2=T2][..]
	a=T1 & a=T2      ? [][T1=T1&T1=T2,T2=T1&T2=T2,..]

