# Bug repros

Standalone `.nl` reproductions for confirmed findings of the 2026-09-07 multi-agent
code review of core + lsp. Each file demonstrates one bug against the CLI and is
red-by-design: while the bug exists the file fails (expectation mismatch or crash);
after the fix it passes and can be promoted into the regular test resources.

Run all: `./run-all.sh` (needs `./gradlew cliJar` first). Assertions are enabled
(`-ea`); `multiline-string-assert.nl` needs that to show its crash.

Every expectation (`? [..][..]`) states the CORRECT behavior, so the CLI reports a
mismatch (or crashes) today. A few queries in the quantifier/diagonal files carry no
expectation on purpose: their exact correct completeness is debatable, they only
illustrate the contradiction described in the header comment.

## Found issues

Status: `confirmed` = both adversarial verifiers agreed (usually with a CLI
reproduction); `half` = only one verifier ran, or one of two refuted; `unverified` =
review was stopped before verification. No finding was fully refuted.

### Core (`src/main/java/org/modelingvalue/nelumbo`)

| Status | Location | Issue | Repro |
|---|---|---|---|
| confirmed | ExistentialQuantifier.java:90, UniversalQuantifier.java:70 | Fully-bound quantifiers turn an unknown body result into a definitive answer | quantifier-definitive-from-unknown.nl |
| confirmed | UniversalQuantifier.java:60 | A[x] concludes true from a single witness; un-enumerated falsehoods ignored | quantifier-definitive-from-unknown.nl |
| confirmed | Predicate.java:335 | Unary predicates with an unbound argument are never enumerated | quantifier-definitive-from-unknown.nl |
| confirmed | KnowledgeBase.java:560 | getFacts claims "complete, no facts" for un-indexed shapes like r(a,a) | diagonal-fact-lookup.nl |
| confirmed | rationals/Rational.java:69 | normalize() keeps negative denominators: broken equality, -2 > 0 inferred | rational-sign.nl |
| confirmed | rationals/Rationals.java:81 | Zero factor builds n/0 rationals: crash or fabricated fact | rational-zero-factor.nl |
| confirmed | integers/Integers.java:74 | Division by zero crashes the whole evaluation | integers-div-zero.nl |
| confirmed | strings/Strings.java:55 | string_concat solves the wrong prefix for asymmetric splits | string-concat-prefix.nl |
| confirmed | strings/Strings.java:82 | int(...) uses Integer.parseInt: silent 32-bit limit | integer-string-32bit.nl |
| confirmed | collections/Collections.java:63 | indexOf crashes on out-of-range index; 2^32 wraps to element 0 | collections-index-out-of-range.nl |
| confirmed | datetime/Multiply.java:49 | period_multiply crashes on multipliers outside int range | datetime-multiply-overflow.nl |
| confirmed | datetime/GreaterThan.java:72 | Year counted as 360 days (comment promises 365); sub-seconds truncated | datetime-year-360.nl |
| confirmed | patterns/SequencePattern.java:162 | alt flag dropped: multi-keyword alternation options lose their identity | alternation-option-identity.nl |
| confirmed | patterns/RepetitionPattern.java:161 | Keyword-only repetition loses its iteration count: rep aa == rep aa aa | repetition-count-lost.nl |
| confirmed | patterns/RepetitionPattern.java:171 | Greedy separator consumption without backtracking: crash on valid input | repetition-separator-greedy.nl |
| confirmed | patterns/OptionalPattern.java:118 | Matched optional with multi-keyword body recorded as absent | optional-presence-lost.nl |
| confirmed | syntax/TokenType.java:37 | '//' directly after an operator char is swallowed into the operator token | comment-after-operator.nl |
| confirmed | syntax/Tokenizer.java:153 | Multi-line STRING token crashes checkToken with -ea (unclosed quote while typing) | multiline-string-assert.nl |
| confirmed | syntax/Token.java:431 | Caret in whitespace/comment never gets completions (skip tokens have previous == null) | - (needs LSP client) |
| confirmed | syntax/Token.java:439 | Unconditional debug println on every completion request | - |
| confirmed | syntax/Token.java:244 | Empty tokens (EOF) contain no position: no completions/hover at end of file without trailing newline | - (needs LSP client) |
| confirmed | tools/EditorWindow.java:1286 | No eval deadline: a divergent rule hangs the editor at 100% CPU | - (interactive) |
| confirmed | tools/EditorWindow.java:1255 | Self/mutual imports cause an infinite refresh loop | - (interactive) |
| confirmed | tools/EditorWindow.java:1258 | Non-ParseException during evaluation silently kills the window's eval loop | - (interactive) |
| confirmed | tools/EditorWindow.java:198 | Every open window permanently occupies a shared ForkJoinPool worker | - (interactive) |
| confirmed | tools/EditorWindow.java:1510 | Debounced auto-save races concurrent writers on the same file | - (interactive) |
| confirmed | tools/NelumboEvaluator.java:110 | Query failing with a non-mismatch error reported as expectationMatched=true | - |
| confirmed | tools/Main.java:53 | REPL crashes with NoSuchElementException at EOF | - (interactive) |
| half | patterns/AlternationPattern.java:129 | Failed alternation options pollute shared previous[] token spacing | - |
| half | syntax/ParseException.java:84 | Exception length garbage/negative when tokens span lines | - |
| unverified | KnowledgeBase.java:380 | Child KBs inherit parent memoization: child facts cannot override memoized falsehoods | - |
| unverified | Node.java:622 + MatchState.java:139 | Rules with string-literal heads are never matched (signature key mismatch); strings that do not lex as one token NPE | - |
| unverified | Node.java:68, lang/Functor.java:221 | Unsynchronized lazy-init caches on objects shared across concurrent evaluations | - |

