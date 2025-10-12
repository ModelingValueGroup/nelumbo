
    <Relation> ::= fib(<Integer>,<Integer>)
    <Integer>  ::= fib(<Integer>)

    <Integer> a, b

    fib(0,0)
    fib(1,1)

    fib(a)=b  <==> fib(a,b)
    fib(a,b)  <==> (a>1 & b=fib(a-1)+fib(a-2)) | (a<=1 & fib(a,b))
    
    fib(0)=a        ? [fib(0)=0][..]
    fib(1)=a        ? [fib(1)=1][..]
    fib(2)=a        ? [fib(2)=1][..]
    fib(3)=a        ? [fib(3)=2][..]
    fib(5)=a        ? [fib(5)=5][..]
    fib(10)=a       ? [fib(10)=55][..]
    fib(100)=a      ? [fib(100)=36#22r8fozas3n8w3][..]
    fib(1000)=a     ? [fib(1000)=36#18nrvsuayughau0blk8aylvbyaqwiaqba77rdsgscn5hzwgbgaws8i8svp4xdmoo82plxiyogd5iaj1cspez8zfeio92a76t9n1frssxklr92wyyxm8r903o1ofgncikuggcwnf][..]
