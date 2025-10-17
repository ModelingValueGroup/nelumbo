# Nelumbo

Nelumbo aims to be a powerful and extensible declarative logic programming language, designed for defining and executing custom syntax and semantics. As a meta-language, Nelumbo will be easily extensible, making it suitable for a wide range of applications. The goal is to integrate it with any IDE using the Language Server Protocol, allowing Nelumbo to serve as a language development platform. The language is currently developed in Java for seamless integration and performance. Please note that Nelumbo is in a very early stage of development, and incompatible changes are likely to occur.

---

## Table of Contents
- [Features](#features)
- [Examples](#examples)
- [FAQ](#faq)
- [Contributing](#contributing)
- [License](#license)
- [Support](#support)

---

## Features
* Define and parse syntaxes
* Define and execute semantics
* Purely declarative semantics
* Define and run tests
* Easily extensible
* Easily integrable
* Written in Java

## Examples

### Family Relations Example
```text
<Person>    :: <Node>
<Male>      :: <Person>
<Female>    :: <Person>

<Relation>  ::= pc(<Person>,<Person>)   // parent-child

<Person>    ::= p(<Person>),   // parent
                c(<Person>),   // child
                a(<Person>),   // ancestor
                d(<Person>),   // descendant
                m(<Person>),   // mother
                f(<Person>)    // father

<Person> a, b, c
<Male>   y
<Female> x
           
c(a)=b  <==>  pc(a,b)
p(a)=b  <==>  pc(b,a)
m(a)=b  <==>  c(x)=a & b=x
f(a)=b  <==>  c(y)=a & b=y

a(a)=b  <==>  d(b)=a
d(a)=c  <==>  c(a)=c |
              d(a)=b & c(b)=c

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

a(Amalia)=a         ? [a(Amalia)=Beatrix,a(Amalia)=Maxima,a(Amalia)=Hendrik,a(Amalia)=Bernhard,a(Amalia)=Juliana,a(Amalia)=Claus,a(Amalia)=Willem,a(Amalia)=Wilhelmina][..]

m(Amalia)=Maxima    ? [m(Amalia)=Maxima][]
m(Amalia)=Willem    ? [][m(Amalia)=Willem]
m(Amalia)=a         ? [m(Amalia)=Maxima][..]
f(Amalia)=a         ? [f(Amalia)=Willem][..]
f(m(f(Amalia)))=a   ? [f(m(f(Amalia)))=Bernhard][..]
```

### Fibonacci Example
```text
<Integer> ::= fib(<Integer>)

<Integer> n, f

fib(n)=f  <==> n<=1 & f=n |
               n>1  & f=fib(n-1)+fib(n-2)

fib(0)=f       ? [fib(0)=0][..]
fib(1)=f       ? [fib(1)=1][..]
fib(2)=f       ? [fib(2)=1][..]
fib(3)=f       ? [fib(3)=2][..]
fib(5)=f       ? [fib(5)=5][..]
fib(10)=f      ? [fib(10)=55][..]
fib(100)=f     ? [fib(100)=36#22r8fozas3n8w3][..]
fib(1000)=f    ? [fib(1000)=36#18nrvsuayughau0blk8aylvbyaqwiaqba77rdsgscn5hzwgbgaws8i8svp4xdmoo82plxiyogd5iaj1cspez8zfeio92a76t9n1frssxklr92wyyxm8r903o1ofgncikuggcwnf][..]
```

## FAQ

### What is GLSP?
GLSP stands for Graphical Language Server Platform. It is an Eclipse Foundation technology for building web-based diagram editors and modeling tools using a client–server architecture, inspired by the Language Server Protocol (LSP). A GLSP Client (typically a web UI built with Sprotty) communicates with a GLSP Server over JSON-RPC to render diagrams, handle user interactions, and execute model operations. GLSP enables consistent, IDE-like diagramming experiences in the browser and can integrate with platforms such as VS Code, Theia, or Eclipse.

Useful links:
- Project page: https://www.eclipse.org/glsp/
- Docs: https://www.eclipse.org/glsp/documentation/
- GitHub: https://github.com/eclipse-glsp/glsp

## Contributing
Contributions and feedback are welcome! Please open issues or pull requests on GitHub.

## License
This project is licensed under the terms of the LICENSE file provided in the repository.

## Support
For questions or support, please use the GitHub Issues page.
