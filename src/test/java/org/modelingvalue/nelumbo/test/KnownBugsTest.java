//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2026 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
//                                                                                                                     ~
// Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in      ~
// compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0  ~
// Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on ~
// an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the  ~
// specific language governing permissions and limitations under the License.                                          ~
//                                                                                                                     ~
// Maintainers:                                                                                                        ~
//     Wim Bast, Tom Brus                                                                                              ~
//                                                                                                                     ~
// Contributors:                                                                                                       ~
//     Victor Lap                                                                                                      ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.nelumbo.test;

import org.junit.jupiter.api.Test;

// The known-bug (xfail) suite. Every method runs a red-by-design .nl repro
// from src/main/resources/org/modelingvalue/nelumbo/bugs/ whose query
// expectations state the CORRECT behavior. While the bug exists the method is
// ABORTED (grey); once fixed it FAILS with "KNOWN BUG APPEARS FIXED" - then
// remove the @KnownBug and promote the method to RegressionTest (nine
// promoted so far: two on 2026-09-11, three on 2026-09-24, four on 2026-09-25).
//
// The CLI twin is ./run-all-tests-with-CLI (repo root): the SAME bug can
// manifest differently on the CLI vs this test JVM (proven 2026-09-13: the
// bba88fc8 reduction bug is a ClassCastException on the CLI but an
// unreduced-term expectation mismatch here), so both harnesses stay.
//
// Dedup review 2026-09-13 (criterion: truly identical construct): no merges.
// collections-index-out-of-range (bare native pos crash) != speculative-guard
// (guard evaluation semantics); datetime multiply-overflow != year-360;
// rational sign != zero-factor; the three reduction repros (bba88fc8 family)
// were all fixed and promoted to RegressionTest on 2026-09-24, and the whole
// state/race cluster (empty-set-branch-in-map-lambda, neighbor-query-changes-
// result, nondeterministic-inference) plus rule-pos-on-mapped-collection-list-
// undecided on 2026-09-25.
//
// Findings WITHOUT an .nl repro (from the 2026-09-07 review; kept here so
// they are not lost - they cannot be tested from this suite):
//   core/editor:
//   - confirmed  tools/EditorWindow.java:1286  no eval deadline: divergent rule hangs the editor at 100% CPU (interactive)
//   - unverified KnowledgeBase.java:380        child KBs inherit parent memoization: child facts cannot override memoized falsehoods
//   - confirmed  syntax/Token.java:431         caret in whitespace/comment never gets completions (needs LSP client)
//   - confirmed  tools/EditorWindow.java:1255  self/mutual imports cause an infinite refresh loop (interactive)
//   - confirmed  tools/EditorWindow.java:1258  non-ParseException during evaluation silently kills the window's eval loop (interactive)
//   - confirmed  tools/EditorWindow.java:198   every open window permanently occupies a shared ForkJoinPool worker (interactive)
//   - confirmed  tools/NelumboEvaluator.java:110 query failing with a non-mismatch error reported as expectationMatched=true
//   - unverified Node.java:622 + MatchState.java:139 rules with string-literal heads never matched; strings that do not lex as one token NPE
//   - confirmed  syntax/Token.java:439         unconditional debug println on every completion request
//   - confirmed  syntax/Token.java:244         empty tokens (EOF) contain no position: no completions/hover at EOF without trailing newline (needs LSP client)
//   - confirmed  tools/EditorWindow.java:1510  debounced auto-save races concurrent writers on the same file (interactive)
//   - confirmed  tools/Main.java:53            REPL crashes with NoSuchElementException at EOF (interactive)
//   - half       patterns/AlternationPattern.java:129 failed alternation options pollute shared previous[] token spacing
//   - half       syntax/ParseException.java:84 exception length garbage/negative when tokens span lines
//   - unverified Node.java:68, lang/Functor.java:221 unsynchronized lazy-init caches shared across concurrent evaluations
//   lsp/server:
//   - confirmed  Main.java:118                 WebSocket mode: any client's normal exit notification System.exit()s the whole multi-session server
//   - confirmed  documentService/DocumentSyncService.java:66 blank documents never registered; later edits silently ignored
//   - unverified QueryResultCache.java:134     stdio auto-eval has no deadline: divergent query hangs the eval worker permanently
//   - confirmed  documentService/DocumentSyncService.java:72 didChange to blank content ignored: stale diagnostics/hints
//   - confirmed  documentService/DocumentSyncService.java:54 didClose never clears published diagnostics; in-flight eval can re-publish
//   - confirmed  QueryResultCache.java:114     backstop timeout cannot stop a runaway evaluation; threads and pool workers leak
//   - confirmed  U.java:448                    makeSelectionRange NPEs when caret is on no token or parse has no root
//   - half       Main.java:125                 race on static Main.client can wire a session to another session's client
//   - half       Main.java:144                 WebSocket sessions never dispose their Workspace: thread + document cache leak
//   - half       workspaceService/WorkspaceExecuteCommandService.java:73 executeCommand runs synchronously on the JSON-RPC dispatch thread, no backstop
//   - half       workspaceService/WorkspaceExecuteCommandService.java:57 the exec code action sends empty arguments: feature dead end to end
//   - unverified NlDocument.java:82            parse deadline is dead code: only checked in Predicate.fixpoint, parsing unbounded
//   - confirmed  QueryResultCache.java:159     evaluate()/closeDocument race re-inserts hints for a closed document
//   - confirmed  documentService/DocumentFormattingService.java:787 placeMarkerAt emits overlapping TextEdits: client rejects the whole format
//   - half       Workspace.java:61             settings.json read/written next to the jar: crash on read-only installs
//   - unverified workspaceService/WorkspaceExecuteCommandService.java:47 unknown command ids throw IllegalArgumentException; null arguments NPE
public class KnownBugsTest extends NelumboTestBase {

