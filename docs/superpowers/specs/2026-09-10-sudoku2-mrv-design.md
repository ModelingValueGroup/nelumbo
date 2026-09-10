# sudoku2.nl - MRV candidate solver

Date: 2026-09-10
Status: superseded during implementation - see Outcome below

## Outcome (added after implementation)

The candidate-grid MRV design below proved unimplementable on the current
engine: guarded Set/List-valued rules called from map lambdas, passing
computed values between rules, `where`-filters inside recursion, and E-guards
paired with E-bodies each fail (undecided results, NPE crashes, or
"Inconsistent results"). The shipped `examples/sudoku2.nl` uses the fallback
that preserves the key insight (forced moves first, guess only when nothing
is forced) in the one rule shape the engine handles: old-sudoku.nl-style
guard-pruned recursion with a pure E/!E pair-check over `ok` for "at most one
digit fits". Measured result: all three sudoku.nl puzzles plus a generated
26-clue backtracking puzzle solve in one ~45s CLI run; sudoku.nl's "real"
puzzle alone ran 2+ hours. The engine constraints found are documented in
the file's header comments and CLAUDE.md.

## Problem

`examples/sudoku.nl` is a brute-force row-major solver. Per blank it tries 9
digits, each checked with `ok()` (three full unit scans) and placed with
`put()` (full grid rebuild via `rowsBefore`/`rowsFrom`). The two derived
puzzles run in ~3s, but the added "real" 45-blank puzzle takes over 30
minutes: the search tree explodes and dead branches are detected late.

## Goal

A second example, `examples/sudoku2.nl`, that solves genuinely hard puzzles
(25-30 clues) in reasonable time and the existing puzzles much faster.
`sudoku.nl` stays untouched as the naive comparison.

## Design

Candidate-grid solver with fewest-candidates-first search (MRV, Norvig-lite).

State threaded through the recursion:

- `g`  : `List<List<Integer>>` digit grid (0 = blank), doubles as output
- `cg` : `List<List<Set<Integer>>>` candidate grid; `{}` for placed cells,
         candidate set for blanks

Predicates:

- `put(g,r,c,d)` - one `map` over row indices `[0..8]` with an inner
  `putRow`; no `rowsBefore`/`rowsFrom` rebuild.
- `elim(cg,r,c,d)` - one nested `map` pass: placed cell -> `{}`, peers (same
  row, column or box) -> `set - {d}`, others unchanged. Replaces `ok()`.
- `pick(g,cg,n)=[r,c]` - MRV: recursion over candidate-count `n = 0,1,2,...`;
  first blank whose candidate set has size `n`. Result is a 2-list (the
  language has no tuples; `/` is exact division, so no packed-int decode).
- `solve(g,cg)=s` - no blanks -> `s=g`; else `pick`, branch `d in candidates`,
  `put` + `elim`, recurse.
- `init(g)=cg` - per cell `{1..9} - rowSet - colSet - boxSet`; the 27 unit
  sets built by set-filter (`{1..9} where [d](d in unit)`). One-time cost.

Why it is fast:

- `n=0` found first: `d in {}` fails -> instant backtrack (early conflict
  detection).
- `n=1`: forced move, no branching (naked singles for free).
- Otherwise branch on 2-3 digits instead of 9.
- Per step one map pass per grid instead of 9 `ok()` scans plus grid rebuild.

## Tests

Self-checking queries in the file itself:

- unit queries per predicate (`put`, `elim`, `pick`, `init`)
- the three puzzles from `sudoku.nl` (incl. the 45-blank one)
- one hard ~26-clue puzzle with an externally verified unique solution

## Non-goals / notes

- No hidden-singles propagation (possible later extension if needed).
- Not registered in `ExampleCatalog`/`ExamplesTest`, same as `sudoku.nl`:
  the parallel-collections bug rules out JUnit; run via CLI with
  `-DPARALLEL_COLLECTIONS=false`.
- CLAUDE.md gets the new file and run instructions.

## Risks

- Engine inference cost per op is high and hard to predict: build
  incrementally, timing with the CLI after each predicate.
- Easy puzzles may regress slightly (fixed init cost); the win is on
  anything that needs search.
