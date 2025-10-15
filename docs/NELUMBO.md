# Nelumbo

## Logic Meta Language

* Fast logic Reasoning
* Fully Declarative
* Define and parse Syntaxes
* Define and execute Semantics
* Define and run Tests
* IDE integration using LSP 

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
  a<=b     <==>  a<b | a=b

  a+b=c    <==>  add(a,b,c)
  a-b=c    <==>  add(c,b,a)
    
  -a=b     <==>  0-a=b
  abs(a)=b <==>  a>=0 & b=a |
                 a<0 & b=-a
                 
  d(a)=c   <==>  c(a)=c |
                 d(a)=b & c(b)=c
```

---
## Native Semantics

### Declaration
```text
  <Predicate> ::= add(<Integer>,<Integer>,<Integer>)
                  @org.modelingvalue.nelumbo.integers.Add
```

### Java Code
```small
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

## Functions and Literals

TODO

## Predicates and Relations

TODO

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

---

## Rationale

* Formalization of TAX laws and Clinical guidelines and protocols
* Semantical rich and no over-specification (hence declarative)
* Perfomant by binding variables by navigating relations only
* Easily extensible and integrable by native classes for relations
* Strongly typed for more consistency and extensibility
* Natural support of not (!) by reasoning over incomplete facts and falsehoods 

---

## Demo

LIVE

---

## Contributing

TODO
