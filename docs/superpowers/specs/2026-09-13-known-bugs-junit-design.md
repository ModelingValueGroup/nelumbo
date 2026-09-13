# Known-bugs as JUnit tests - design

Date: 2026-09-13

## Goal

Today there are two test mechanisms: the JUnit suite and the standalone
`bug-repros/` directory (red-by-design `.nl` files run via the CLI). Merge them:
one big JUnit suite that also carries the known-bug repros, self-reporting when
a bug gets fixed, while keeping a CLI runner (CLI and test JVM can manifest the
same bug differently - proven 2026-09-13).

## Mechanism: `@KnownBug` (xfail)

Own JUnit 5 extension, no third-party dependency:

- `@KnownBug(value = "<symptom>", flaky = false)` on a `@Test` method.
- Test FAILS -> `TestAbortedException` -> shows as skipped/aborted (grey),
  build stays green.
- Test PASSES -> forced FAILURE: "KNOWN BUG APPEARS FIXED: <symptom> - remove
  @KnownBug and promote to RegressionTest". Every ordinary `./gradlew test`
  announces fixes by itself; no manual verification runs needed.
- `flaky = true` (only `nondeterministic-inference`): a pass also aborts, so a
  lucky run never turns the build red.
- Class-wide `@Timeout` so a diverging repro cannot hang the build.

## Layout

- `.nl` repros move (git mv) to `src/main/resources/org/modelingvalue/nelumbo/bugs/`.
- New test class `KnownBugsTest` (core), one `@Test` + `@KnownBug` per repro,
  grouped in commented sections per root-cause cluster. `bugResource()` helper
  in `NelumboTestBase`.
- `@KnownBug`/`KnownBugExtension` live in the core test sources next to it.
- `RegressionTest` stays as is: a fixed known-bug is promoted by moving the
  method there and dropping the annotation (like the two already promoted).

## Simplify + dedup (phase 1, CLI is the truth)

- Shrink each repro to its minimal trigger. Per file a fixed symptom grep
  string; it must match before AND after shrinking - no silent meaning drift.
  Big candidates: `speculative-guard-index-crash` (92 lines, contains a full
  9x9 solver), `where-filter-npe-in-recursion` (78).
- Dedup ONLY when two files exercise the truly identical construct. When in
  doubt: keep both - testing a duplicate is cheaper than losing a
  not-actually-duplicate.

## Documentation moves into the tests

- The per-bug info now in `bug-repros/README.md` (status, code location, issue
  description) moves into the header comment of the owning `.nl` file and/or
  the comment on its JUnit method.
- Findings WITHOUT an `.nl` repro (interactive/editor/LSP ones) become a
  comment block in `KnownBugsTest`'s class javadoc, so nothing is lost.
- `bug-repros/` is then deleted entirely.

## CLI runner

- `run-all.sh` moves to the repo root as `run-all-tests-with-CLI` (bash), path
  updated to the new resources location. It stays the CLI-manifestation truth
  for Wim; the JUnit side tests the test-JVM manifestation.

## Test-JVM verification (phase 2 gate)

Each converted test must be proven RED in the test JVM (manifestation may
differ from the CLI). Files that are not red there (e.g. the
quantifier/diagonal queries that deliberately carry no expectation) either get
a sharpened expectation or stay CLI-only, documented in the class javadoc.

## Process

- Feature branch `known-bugs-junit` off `develop`; small commits per step.
- CLAUDE.md updated at the end (bug-repros section rewritten).

## Non-goals

- No new bug hunting; content moves and shrinks, meaning stays identical.
- No promotion decisions: everything currently red stays `@KnownBug`.
- The LSP/interactive findings get no new repro attempts.