    static {
        setProp("PARALLEL_COLLECTIONS", "false");
        setProp("REVERSE_NELUMBO", "false");
        setProp("RANDOM_NELUMBO", "false");
        setProp("TRACE_NELUMBO", "false");
        setProp("TRACE_SYNTATIC", "false");
        setProp("VERBOSE_TESTS", "false");
    }

    // ==== map / lambda cluster ====

    @KnownBug("arithmetic inside a set literal not evaluated (undecided; ClassCast ListImpl->Set in a map lambda)")
    @Test
    public void setLiteralArithmeticUnevaluated() {
        bugResource("set-literal-arithmetic-unevaluated.nl");
    }

    // ==== state / race cluster ====
    // the rest of this cluster (empty-set-branch-in-map-lambda,
    // neighbor-query-changes-result, nondeterministic-inference) was fixed and
    // promoted on 2026-09-25; larger-scale manifestations seen mid-session
    // (near-identical rules corrupting each other, forwarding rules flipping
    // results, an unused rule being load-bearing) reproduced only against
    // intermediate sudoku-9x9-smart.nl versions - facets of the same defects

    @KnownBug("where-filter over user Boolean rule NPEs inside deep recursion (currently masked by bba88fc8)")
    @Test
    public void whereFilterNpeInRecursion() {
        bugResource("where-filter-npe-in-recursion.nl");
    }

    // ==== speculative evaluation ====

    @KnownBug("rule guards evaluated speculatively: unguarded pos crashes out-of-range (currently masked by bba88fc8)")
    @Test
    public void speculativeGuardIndexCrash() {
        bugResource("speculative-guard-index-crash.nl");
    }

    // ==== parser / diagnostics ====

    @KnownBug("E/!E with 4 variables parses in rule bodies then crashes at runtime")
    @Test
    public void fourVarQuantifierCrash() {
        bugResource("four-var-quantifier-crash.nl");
    }

    @KnownBug("if-guard on its own continuation line silently dropped")
    @Test
    public void guardOnContinuationLineDropped() {
        bugResource("guard-on-continuation-line-dropped.nl");
    }

    @KnownBug("undeclared variable: misleading error, rule silently dropped")
    @Test
    public void undeclaredVariableDiagnostics() {
        bugResource("undeclared-variable-diagnostics.nl");
    }

    @KnownBug("// directly after operator char swallowed into operator token")
    @Test
    public void commentAfterOperator() {
        bugResource("comment-after-operator.nl");
    }

    @KnownBug("multi-line STRING token crashes checkToken with -ea")
    @Test
    public void multilineStringAssert() {
        bugResource("multiline-string-assert.nl");
    }

    // ==== patterns ====

    @KnownBug("multi-keyword alternation options lose their identity (alt flag dropped)")
    @Test
    public void alternationOptionIdentity() {
        bugResource("alternation-option-identity.nl");
    }

    @KnownBug("matched optional with multi-keyword body recorded as absent")
    @Test
    public void optionalPresenceLost() {
        bugResource("optional-presence-lost.nl");
    }

    @KnownBug("greedy separator consumption without backtracking crashes on valid input")
    @Test
    public void repetitionSeparatorGreedy() {
        bugResource("repetition-separator-greedy.nl");
    }

    // ==== logic / quantifiers / facts ====

    @KnownBug("fully-bound quantifiers turn unknown body result into definitive answer")
    @Test
    public void quantifierDefinitiveFromUnknown() {
        bugResource("quantifier-definitive-from-unknown.nl");
    }

    @KnownBug("getFacts claims complete-no-facts for un-indexed shapes like r(a,a)")
    @Test
    public void diagonalFactLookup() {
        bugResource("diagonal-fact-lookup.nl");
    }

    // ==== stdlib: numbers / strings / collections / datetime ====

    @KnownBug("normalize() keeps negative denominators: -2 > 0 inferred")
    @Test
    public void rationalSign() {
        bugResource("rational-sign.nl");
    }

    @KnownBug("zero factor builds n/0 rationals")
    @Test
    public void rationalZeroFactor() {
        bugResource("rational-zero-factor.nl");
    }

    @KnownBug("division by zero crashes the whole evaluation")
    @Test
    public void integersDivZero() {
        bugResource("integers-div-zero.nl");
    }

    @KnownBug("int(...) uses Integer.parseInt: silent 32-bit limit")
    @Test
    public void integerString32bit() {
        bugResource("integer-string-32bit.nl");
    }

    @KnownBug("indexOf crashes on out-of-range index; 2^32 wraps to element 0")
    @Test
    public void collectionsIndexOutOfRange() {
        bugResource("collections-index-out-of-range.nl");
    }

    @KnownBug("period_multiply crashes on multipliers outside int range")
    @Test
    public void datetimeMultiplyOverflow() {
        bugResource("datetime-multiply-overflow.nl");
    }

    @KnownBug("year counted as 360 days in period comparison")
    @Test
    public void datetimeYear360() {
        bugResource("datetime-year-360.nl");
    }
}
