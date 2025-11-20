    
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

	<Test> :: <Node>
	<Test> ::= T1, T2

    T1=T1           ? [()][]
    T1=T2           ? [][()]
    T1!=T1          ? [][()]
    T1!=T2          ? [()][]

    <Test> a, b

    a=b             ? [..][..]
    !(a=b)          ? [..][..]
    a!=b            ? [..][..]
    a=T1            ? [(a=T1)][..]
    T1=a            ? [(a=T1)][..]
    !(a=T1)         ? [..][(a=T1)]
    !(T1=a)         ? [..][(a=T1)]
    a!=T1           ? [..][(a=T1)]
    T1!=a           ? [..][(a=T1)]   
   
	a=T1|a=T2       ? [(a=T1),(a=T2)][..]
	a=T1&a=T2       ? [][(a=T1),(a=T2),..]

	!(a!=T1&a!=T2)  ? [(a=T1),(a=T2)][..]
	!(a!=T1|a!=T2)  ? [][(a=T1),(a=T2),..]
	
	E[a](a=T1|a=T2) ? [()][]
    A[a](a=T1&a=T2) ? [][()]
    
    !A[a](a!=T1&a!=T2) ? [()][]
    !E[a](a!=T1|a!=T2) ? [][()]
