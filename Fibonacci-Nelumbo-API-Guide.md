# Using Nelumbo: The Fibonacci Example

This guide explains how to use the Nelumbo logic programming framework in Java, by walking through the Fibonacci example.

## Overview
Nelumbo allows you to define logic variables, relations, functions, and rules in Java, and then query or test them declaratively. The Nelumbo API provides the building blocks for this logic programming style.

## 1. Defining Relations and Functions
Define a relation and a function for Fibonacci:

```java
// Relation: fib(P, Q) means Q is the Fibonacci number at position P
static Functor2<Relation, IntegerCons, IntegerCons> FIB_REL = functor2(FibonacciTest::fib);
static Relation fib(IntegerCons i, IntegerCons f) {
    return relation(FIB_REL, i, f);
}

// Function: fib(R) computes the Fibonacci number for R
static Functor1<IntegerFunc, Integer> FIB_FUNC = functor1(FibonacciTest::fib);
static IntegerFunc fib(Integer i) {
    return function(FIB_FUNC, i);
}
```

## 2. Defining Variables
You can create variables for use in rules and queries:

```java
IntegerCons P = iConsVar("P"); // Variable for integer constants
IntegerCons Q = iConsVar("Q");
Integer     R = iVar("R");     // Variable for integers
Integer     S = iVar("S");
```

## 3. Defining Constants
You can create variables for use in rules and queries:

```java
IntegerCons ZERO = i(0);    // Integer constant 0
IntegerCons  TWO = i(2);    // Integer constant 2
```

## 4. Writing Rules
Rules define the logic of the Fibonacci

```java
private void fibonacciRules() {
    integerRules(); // Import standard integer rules

    // Base cases: fib(0, 0) and fib(1, 1)
    rule(fib(P, Q), and(ge(P, i(0)), le(P, i(1)), eq(Q, P)));

    // Recursive case: fib(P, Q) if Q = fib(P-1) + fib(P-2) for P > 1
    rule(fib(P, Q), and(gt(P, i(1)), eq(plus(fib(minus(P, i(1))), fib(minus(P, i(2)))), Q)));

    // Relate function and relation forms
    rule(eq(fib(R), S), and(eq(R, P), eq(S, Q), fib(P, Q)));
}
```

## 5. Running Logic and Querying
Use the `run` method to execute logic code in a knowledge base context. You can then assert or query results:

```java
run(() -> {
    fibonacciRules();
    
    // Query: Is 8 the Fibonacci number at position 6?
    isTrue(eq(fib(i(6)), i(8)));
    
    // Query: What is the Fibonacci number at position 7?
    hasBindings(eq(fib(i(7)), P), binding(P, i(13)));
});
```

## 6. Example Test
A test for small Fibonacci numbers:

```java
@RepeatedTest(NR_OF_REPEATS * 2)
public void smallFibonacciTest() {
    run(() -> {
        fibonacciRules();
        
        hasBindings(eq(fib(i(1)), P), binding(P, i(1)));
        hasBindings(eq(fib(i(6)), P), binding(P, i(8)));
        
        isTrue(eq(fib(i(0)), i(0)));
        isTrue(eq(fib(i(1)), i(1)));
        isTrue(eq(fib(i(6)), i(8)));
    });
}
```

## 6. Summary of Logic.java API Usage
- **Variables:** `iConsVar`, `iVar`
- **Constants:** `i(long)`, `i(String, radix)`
- **Relations/Functions:** `relation`, `function`, `functor1`, `functor2`, etc.
- **Rules:** `rule(consequence, condition)`
- **Logic Composition:** `and`, `or`, `not`, etc.
- **Comparisons:** `eq`, `gt`, `le`, etc.
- **Calculations:** `plus`, `minus`, `divide`, etc.
- **Execution:** `run(() -> { ... })`
- **Assertions/Queries:** `isTrue`, `hasBindings`, `getBindings`, etc.

## 7. Conclusion
Nelumbo's API enables declarative logic programming in Java. The Fibonacci example demonstrates how to define recursive rules, relate functions and relations, and query the logic engine for results all within standard Java code.

For more details, see the source files:
- [`FibonacciTest.java`](src/test/java/org/modelingvalue/nelumbo/test/FibonacciTest.java)
- [`Logic.java`](src/main/java/org/modelingvalue/nelumbo/Logic.java)
- [`Integers.java`](src/main/java/org/modelingvalue/nelumbo/Integers.java)
