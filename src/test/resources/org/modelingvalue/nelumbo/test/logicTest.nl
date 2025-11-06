
	<Test> :: <Node>
	<Test> ::= T1, T2, T3
    <Test> a, b, c

    a=b             ? [..][..]
    a=T1            ? [(a=T1)][..]
    T1=a            ? [(a=T1)][..]
    T1=T1           ? [()][]
    T1=T2           ? [][()]
    T1!=T1          ? [][()]
    T1!=T2          ? [()][]

    true&true       ? [()][]
    true&false      ? [][()]
    false&true      ? [][()]
    false&false     ? [][()]
    
    true|true       ? [()][]
    true|false      ? [()][]
    false|true      ? [()][]
    false|false     ? [][()]
    
    !true           ? [][()]
    !false          ? [()][]

	a=T1|a=T2       ? [(a=T1),(a=T2)][..]
	a=T1&a=T2       ? [][(a=T1),(a=T2),..]
	
	E[a](a=T1|a=T2) ? [()][..]
    A[a](a=T1&a=T2) ? [][(),..]

