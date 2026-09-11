# Bug repros

Standalone `.nl` reproductions for confirmed engine findings: the 2026-09-07
multi-agent code review of core + lsp, plus the 2026-09-10 sudoku2 session (its
own section below). Each file demonstrates one bug against the CLI and is
red-by-design: while the bug exists the file fails (expectation mismatch or
crash); after the fix it passes and can be promoted into the regular test
resources.

Run all: `./run-all.sh` (needs `./gradlew cliJar` first). Assertions are enabled
(`-ea`); `multiline-string-assert.nl` needs that to show its crash.

Every expectation (`? [..][..]`) states the CORRECT behavior, so the CLI reports a
mismatch (or crashes) today. A few queries in the quantifier/diagonal files carry no
expectation on purpose: their exact correct completeness is debatable, they only
illustrate the contradiction described in the header comment.

## Found issues: 2026-09-10 sudoku2 session

Found while building `examples/sudoku-9x9-smart.nl` (a singles-first sudoku solver);
every repro was verified red against the CLI, most of them deterministically
(`nondeterministic-inference.nl` is red-by-race and may pass on a lucky run).
All run with `-DPARALLEL_COLLECTIONS=false` (run-all.sh passes it since
2026-09-10).

The last three rows are facets of one suspected root cause: predicate identity
/ interning state shared across structurally-similar predicates and across
queries. Larger-scale manifestations seen during the session (structurally
near-identical rules in different rules corrupting each other, a forwarding
rule like `sudoku2(g)=s <=> s=scan(g,0,0)` flipping results to
open/inconsistent, an unused rule being load-bearing) reproduced repeatedly
against intermediate versions of sudoku-9x9-smart.nl but NOT as single mutations of its
final content - consistent with the state/race dependence the minimal repros
demonstrate.

| Status | Location | Issue | Repro |
|---|---|---|---|
| confirmed | logic/InferResult.java:72 via lang/Lambda.java:142 and collections/Collections.java:157 | `where` set-filter over a user Boolean rule NPEs the whole run inside deep recursion (fine standalone) | where-filter-npe-in-recursion.nl |
| confirmed | logic/Predicate.callMethod -> collections/Collections.java:63 | Rule guards evaluated speculatively: unguarded `pos` accessors crash on out-of-range probes (IndexOutOfBounds) even when a preceding guard conjunct is false | speculative-guard-index-crash.nl |
| confirmed | lang/Lambda argument extraction | E/!E with 4 variables: parse error in a query, but inside a rule body it parses and then crashes the whole evaluation at runtime | four-var-quantifier-crash.nl |
| confirmed | syntax/Parser | An alternative's `if` guard on its own continuation line is reported as a parse error and then SILENTLY DROPPED: the alternative runs guardless, results become inconsistent | guard-on-continuation-line-dropped.nl |
| confirmed | syntax/Parser | Undeclared variable: misleading error pointing at a nearby token, unknown identifier never named, rule silently dropped | undeclared-variable-diagnostics.nl |
| confirmed | lambda lifting / interning | Guarded Set-valued rule with an empty-set branch, called from a map lambda: map undecided (rule fine standalone, non-empty values fine) | empty-set-branch-in-map-lambda.nl |
| confirmed | inference memoization / interning | A query's result depends on NEIGHBORING queries: pos-extraction of a mapped result is undecided alone, decided when a whole-result query precedes it; at solver scale result FORMS flip between closed and open | neighbor-query-changes-result.nl |
| confirmed | ContextPool inference race | Same file, same flags, different results run-to-run: PARALLEL_COLLECTIONS=false serializes collections but NOT inference (ContextThread.createPool, Collection.PARALLELISM floor 2) | nondeterministic-inference.nl |

## Found issues: 2026-09-11 sudoku-csp session

Found while writing `examples/sudoku-4x4-csp.nl` (a Norvig candidate-set CSP
solver). Building stalled at the functional grid update; the three deterministic
bugs below are the root causes. Each repro is condensed to the minimal trigger
(no grid / no enum needed).

