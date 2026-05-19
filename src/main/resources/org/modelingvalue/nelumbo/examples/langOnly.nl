
    // Demonstrates what a `.nl` file can declare with only `nelumbo.lang`
    // in scope — no Boolean, no equality, no `fact`/`<=>`/`?`. Useful as a
    // smoke test that the bootstrap layer is self-contained.
    //
    // With only `import nelumbo.lang`, the following work:
    //   - type declarations and inheritance (single and multiple)
    //   - generic type declarations (`Type T`, `Box<T> :: ...`)
    //   - functor declarations with literal alternatives
    //   - functor declarations on generic types, including holes
    //   - variable declarations
    //   - private-functor scope blocks
    //
    // What is NOT available without also importing `nelumbo.logic`:
    //   - `=`, `!=`, `&`, `|`, `!`, `->`, `<->`, `E[]`, `A[]`
    //   - `fact`, `<=>` rules, `?` queries
    //   - therefore: there are no testable assertions in this file.
    //   This file passes simply by parsing and loading without errors.

    import nelumbo.lang

    // --- Type hierarchy ---
    Animal :: Object
    Mammal :: Animal
    Dog    :: Mammal
    Cat    :: Mammal

    // Multiple inheritance
    Smart  :: Object
    Living :: Object
    Person :: Smart, Living

    // --- Literal alternatives ---
    Dog    ::= Rex, Fido
    Cat    ::= Whiskers, Tom
    Person ::= Alice, Bob

    // --- Variable declarations ---
    Animal a, b
    Dog    d
    Cat    ct
    Person p, q

    // --- Generic type parameter and a generic functor ---
    Type T
    Box<T> :: Object
    Box<T> ::= box(<T>)

    // --- Private-functor scope block ---
    {
       Local :: Object
       private Local ::= X, Y, Z
       Local v
    }
    
    Color :: Object
    
    Color ::= mix(<Color>,<Color>)
