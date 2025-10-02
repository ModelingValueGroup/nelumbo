# Nelumbo

## Logic Meta Language

* Fast logic Reasoning
* Define and parse Syntaxes
* Define and execute Semantics
* Define and run Tests
* Fully Declarative

---

## Syntax

### Types

```text
  <Person>  :: <Node>
  <Male>    :: <Person>
  <Female>  :: <Person>
    
  <Set>     :: <Node>
```

### Patterns

```text
  <Integer> ::= <NUMBER>                       @nelumbo.integers.Integer
                    
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
```

### Rules

```text
  a<=b     <==>  a<b | a=b

  a+b=c    <==>  add(a,b,c)
  a-b=c    <==>  add(c,b,a)
    
  -a=b     <==>  0-a=b
  abs(a)=b <==>  (a>=0 & b=a) | (a<0 & b=-a)
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
  abs(a)=10 ?     [abs(-10)=10,abs(10)=10][abs(0)=10,..]
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
