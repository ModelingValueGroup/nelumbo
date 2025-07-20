# Nelumbo

Nelumbo is a powerful and extensible declarative logic programming language designed for defining and executing custom syntax and semantics. As a meta-language, Nelumbo is easily extendable, making it suitable for a wide range of applications. The language is implemented in Java for seamless integration and performance.

---

## Table of Contents
- [Features](#features)
- [Examples](#examples)
- [Contributing](#contributing)
- [License](#license)
- [Support](#support)

---

## Features

## Examples

### Family Relations Example
```nl
<Person>    :: <Node>
<Male>      :: <Person>
<Female>    :: <Person>
<Relation>  ::= pc(<Person>,<Person>)   // parent-child
<Predicate> ::= ad(<Person>,<Person>)   // ancestor-descendant
<Person>    ::= p(<Person>),   // parent
                c(<Person>),   // child
                a(<Person>),   // ancestor
                d(<Person>)    // descendant
<Female>    ::= m(<Person>)    // mother
<Male>      ::= f(<Person>)    // father
<Person> a, b, c
<Male>   y
<Female> x
ad(a,c) <==   pc(a,c),
              ad(a,b) & pc(b, c)
c(a)=b  <==>  pc(a,b)
p(a)=b  <==>  pc(b,a)
d(a)=b  <==>  ad(a,b)
a(a)=b  <==>  ad(b,a)
m(a)=b  <==>  pc(x,a) & b=x
f(a)=b  <==>  pc(y,a) & b=y
<Male>   ::= Hendrik, Bernhard, Claus, Willem
<Female> ::= Wilhelmina, Juliana, Beatrix, Maxima, Amalia
pc(Hendrik, Juliana)
pc(Wilhelmina, Juliana)
pc(Juliana, Beatrix)
pc(Bernhard, Beatrix)
pc(Beatrix, Willem)
pc(Claus, Willem)
pc(Willem, Amalia)
pc(Maxima, Amalia)
? m(Amalia)=Maxima    [m(Amalia)=Maxima][]
? m(Amalia)=Willem    [][m(Amalia)=Willem]
? m(Amalia)=a         [m(Amalia)=Maxima][..]
? a(Amalia)=a         [a(Amalia)=Beatrix,a(Amalia)=Maxima,a(Amalia)=Hendrik,a(Amalia)=Bernhard,a(Amalia)=Juliana,a(Amalia)=Claus,a(Amalia)=Willem,a(Amalia)=Wilhelmina][..]
? f(m(f(Amalia)))=a   [f(m(f(Amalia)))=Bernhard][..]
```

### Fibonacci Example
```nl
<Relation> ::= fib(<Integer>,<Integer>)
<Integer>  ::= fib(<Integer>)
<Integer> a, b
fib(0,0)
fib(1,1)
fib(a)=b  <==> fib(a,b)
fib(a,b)  <==  a>1 & b=fib(a-1)+fib(a-2)
? fib(0)=a        [fib(0)=0][..]
? fib(2)=a        [fib(2)=1][..]
? fib(5)=a        [fib(5)=5][..]
? fib(10)=a       [fib(10)=55][..]
? fib(100)=a      [fib(100)=36#22r8fozas3n8w3][..]
? fib(1000)=a     [fib(1000)=36#18nrvsuayughau0blk8aylvbyaqwiaqba77rdsgscn5hzwgbgaws8i8svp4xdmoo82plxiyogd5iaj1cspez8zfeio92a76t9n1frssxklr92wyyxm8r903o1ofgncikuggcwnf][..]
```

## Contributing
Contributions and feedback are welcome! Please open issues or pull requests on GitHub.

## License
This project is licensed under the terms of the LICENSE file provided in the repository.

## Support
For questions or support, please use the GitHub Issues page.