# Nelumbo Feature Tour Site

Date: 2026-07-11
Status: approved design, pending implementation plan

## Goal

Turn the demo site into a feature tour: a sidebar-navigated site that explains each
major Nelumbo capability and gives the learner editable areas to test and train,
including self-checking exercises. Built on the existing Monaco/LSP field component
and the `/lsp` + `/eval` server endpoints.

## Decisions

- Structure: left sidebar of feature areas + a content pane that shows one section at
  a time.
- Test/train: each section has prose, a pre-seeded demo field, and 1+ exercise cards.
  An exercise is a challenge plus a field whose query carries an expected result
  (`... =EXPECTED ?`), so the editor's mismatch squiggle and inline result confirm a
  correct solution with no grading code. Each exercise has a "Show solution" reveal.
- Coverage: comprehensive, 8 sections (see below).
- Routing: the tour is the landing page at `/`; the free-form playground moves to
  `/playground.html` and is linked from the tour. The old `/demo.html` is removed.
- Prose: English. The Dutch `de/het` grammar example may appear as an optional DSL
  showcase; the primary DSL demo is the English "Who is a friend of Piet?" example.

## Routing changes

- `GET /` -> `tour.html` (new).
- `GET /playground.html` -> the current playground (single large free-form field).
- `GET /demo.html` -> removed.
- `/assets/*` -> unchanged (bundled Monaco/LSP frontend).
- `/eval`, `/eval/trace`, `/metadata`, `/health`, `/lsp` -> unchanged.

## Layout

`tour.html`, dark theme matching the current site:

```
+-------------------+--------------------------------------------+
| Nelumbo           |  ## Rules and recursion                    |
|                   |  prose explaining <=> rules and recursion  |
| > Logic           |                                            |
|   Facts & queries |  [ demo field: fib defined      Run ]      |
|   Rules           |  results panel                             |
|   Data types      |                                            |
|   Relations       |  Challenge: define factorial(n)            |
|   Custom DSL      |  [ exercise field: factorial(5)=120 ? Run ]|
|   Transformations |  [ Show solution ]                         |
|   Scoping         |                                            |
|                   |                                            |
| playground ->     |                                            |
+-------------------+--------------------------------------------+
```

- Sidebar lists the 8 sections plus a link to `/playground.html`.
- Clicking a sidebar item hides the other sections, shows the target, marks it active,
  and lazily mounts that section's editors the first time it is shown.
- Only the active (and previously visited) sections have live Monaco editors, keeping
  the page light despite ~20 total fields.

## Sections (8)

Each: prose + one demo field + 1-2 exercises with solutions. Every snippet is
self-contained (`import ...`).

1. Propositional logic - three-valued `&` `|` `!`, quantifiers `E[x]` `A[y]`; the
   `[()][]` / `[..][..]` result notation for true/false/unknown.
2. Facts and queries - `fact`, object/type declarations, querying with variables and
   the `? [expected][..]` notation.
3. Rules and recursion - `<=>` rules with `if` guards; fib; exercise: factorial.
4. Data types - integers, rationals, strings, collections, datetime (one compact demo
   each; exercises on a couple).
5. Relational knowledge bases - the family example (parent/child/ancestor/mother/
   father via rules and quantifiers); exercise: add a `grandparent` rule.
6. Custom DSL syntax - define functors/patterns to get natural-language statements and
   queries ("Who is a friend of Piet?"); exercise: add a phrase.
7. Transformations - `::>` transformation rules / attribute definitions; exercise: a
   small attribute.
8. Scoping and namespaces - `{ }` blocks, `private`, reusing a name in two scopes.

Section content is sourced/adapted from `src/main/resources/org/modelingvalue/nelumbo/
examples/*.nl` and `tests/*.nl`, verified to evaluate (see below).

## Frontend component changes

In `http/src/main/frontend/src/nelumbo-fields.ts`, split the current
`initNelumboFields()` into reusable pieces and add the solution reveal:

- `connect(): Promise<LanguageClient|null>` - establish the single page-shared `/lsp`
  language client (module-level singleton; safe to call repeatedly). On failure returns
  null and shows the existing "language features unavailable" banner.
- `mountFields(container: ParentNode): void` - upgrade every `.nelumbo-field` in
  `container` that is not already mounted (idempotent via a marker), creating the Monaco
  editor, Run button, results panel, and - if a `.nelumbo-solution` sibling exists - a
  "Show solution" toggle in the toolbar that reveals/hides that block (the learner's
  attempt is never overwritten).
- `initNelumboFields(): Promise<void>` - unchanged behavior for the playground:
  `connect()` then `mountFields(document)`.

The tour page calls `connect()` once on load, then `mountFields(sectionEl)` when a
section is first shown. Monaco/monaco-languageclient sends `didOpen` for models created
after the client starts, so lazy mounting still gets full LSP features.

Exercise markup contract:

```html
<div class="nelumbo-exercise">
  <p class="challenge">Define factorial so that factorial(5) = 120.</p>
  <div class="nelumbo-field">import nelumbo.integers
Integer ::= factorial(&lt;Integer&gt;)
Integer n, r
factorial(5)=120 ?
</div>
  <pre class="nelumbo-solution">... a known-good full solution ...</pre>
</div>
```

The expected result lives in the query line (`factorial(5)=120 ?`); the editor squiggles
until the learner's rule makes it match. `.nelumbo-solution` starts hidden.

## Page navigation script

A small script in `tour.html` (not the bundle): builds sidebar behavior, shows one
`.tour-section` at a time, tracks which sections are mounted, and calls
`NelumboFields.connect()` / `NelumboFields.mountFields(section)`. No routing framework.

## Content correctness

Every demo, exercise, and solution snippet is verified before shipping: unescape the
HTML entities, write to a temp `.nl` file, and run the CLI
(`java -jar build/libs/nelumbo-*-cli.jar file.nl`, exit 0). Exercises must actually go
green when solved: the exercise field (before solving) shows the expected-result
mismatch, and pasting/loading the `.nelumbo-solution` makes it pass. This is the bulk of
the work.

## Testing

- Java: update `NelumboHttpServerTest` - `/` now serves the tour (assert sidebar +
  bundle + fields), add `/playground.html` served, drop/replace the `/demo.html` test;
  keep the `/assets` bundle test.
- Frontend: `tsc --noEmit` typecheck as part of the bundle build.
- Snippet correctness: CLI-verified during authoring (not automated in CI).
- Browser runtime (editor mounts, section switching, squiggle-on-mismatch, Show
  solution) is smoke-tested manually; no Playwright.

## Out of scope

- Progress tracking / accounts / persistence of learner input.
- Server-side grading (self-check is client-side via the LSP mismatch diagnostic).
- Automated browser E2E tests.
- New language features; this is presentation only.

## Sizing

- Frontend component split + solution reveal: small.
- Tour page shell (sidebar, section switching, styles): medium.
- Eight sections of verified prose + demo + exercises + solutions: the bulk.
- Server routing + test updates: small.