| Status | Location | Issue | Repro |
|---|---|---|---|
| confirmed | logic/Predicate.callMethod (reflective native invoke) | A collection-returning functor CALL used as an argument crashes with `IllegalArgumentException: argument type mismatch`. Minimal: `x pos lst(0)` (functor-list into the native `pos`). DETERMINISTIC (9/9), independent of PARALLELISM/PARALLEL_COLLECTIONS. Binding the inner call via `E[..]` first works; arithmetic nesting works. The sudoku accessor `cell(g,r,c)=x <=> x=at(row(g,r),c)` hits it via the nested `row(g,r)` | nested-list-functor-arg-type-mismatch.nl |
| confirmed | interning / type-confusion race in recursive list-concat | REGRESSION vs 2026-09-10 notes: a recursive list-CONCAT builder crashes DETERMINISTICALLY on the CLI (5/5) with `ClassCastException: Variable cannot be cast to List`. Minimal: `build(n)` accumulating `[1..n]` by concat; it is the `rowsBefore`/`rowsFrom` shape of sudoku-*-smart.nl. Concat-free recursion is fine; the crash needs `s=pre+[rw]` in the recursion. Over a Set-element grid the same builder DIVERGES (deadline) instead; a recursive set-UNION accumulator crashes identically (so it is recursive COLLECTION accumulation in general). Both `examples/sudoku-4x4.nl` and `sudoku-4x4-smart.nl` now crash too (3/3, 2/2). Simple + collection examples still run clean | recursive-list-concat-classcast.nl |
| confirmed | logic equality resolution (collection-typed) | A collection-returning functor call on the RHS of `=` is NOT evaluated: `pick(i)=l <=> l = mk(i)` yields `l = mk(0)` (unreduced term + leaking anon binding). `mk(i) = l` (LHS) works; an Integer-returning functor works either side; a `map` on the RHS is unreduced too - so `=` is inconsistently non-commutative for collection-returning functors | set-functor-rhs-equals-not-reduced.nl |

## Found issues: 2026-09-07 code review

Status: `confirmed` = both adversarial verifiers agreed (usually with a CLI
reproduction); `half` = only one verifier ran, or one of two refuted; `unverified` =
review was stopped before verification. No finding was fully refuted.

Severity is the heading level below (as judged by the reviewers): **high** = wrong
results/crash/hang in normal use, **medium** = wrong behavior in plausible edge
cases, **low** = minor or unlikely.

### Core (`src/main/java/org/modelingvalue/nelumbo`)

#### High

| Status | Location | Issue | Repro |
|---|---|---|---|
| confirmed | ExistentialQuantifier.java:90, UniversalQuantifier.java:70 | Fully-bound quantifiers turn an unknown body result into a definitive answer | quantifier-definitive-from-unknown.nl |
| confirmed | UniversalQuantifier.java:60 | A[x] concludes true from a single witness; un-enumerated falsehoods ignored | quantifier-definitive-from-unknown.nl |
| confirmed | rationals/Rational.java:69 | normalize() keeps negative denominators: broken equality, -2 > 0 inferred | rational-sign.nl |
| confirmed | rationals/Rationals.java:81 | Zero factor builds n/0 rationals: crash or fabricated fact | rational-zero-factor.nl |
| confirmed | integers/Integers.java:74 | Division by zero crashes the whole evaluation | integers-div-zero.nl |
| confirmed | strings/Strings.java:55 | string_concat solves the wrong prefix for asymmetric splits | string-concat-prefix.nl |
| confirmed | collections/Collections.java:63 | indexOf crashes on out-of-range index; 2^32 wraps to element 0 | collections-index-out-of-range.nl |
| confirmed | patterns/SequencePattern.java:162 | alt flag dropped: multi-keyword alternation options lose their identity | alternation-option-identity.nl |
| confirmed | patterns/RepetitionPattern.java:171 | Greedy separator consumption without backtracking: crash on valid input | repetition-separator-greedy.nl |
| confirmed | tools/EditorWindow.java:1286 | No eval deadline: a divergent rule hangs the editor at 100% CPU | - (interactive) |
| unverified | KnowledgeBase.java:380 | Child KBs inherit parent memoization: child facts cannot override memoized falsehoods | - |

#### Medium

