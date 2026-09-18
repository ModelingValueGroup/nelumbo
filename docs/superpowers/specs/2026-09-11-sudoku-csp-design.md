# Sudoku CSP solver (Norvig) - design

Date: 2026-09-11

## Goal

The real goal is NOT to solve sudokus but to **surface engine bugs** in Nelumbo so
Wim can fix them. This is the third sudoku solver (after `sudoku-9x9.nl`
brute-force and `sudoku-9x9-smart.nl` singles-first). It implements Peter
Norvig's "Solving Every Sudoku Puzzle" algorithm **exactly as described in the
literature**.

**Hard rule: do NOT work around engine issues.** The earlier smart solver
deliberately stayed inside the one proven-safe rule shape to dodge crashes. Here
we do the opposite: write the algorithm the natural, faithful way. When the
engine crashes, goes undecided, or is nondeterministic, STOP, minimise a
red-by-design repro into `bug-repros/`, and report it. Do not reshape the code to
avoid it. (Functional threading instead of Norvig's dict mutation is the
language paradigm, not a workaround.)

## Algorithm (Norvig, faithful)

Candidate-set model. Each cell holds the set of digits still possible. Two
propagation rules plus depth-first search:

- `assign(g,r,c,d)`: eliminate every OTHER candidate from cell (r,c) - i.e.
  `eliminate(g,r,c,d2)` for each `d2` currently in the cell with `d2 != d`.
- `eliminate(g,r,c,d)`:
  - if `d` no longer in the cell -> unchanged.
  - remove `d`; if the cell becomes EMPTY -> contradiction (fail).
  - (naked single) if the cell is reduced to one value `d2` -> eliminate `d2`
    from all PEERS of (r,c).
  - (hidden single) for each UNIT of (r,c): count the places where `d` is still
    a candidate; 0 places -> fail; exactly 1 place `s2` -> `assign(g,s2,d)`.
- `search(g)`:
  - all cells singleton -> solved.
  - else pick the unfilled cell with the FEWEST candidates (MRV heuristic), try
    each candidate `d`: `assign` then `search`, first success wins.
- `parse(puzzle)`: start every cell full, then `assign` each given clue.
- `sudokuCSP(puzzle) = search(parse(puzzle))`.

The mutual recursion `assign <-> eliminate <-> assign` and the deep peer/unit
iteration are the heart of the algorithm and the main bug-finding surface.

## Representation

- **Options are a type, not integers** (Tom's call, and Norvig-faithful - he
  treats digits as symbols). Idiomatic Nelumbo enum, like `Lidwoord ::= de, het`
  (deHet.nl) and `Male ::= Hendrik, ...` (family.nl):

  ```
  Digit :: Object
  Digit ::= D1, D2, D3, D4          // 9x9: D1..D9
  ```

  This removes a whole class of accidental arithmetic on option tokens (the smart
  solver did `x < y` on digits, which is meaningless), and it exercises `Set<E>`
  over a USER type + generic type parameter + finite-domain enumeration - a
  different engine surface than integer sets. If that breaks, it is a finding.

- **Candidate grid:** `List<List<Set<Digit>>>`. Cell (r,c) = its candidate set.
  Given clue -> singleton `{Dk}`; blank -> full set `{D1,D2,D3,D4}`. Deeply
  nested generics + functional grid update over sets - untested, bug-rich.

- **`Integer` stays** for coordinates (r,c), box base, and cardinality (`|s|`,
  MRV comparison). Two types in play by design: `Digit` for values, `Integer`
  for positions/counts.

## Set primitives used (all present in nelumbo.collections)

- `s - {d}` difference (eliminate), `|s|` cardinality (empty/singleton/MRV),
  `d in s` membership, `{...}` literals, subset `<`. Cardinality and MRV stay in
  Integer space.

## Peers / units

Computed by rules, mirroring the existing `ok`/`bb` in `sudoku-4x4-smart.nl`:
row cells, column cells, box cells (box base via `bb`). `peers(r,c)` = the three
units minus the cell itself.

## IO seam (keeps puzzles and the oracle readable)

`0` can no longer mean blank. Internally, unknown = full set (Norvig-natural).
At the boundary only:

- puzzle input stays `List<List<Integer>>` (0 = blank, 1..k = clue); a thin
  `dig(1)=D1, dig(2)=D2, ...` relation maps to `Digit` during `parse`.
- `unparse(g)=List<List<Integer>>` maps each solved singleton back to its
  integer, so the query expectation reads as the plain solved grid.

This is pure boundary conversion, not an engine dodge. It also isolates the
solver from phase 3: the DSL only has to produce the same `List<List<Integer>>`.

## Puzzles & oracle

Reuse the puzzles already vetted in the smart solvers, so the known solution is
the correctness oracle:

- 4x4: the three puzzles from `sudoku-4x4-smart.nl`, solution
  `[[1,2,3,4],[3,4,1,2],[2,1,4,3],[4,3,2,1]]`.
- 9x9: the puzzles from `sudoku-9x9-smart.nl`.

Each query expectation states the CORRECT solved grid, so any crash or mismatch
is a bug to report - not something to reshape around.

## Phases

1. `examples/sudoku-4x4-csp.nl` - Norvig solver, digits D1..D4, 2x2 boxes,
   list-literal input. Small enough to hand-trace and isolate bugs.
2. `examples/sudoku-9x9-csp.nl` - 9x9 copy, digits D1..D9, 3x3 boxes.
3. **Grid DSL** (separate bug pass) - a Root-extending functor giving an
   intuitive grid notation that produces the phase-1/2 input grid:

   ```
   Puzzle :: Root
   Puzzle ::= sudoku <grid> #0     // grid = repetition of rows; row = repetition of cells; . = blank
   ```

   Deliberately last and on its own: it exercises the known-fragile parser areas
   (Root-extending functors + the explicit `#N` precedence gotcha, nested
   repetition patterns, multi-line newline/continuation handling), so keeping it
   separate preserves clean bug attribution between solver bugs and parser bugs.

## Running & verification (CLI-only)

Like the other sudoku files, NOT wired into `ExamplesTest`/`ExampleCatalog`
(parallel-inference nondeterminism crashes the Gradle test JVM). Verify with the
CLI:

```
./gradlew :cli:cliJar
java -DPARALLEL_COLLECTIONS=false -jar cli/build/libs/nelumbo-cli-*.jar examples/sudoku-4x4-csp.nl
```

- verify after every rule while building; run twice (results are
  nondeterministic run-to-run - see the engine-constraints notes).
- grep output case-INSENSITIVELY for `unexpected|exception|inconsistent|expected
  result` (a case-sensitive grep once missed silently-dropped-guard parse errors
  for hours).
- optional: a `@Disabled` entry in `SudokuExamplesTest` for parity, matching the
  existing CLI-only sudoku methods.

## Bug handling

For every engine issue hit: capture a minimal red-by-design `.nl` in
`bug-repros/` (expectation states the CORRECT behaviour), add a line under a
2026-09-11 section of `bug-repros/README.md`, and report it for Wim. Never
refactor the solver to avoid the defect.

## Non-goals

- Not a fast or optimised solver.
- Not wired into JUnit as a passing test.
- Not reusing the per-digit `ok`-scan of the smart solver (that is the
  safe-shape dodge this exercise rejects).