### LSP server (`lsp/server/src/main/java/org/modelingvalue/nelumbo/lsp`)

No CLI repros possible here: these need an interactive LSP client (or threads).

| Status | Location | Issue |
|---|---|---|
| confirmed | Main.java:118 | WebSocket mode: any client's normal exit notification System.exit()s the whole multi-session server |
| confirmed | documentService/DocumentSyncService.java:66 | Blank documents are never registered; all later edits silently ignored |
| confirmed | documentService/DocumentSyncService.java:72 | didChange to blank content ignored: stale diagnostics/hints on an emptied editor |
| confirmed | documentService/DocumentSyncService.java:54 | didClose never clears published diagnostics; in-flight eval can re-publish after close |
| confirmed | QueryResultCache.java:114 | Backstop timeout cannot stop a runaway evaluation; threads and pool workers leak |
| confirmed | QueryResultCache.java:159 | evaluate()/closeDocument race re-inserts hints for a closed document |
| confirmed | U.java:448 | makeSelectionRange NPEs when the caret is on no token or the parse has no root |
| confirmed | documentService/DocumentFormattingService.java:787 | placeMarkerAt emits overlapping TextEdits: client rejects the whole format |
| half | Main.java:125 | Race on static Main.client can wire a session to another session's client |
| half | Main.java:144 | WebSocket sessions never dispose their Workspace: thread + document cache leak per connection |
| half | workspaceService/WorkspaceExecuteCommandService.java:73 | executeCommand evaluates synchronously on the JSON-RPC dispatch thread, no backstop |
| half | workspaceService/WorkspaceExecuteCommandService.java:57 | The exec code action sends empty arguments: feature is dead end to end |
| half | Workspace.java:61 | settings.json is read/written next to the jar: crash on read-only installs |
| unverified | QueryResultCache.java:134 | Stdio mode auto-eval has no deadline: divergent query hangs the eval worker permanently |
| unverified | NlDocument.java:82 | Parse deadline is dead code: deadline only checked in Predicate.fixpoint, parsing unbounded |
| unverified | workspaceService/WorkspaceExecuteCommandService.java:47 | Unknown command ids throw IllegalArgumentException; null arguments NPE |
