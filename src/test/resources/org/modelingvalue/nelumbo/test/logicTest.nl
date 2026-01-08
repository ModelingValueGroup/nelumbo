    
    // Proposition Logic
    
    true            ? [()][]
    false           ? [][()]
    unknown         ? [..][..]
    
    true&true       ? [()][]
    true&false      ? [][()]
    false&true      ? [][()]
    false&false     ? [][()]
    
    true|true       ? [()][]
    true|false      ? [()][]
    false|true      ? [()][]
    false|false     ? [][()]
    
    unknown&true    ? [..][..]
    unknown&false   ? [][()]
    true&unknown    ? [..][..]
    false&unknown   ? [][()]
    
    unknown|true    ? [()][]
    unknown|false   ? [..][..]
    true|unknown    ? [()][]
    false|unknown   ? [..][..]
    
    !true           ? [][()]
    !false          ? [()][]
    !unknown        ? [..][..]
    
    
    <Predicate> p

    p               ? [(p=true)][(p=false)]
    !p              ? [(p=false)][(p=true)]
    
    p&true          ? [(p=true)][(p=false)]
    true&p          ? [(p=true)][(p=false)]
    p|false         ? [(p=true)][(p=false)]
    false|p         ? [(p=true)][(p=false)]
    
    p&false         ? [][()]
    false&p         ? [][()]
    p|true          ? [()][]
    true|p          ? [()][]
    
    p|unknown       ? [(p=true),..][..]
    unknown|p       ? [(p=true),..][..]
    p&unknown       ? [..][(p=false),..]
    unknown&p       ? [..][(p=false),..]

    // Identity

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
	
	// Predicate Logic
	
	E[a](a=T1|a=T2) ? [()][]
    A[a](a=T1&a=T2) ? [][()]
    
    !A[a](a!=T1&a!=T2) ? [()][]
    !E[a](a!=T1|a!=T2) ? [][()]