| Status | Location | Issue | Repro |
|---|---|---|---|
| confirmed | Predicate.java:335 | Unary predicates with an unbound argument are never enumerated | quantifier-definitive-from-unknown.nl |
| confirmed | KnowledgeBase.java:560 | getFacts claims "complete, no facts" for un-indexed shapes like r(a,a) | diagonal-fact-lookup.nl |
| confirmed | strings/Strings.java:82 | int(...) uses Integer.parseInt: silent 32-bit limit | integer-string-32bit.nl |
| confirmed | datetime/Multiply.java:49 | period_multiply crashes on multipliers outside int range | datetime-multiply-overflow.nl |
| confirmed | patterns/RepetitionPattern.java:161 | Keyword-only repetition loses its iteration count: rep aa == rep aa aa | repetition-count-lost.nl |
| confirmed | patterns/OptionalPattern.java:118 | Matched optional with multi-keyword body recorded as absent | optional-presence-lost.nl |
| confirmed | syntax/TokenType.java:37 | '//' directly after an operator char is swallowed into the operator token | comment-after-operator.nl |
| confirmed | syntax/Tokenizer.java:153 | Multi-line STRING token crashes checkToken with -ea (unclosed quote while typing) | multiline-string-assert.nl |
| confirmed | syntax/Token.java:431 | Caret in whitespace/comment never gets completions (skip tokens have previous == null) | - (needs LSP client) |
| confirmed | tools/EditorWindow.java:1255 | Self/mutual imports cause an infinite refresh loop | - (interactive) |
| confirmed | tools/EditorWindow.java:1258 | Non-ParseException during evaluation silently kills the window's eval loop | - (interactive) |
| confirmed | tools/EditorWindow.java:198 | Every open window permanently occupies a shared ForkJoinPool worker | - (interactive) |
| confirmed | tools/NelumboEvaluator.java:110 | Query failing with a non-mismatch error reported as expectationMatched=true | - |
| unverified | Node.java:622 + MatchState.java:139 | Rules with string-literal heads are never matched (signature key mismatch); strings that do not lex as one token NPE | - |

#### Low

| Status | Location | Issue | Repro |
|---|---|---|---|
| confirmed | datetime/GreaterThan.java:72 | Year counted as 360 days (comment promises 365); sub-seconds truncated | datetime-year-360.nl |
| confirmed | syntax/Token.java:439 | Unconditional debug println on every completion request | - |
| confirmed | syntax/Token.java:244 | Empty tokens (EOF) contain no position: no completions/hover at end of file without trailing newline | - (needs LSP client) |
| confirmed | tools/EditorWindow.java:1510 | Debounced auto-save races concurrent writers on the same file | - (interactive) |
| confirmed | tools/Main.java:53 | REPL crashes with NoSuchElementException at EOF | - (interactive) |
| half | patterns/AlternationPattern.java:129 | Failed alternation options pollute shared previous[] token spacing | - |
| half | syntax/ParseException.java:84 | Exception length garbage/negative when tokens span lines | - |
| unverified | Node.java:68, lang/Functor.java:221 | Unsynchronized lazy-init caches on objects shared across concurrent evaluations | - |

### LSP server (`lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp`)

No CLI repros possible here: these need an interactive LSP client (or threads).

#### High

| Status | Location | Issue |
|---|---|---|
| confirmed | Main.java:118 | WebSocket mode: any client's normal exit notification System.exit()s the whole multi-session server |
| confirmed | documentService/DocumentSyncService.java:66 | Blank documents are never registered; all later edits silently ignored |
| unverified | QueryResultCache.java:134 | Stdio mode auto-eval has no deadline: divergent query hangs the eval worker permanently |

#### Medium

| Status | Location | Issue |
|---|---|---|
| confirmed | documentService/DocumentSyncService.java:72 | didChange to blank content ignored: stale diagnostics/hints on an emptied editor |
| confirmed | documentService/DocumentSyncService.java:54 | didClose never clears published diagnostics; in-flight eval can re-publish after close |
| confirmed | QueryResultCache.java:114 | Backstop timeout cannot stop a runaway evaluation; threads and pool workers leak |
| confirmed | U.java:448 | makeSelectionRange NPEs when the caret is on no token or the parse has no root |
| half | Main.java:125 | Race on static Main.client can wire a session to another session's client |
| half | Main.java:144 | WebSocket sessions never dispose their Workspace: thread + document cache leak per connection |
| half | workspaceService/WorkspaceExecuteCommandService.java:73 | executeCommand evaluates synchronously on the JSON-RPC dispatch thread, no backstop |
| half | workspaceService/WorkspaceExecuteCommandService.java:57 | The exec code action sends empty arguments: feature is dead end to end |
| unverified | NlDocument.java:82 | Parse deadline is dead code: deadline only checked in Predicate.fixpoint, parsing unbounded |

#### Low

| Status | Location | Issue |
|---|---|---|
| confirmed | QueryResultCache.java:159 | evaluate()/closeDocument race re-inserts hints for a closed document |
| confirmed | documentService/DocumentFormattingService.java:787 | placeMarkerAt emits overlapping TextEdits: client rejects the whole format |
| half | Workspace.java:61 | settings.json is read/written next to the jar: crash on read-only installs |
| unverified | workspaceService/WorkspaceExecuteCommandService.java:47 | Unknown command ids throw IllegalArgumentException; null arguments NPE |
