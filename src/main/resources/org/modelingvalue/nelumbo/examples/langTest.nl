
    import nelumbo.logic

    // =========================================================
    // 1. Simple type declaration and literal enumeration
    // =========================================================

    Color :: Object
    Color ::= red, green, blue

    Color c
    c=red    ? [(c=red)][..]
    c=green  ? [(c=green)][..]
    red=red  ? [()][]
    red=blue ? [][()]
    red!=blue ? [()][]

    // =========================================================
    // 2. Single-inheritance chain — a Dog literal is also a Mammal and an Animal
    // =========================================================

    Animal :: Object
    Mammal :: Animal
    Dog    :: Mammal
    Cat    :: Mammal

    Dog ::= Rex, Fido
    Cat ::= Whiskers, Tom

    Dog    d
    Cat    ct
    Mammal m
    Animal a

    // A Dog literal binds at its own type and at every supertype.
    d=Rex ? [(d=Rex)][..]
    m=Rex ? [(m=Rex)][..]
    a=Rex ? [(a=Rex)][..]

    // A Cat literal does the same on the other branch of the hierarchy.
    ct=Whiskers ? [(ct=Whiskers)][..]
    m=Whiskers  ? [(m=Whiskers)][..]
    a=Whiskers  ? [(a=Whiskers)][..]

    // Cross-branch bindings are proven false — Dog and Cat are siblings,
    // so a Cat literal can never equal a Dog variable, and vice versa.
    //
    // With a variable on one side, the facts side is closed-empty (no value
    // of the variable can satisfy the equation), and the falsehoods side
    // stays open (the reasoner does not enumerate every Dog literal).
    d=Whiskers   ? [][..]
    ct=Rex       ? [][..]

    // With concrete literals on both sides, both sides close.
    Rex=Whiskers ? [][()]
    Whiskers=Rex ? [][()]

    // Same-type, distinct literals are also proven false.
    Rex=Fido         ? [][()]
    Whiskers=Tom     ? [][()]
    Rex=Rex          ? [()][]
    Whiskers=Whiskers ? [()][]

    // =========================================================
    // 3. Multiple inheritance
    // =========================================================

    Smart  :: Object
    Living :: Object
    Person :: Smart, Living

    Person ::= Alice, Bob

    Smart  s
    Living l

    s=Alice  ? [(s=Alice)][..]
    l=Alice  ? [(l=Alice)][..]
    Alice=Bob ? [][()]

    // =========================================================
    // 4. Comma-separated alternative declarations spanning lines
    // =========================================================

    Lidwoord :: Object
    Lidwoord ::= de,
                 het,
                 een

    Lidwoord lw
    lw=de  ? [(lw=de)][..]
    lw=een ? [(lw=een)][..]
    de=het ? [][()]

    // =========================================================
    // 5. Multi-name variable declarations
    // =========================================================

    Color c1, c2, c3
    c1=red   ? [(c1=red)][..]
    c2=green ? [(c2=green)][..]
    c3=blue  ? [(c3=blue)][..]

    // =========================================================
    // 6. Generic parenthesisation — `T ::= (<T>)` from lang.nl
    //    Wrapping a literal in parentheses yields the same literal,
    //    for any type, at any nesting depth.
    // =========================================================

    (red)     = red   ? [()][]
    ((red))   = red   ? [()][]
    (((red))) = red   ? [()][]
    (Alice)   = Alice ? [()][]
    (Rex)     = Rex   ? [()][]
    (red)     = blue  ? [][()]

    // =========================================================
    // 7. Private functor confined to its scope
    // =========================================================

    {
       Aa :: Object
       private Aa ::= X, Y, Z
       Aa v
       v=X ? [(v=X)][..]
       X=Y ? [][()]
       Z=Z ? [()][]
    }

    {
       // A second scope with its own private type — same literal names X, Y
       // would be independent of the ones above.
       Bb :: Object
       private Bb ::= X, Y
       Bb v
       v=X ? [(v=X)][..]
       X=Y ? [][()]
    }

    // =========================================================
    // 8. Generic type parameter — `Type T` from lang.nl plus a
    //    generic functor pattern `box(<T>)` exercising the
    //    `<NAME> < <Type> >` parameterised-type syntax.
    // =========================================================

    Type T
    Box<T> :: Object
    Box<T> ::= box(<T>)

    // Structural equality on ground terms of a generic functor
    box(red)=box(red)     ? [()][]
    box(red)=box(green)   ? [][()]
    box(Rex)=box(Rex)     ? [()][]
    box(Alice)=box(Alice) ? [()][]
    box(Alice)=box(Bob)   ? [][()]

    // Parenthesisation applies to generic types too
    (box(red))=box(red)   ? [()][]
