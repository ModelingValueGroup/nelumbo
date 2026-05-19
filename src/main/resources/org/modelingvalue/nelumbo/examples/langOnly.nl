
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
    //   - functors on Root-derived types, so their ground instances are
    //     valid top-level statements (no `fact`/`?` needed to use them)
    //   - every Pattern subtype exercised in such a functor body:
    //     TokenTextPattern, TokenTypePattern (via NATIVE node types),
    //     NodeTypePattern, SequencePattern (`()`, `[]`, `{}`),
    //     AlternationPattern, OptionalPattern, RepetitionPattern
    //     (one-or-more, zero-or-more, with separator)
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

    // =========================================================
    // Pattern-type coverage
    //
    // Each functor below is declared on a subtype of `Root`, so its
    // ground instances are valid top-level statements that the parser
    // will check. Comments name the Pattern subtype being exercised.
    // =========================================================

    // Root-derived functor declarations use an explicit `#0` precedence,
    // matching the convention used by `nelumbo.logic` (see logic.nl) and
    // by `deHet.nl`. Without it, the parser cannot disambiguate repeated
    // instances of the new functors against other Root alternatives.

    // --- 1. TokenTextPattern: a bare NAME literal in the functor body ---
    Greeting :: Root
    Greeting ::= hello #0

    hello

    // --- 2. AlternationPattern: <(> a <|> b <|> c <)> ---
    Salute :: Root
    Salute ::= salute <(> hi <|> hey <|> howdy <)> #0

    salute hi
    salute hey
    salute howdy

    // --- 3. OptionalPattern: <(> p <)?> ---
    Maybe :: Root
    Maybe ::= maybe <(> bang <)?> #0

    maybe
    maybe bang

    // --- 4. RepetitionPattern, one-or-more: <(> p <)+> ---
    Many :: Root
    Many ::= many <(> bing <)+> #0

    many bing
    many bing bing
    many bing bing bing

    // --- 5. RepetitionPattern, zero-or-more: <(> p <)*> ---
    Any :: Root
    Any ::= any <(> zap <)*> #0

    any
    any zap
    any zap zap zap

    // --- 6. RepetitionPattern with separator: <(> p <,> sep <)+> ---
    Shade :: Object
    Shade ::= shred, sgreen, sblue
    Listing :: Root
    Listing ::= list <(> <Shade> <,> , <)+> #0

    list shred
    list shred, sgreen
    list shred, sgreen, sblue

    // --- 7. TokenTypePattern via NATIVE node types (<NAME>, <NUMBER>, <STRING>) ---
    Echo  :: Root
    Echo  ::= echo <NAME>   #0
    Count :: Root
    Count ::= count <NUMBER> #0
    Say   :: Root
    Say   ::= say <STRING>  #0

    echo hello
    count 42
    say "howdy"

    // --- 8. SequencePattern, three delimiter flavours: (), [], {} ---
    Bag  :: Object
    Bag  ::= bag(<Shade>)
    Tray :: Object
    Tray ::= tray[<Shade>]
    Cell :: Object
    Cell ::= cell{<Shade>}

    // NodeTypePattern + Alternation, used to surface each at top level.
    Show :: Root
    Show ::= show <(> <Bag> <|> <Tray> <|> <Cell> <)> #0

    show bag(shred)
    show tray[sgreen]
    show cell{sblue}

    // --- 9. NodeTypePattern with explicit precedence: <Type#N> ---
    Pair :: Root
    Pair ::= pair <Shade#100> <Shade#100> #0

    pair shred sgreen
    pair sblue sblue
