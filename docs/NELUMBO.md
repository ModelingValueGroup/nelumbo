# Nelumbo

<img src="nelumbo.svg" alt="Nelumbo" width="50" height="50" />

## Logic Meta Language

* Fast logic Reasoning
* Define and parse Syntaxes
* Define and execute Semantics
* Define and run Tests
* IDE integration using LSP
* Lightweight Language Workbench

---

## Requirements for Nelumbo

* Perfect for formalization of e.g. TAX laws and Clinical Knowledge
* Semantically rich and proven consistent
* Fully declarative, hence, no over-specification
* Performant by binding variables based on navigating relations only
* Easily extensible and integrable with the use of native classes
* Strongly typed for more consistency and extensibility
* Natural support of not (!) by reasoning over incomplete facts and falsehoods

---

## Nelumbo vs Dclare

* Both have reactive execution semantics (push) and lazy derivation (pull).
* Dclare has blackbox rules. This causes scalability issues with pushing.
* Nelumbo rules are compltally defined in Nelumo. Hence, no scalabilty issues.
* Pulling with Dclare has problems with opposite relations.
* Nelumbo is multi-directional by nature.

---

## Syntax

### Types

```text
  <Smart>   :: <Node>
  <Living>  :: <Node>
  <LLM>     :: <Smart>
  <Person>  :: <Smart>, <Living>
  <Male>    :: <Person>
  <Female>  :: <Person>
    

```

### Patterns

```text
  <Integer> ::= <NUMBER>                      @nelumbo.integers.Integer
                    
  <Integer> ::= <Integer> - <Integer>  #40,
                <Integer> + <Integer>  #40,
                          - <Integer>  #80
                              
  <Set>     ::= { <[> <Node> <{> , <Node> <}> <]> }    @nelumbo.sets.Set
```

---

## Semantics

### Variables

```text
  <Integer> a, b, c
  <Person>  x, y, z 
```

### Rules

```text
  a<=b     <=>  a<b | a=b

  a+b=c    <=>  add(a,b,c)
  a-b=c    <=>  add(c,b,a)
    
  -a=b     <=>  0-a=b
  abs(a)=b <=>  a>=0 & b=a |
                 a<0 & b=-a
                 
  d(a)=c   <=>  c(a)=c |
                d(a)=b & c(b)=c
```

---

## Running

### Queries

```text
  a+11=21   ?     // [10+11=21][..]
```

### Testing

```text
  a+11=21   ?     [10+11=21][..]
  abs(a)=10 ?     [abs(-10)=10,abs(10)=10][..]
```

### Proof

```text
  a+11=21
    a+11=21 <=> add(a,11,21)
      add(l1,11,21) [add(10,11,21)][..]
    l1+11=21 [10+11=21][..]
  a+11=21 [10+11=21][..]
```

---

## Native Semantics

### Declaration

```text
  <Predicate> ::= add(<Integer>,<Integer>,<Integer>)
                  @org.modelingvalue.nelumbo.integers.Add
```

### Java Code

```text
 protected InferResult infer(int nrOfUnbound, InferContext context) {
    if (nrOfUnbound > 1) {
        return unknown();
    }
    BigInteger addend1 = getVal(0, 0);
    BigInteger addend2 = getVal(1, 0);
    BigInteger sum = getVal(2, 0);
    if (addend1 != null && addend2 != null) {
        BigInteger s = addend1.add(addend2);
        if (sum != null) {
            boolean eq = s.equals(sum);
            return eq ? factCC() : falsehoodCC();
        } else {
            return set(2, Integer.of(s)).factCI();
        }
    } else if (addend1 != null && sum != null) {
        return set(1, Integer.of(sum.subtract(addend1))).factCI();
    } else if (addend2 != null && sum != null) {
        return set(0, Integer.of(sum.subtract(addend2))).factCI();
    } else {
        return unknown();
    }
}
```

---

## Fibonacci Example

```text
  <Integer> ::= fib(<Integer>)

  <Integer> n, f

  fib(n)=f <=> f=n                 if n<=1,
               f=fib(n-1)+fib(n-2) if n>1  
    
  fib(0)=f    ? [fib(0)=0][..]
  fib(2)=f    ? [fib(2)=1][..]
  fib(10)=f   ? [fib(10)=55][..]
  fib(100)=f  ? [fib(100)=36#22r8fozas3n8w3][..]
  fib(1000)=f ? [fib(1000)=36#18nrvsuayughau0blk8aylvbyaqwiaqba77rdsgscn5hzwgbgaws8i8svp4xdmoo82plxiyogd5iaj1cspez8zfeio92a76t9n1frssxklr92wyyxm8r903o1ofgncikuggcwnf][..]
```

---

### Literals

```text
  <Male>    ::= Hendrik, Bernhard, Claus, Willem
  <Female>  ::= Wilhelmina, Juliana, Beatrix, Maxima, Amalia
  
  <Integer> ::= <NUMBER>    @org.modelingvalue.nelumbo.integers.Integer
```

### Functions

```text
  <Integer> ::= <Integer> -  <Integer>  #40,
                <Integer> +  <Integer>  #40

  a+b=c   <=>  add(a,b,c)
  a-b=c   <=>  add(c,b,a)
```

---

### Relations and Facts

```text
  <Relation>  ::= pc(<Person>,<Person>)   // parent-child

  // Facts
  pc(Beatrix, Willem)
  pc(Claus, Willem)
  pc(Willem, Amalia)
  pc(Maxima, Amalia)
```

### Predicates

```text
  <Predicate> ::= <Integer> <= <Integer>  #30
                    
  a<=b    <=>  a<b | a=b
```

---

| Operator | Meaning |
|----------|---------|
| `::`     | Type    |
| `::=`    | Pattern |
| `<=>`    | Rule    |

---

## Demo

### LIVE

---

## Plans

* Exists and ForAll E(....) A(...)
* Namespaces
* Generics (type arguments)
* LSP (also on WEB)
* Reactive update execution semantics
* Language pattern transformations
* Deprecation and migration support
* Abstract vs Concrete language mappings (like Xtext?)
* ....

---

## Contributing

* Open source : Create tests and libraries
* Help to write scientific publication
* We are aiming for a subsidy to fund serious development in the coming years
* Github : https://github.com/ModelingValueGroup/nelumbo
* Mail   : wim.bast@gmail.com

---

# Q&A
