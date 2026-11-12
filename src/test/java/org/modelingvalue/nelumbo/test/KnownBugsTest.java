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
//     Wim Bast, Tom Brus, Victor Lap                                                                                  ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.nelumbo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboConstants;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator.EvalResult;

// The known-bug (xfail) suite. Every method runs a red-by-design .nl repro
// from src/main/resources/org/modelingvalue/nelumbo/bugs/ whose query
// expectations state the CORRECT behavior. While the bug exists the method is
// ABORTED (grey); once fixed it FAILS with "KNOWN BUG APPEARS FIXED" - then
// remove the @KnownBug and promote the method to RegressionTest (twenty-two
// promoted so far: two on 2026-09-11, three on 2026-09-24, four on 2026-09-25,
// one on 2026-10-02, five on 2026-10-06, on 2026-10-18 one merged from
// three unbound-lookup repros whose remaining expectations were wrong: a
// variable is never bound by enumerating a type, and on 2026-10-26 one merged
// from the three crash repros integers-div-zero, collections-index-out-of-range
// and datetime-multiply-overflow: an exception in a built-in operation leaves
// the query unknown, an error without a result; also on 2026-10-26
// speculative-guard-index-crash (an exception next to a false operand is
// irrelevant), and
// undeclared-variable-diagnostics, whose expectation was wrong: an undeclared
// name is not a variable, so the rule is not well-formed and the query unknown,
// and guard-on-continuation-line-dropped, which is no bug: a NEWLINE after a
// non-operator ends the statement, so a guard on its own line is a parse error
// and the guardless alternative gives a conflict; and on 2026-10-27
// decided-conjunction-hides-later-result: a shallow unknown is no longer
// remembered, and inconsistency-missed-by-rule-order: the consistency check
// between rules is symmetric).
//
// The CLI twin is ./run-all-tests-with-CLI (repo root): the SAME bug can
// manifest differently on the CLI vs this test JVM (proven 2026-09-13: the
// bba88fc8 reduction bug is a ClassCastException on the CLI but an
// unreduced-term expectation mismatch here), so both harnesses stay.
//
// Dedup review 2026-09-13 (criterion: truly identical construct): no merges.
// collections-index-out-of-range (bare native index crash) != speculative-guard
// (guard evaluation semantics); datetime multiply-overflow != year-360 (the
// two crash repros were promoted on 2026-10-26); rational sign != zero-factor; the three reduction repros (bba88fc8 family)
// were all fixed and promoted to RegressionTest on 2026-09-24, and the whole
// state/race cluster (empty-set-branch-in-map-lambda, neighbor-query-changes-
// result, nondeterministic-inference) plus rule-pos-on-mapped-collection-list-
// undecided on 2026-09-25, the CLI-only load-time repro nested-generic-
// rule-load-time on 2026-10-02, and the map/lambda repro set-literal-
// arithmetic-unevaluated on 2026-10-06 (the list-literal twin `[j+1]`, which
// did not parse before, is covered by the promoted file too). Also promoted on
// 2026-10-06: the parser repro four-var-quantifier-crash (logic.nl now has
// Lambda4..6, so E/A/lambdas take up to 6 variables in every position) and
// the pattern repro repetition-separator-greedy (the separator backtracking
// was in since eeb06747; the repro only stayed red because its `:: Object`
// functor is Function-typed since bba88fc8 - its functor is a `struct` now),
// and its sibling optional-presence-lost (OptionalPattern.args keeps a matched
// argument-less body as present; also a `struct` in the promoted file) and
// alternation-option-identity (the keep flag is passed through
// SequencePattern.args now; `struct` too) - no pattern repro is left.
// Promoted on 2026-11-11, when the `struct` functor modifier replaced user
// `:: Literal` types: literal-type-in-intersection-hole and
// literal-binding-expectation-unchecked.
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
//   - confirmed  logic/Rule.java:84            (found 2026-10-06) the CLI crashes on tests/langOnly.nl with a ClassCastException
//                                              (ListImpl -> Predicate) in Rule.consequence, reached from Rule.toString via
//                                              NodeTypePattern.string; CLI-only: the JUnit NelumboTest.langOnly passes
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
        setProp("TRACE_SYNTATIC", "false");
        setProp("VERBOSE_TESTS", "false");
    }

    @Override
    protected void settings(KnowledgeBase kb) {
        kb.setReverseOrder(env("REVERSE_NELUMBO", false));
        kb.setRandomOrder(env("RANDOM_NELUMBO", true));
        kb.setTrace(env("TRACE_NELUMBO", false));
    }

    // ==== parser / diagnostics ====

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

    @KnownBug("single-hole functor (Cell ::= <Integer>) makes the parser NPE on every later Integer expression")
    @Test
    public void singleHoleFunctorParserNpe() {
        bugResource("single-hole-functor-parser-npe.nl");
    }

    // the correct outcome is a parse error, which bugResource (a clean run) cannot express
    @KnownBug("unquoted = or != in a functor alternative after a line break overflows the parser stack instead of a parse error")
    @Test
    public void continuationLineEqualsPatternOverflow() throws IOException {
        String     name   = "continuation-line-equals-pattern-overflow.nl";
        String     source = new String(
                KnownBugsTest.class.getResourceAsStream(NelumboConstants.NELUMBO_BUGS + name).readAllBytes());
        EvalResult result = evaluate(source, name, 60_000);
        assertEquals(1, result.diagnostics().size(), "diagnostics: " + result.diagnostics());
        assertTrue(result.diagnostics().get(0).message().startsWith("Unexpected token '<'"), "diagnostics: " + result.diagnostics());
    }

    // ==== logic / quantifiers / facts ====

    @KnownBug("pattern whose only argument is a unique hole (winner(<one Person>)) is never closed")
    @Test
    public void singleUniqueHoleNotClosed() {
        bugResource("single-unique-hole-not-closed.nl");
    }

    @KnownBug("variable nested in a fact argument (owns(Piet,box(n))) is not bound by the lookup")
    @Test
    public void nestedArgumentVariableNotNavigated() {
        bugResource("nested-argument-variable-not-navigated.nl");
    }

    @KnownBug("conditional expression (c then x else y) whose condition and branch share a variable NPEs")
    @Test
    public void thenElseSharedVariableNpe() {
        bugResource("then-else-shared-variable-npe.nl");
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

    @KnownBug("int(...) uses Integer.parseInt: silent 32-bit limit")
    @Test
    public void integerString32bit() {
        bugResource("integer-string-32bit.nl");
    }

    // the correct behavior is an error per query, which bugResource (pass = no
    // error) cannot express: every query must have an error and no result
    @KnownBug("list index outside the int range wraps to its low 32 bits instead of an index error")
    @Test
    public void collectionsIndexWrapsPastIntRange() throws IOException {
        String name = "collections-index-wraps-past-int-range.nl";
        String source = new String(
                KnownBugsTest.class.getResourceAsStream(NelumboConstants.NELUMBO_BUGS + name).readAllBytes());
        EvalResult result = evaluate(source, name, 60_000);
        long errors = result.diagnostics().size();
        List<String> wrapped = result.queries().stream().filter(q -> q.result() != null)
                .map(q -> q.query() + " ? " + q.result()).toList();
        assertEquals(List.of(), wrapped, "queries answered from a wrapped index");
        assertEquals(result.queries().size(), errors, "diagnostics: " + result.diagnostics());
    }

    @KnownBug("set-builder compared with a closed set literal holds for any set")
    @Test
    public void setBuilderEqualsAnySet() {
        bugResource("set-builder-equals-any-set.nl");
    }

    @KnownBug("year counted as 360 days in period comparison")
    @Test
    public void datetimeYear360() {
        bugResource("datetime-year-360.nl");
    }

    @KnownBug("0-arity function (lst == [1,2,3]) passed as an argument crashes a native with argument type mismatch")
    @Test
    public void zeroArityFunctionArgumentCrash() {
        bugResource("zero-arity-function-argument-crash.nl");
    }

    @KnownBug("function query with its result given (f(x) = [..] ? [()][]) diverges where f(x)=s ? answers in a second")
    @Test
    public void givenResultQueryDiverges() {
        bugResource("given-result-query-diverges.nl");
    }

    @KnownBug("function query whose given result is a call (g(1) = h(1), |l| = |m|) crashes with argument type mismatch: the call reaches a native unevaluated")
    @Test
    public void givenResultCallCrash() {
        bugResource("given-result-call-crash.nl");
    }

    @KnownBug("list of lists with [] next to a list holding an expression ([[],[1+2]]) does not parse")
    @Test
    public void emptyListBesideExpressionList() {
        bugResource("empty-list-beside-expression-list.nl");
    }

    @KnownBug("empty list or set ([] or {}) as an element of a collection makes equality fail ([[],[2]] = ll is [][..])")
    @Test
    public void emptyListAndSetInListErrors() {
        bugResource("empty-list-and-set-in-list-errors.nl");
    }

    @KnownBug("empty set or list literal as a value of a function rule (f(i) == {}) does not parse")
    @Test
    public void emptyCollectionFunctionRuleValue() {
        bugResource("empty-collection-function-rule-value.nl");
    }

    @KnownBug("date-time value with a time cannot be written bare in an expected result ([(2024-01-15T11:30)] does not parse)")
    @Test
    public void datetimeBareBindingValue() {
        bugResource("datetime-bare-binding-value.nl");
    }

    @KnownBug("pattern starting with a repetition of holes (<(> <Person> <,> , <)+> are friends) accepts only one element")
    @Test
    public void leadingRepetitionPattern() {
        bugResource("leading-repetition-pattern.nl");
    }

    @KnownBug("constant starting with a type parameter's name (Tom with Type T) in a <T> hole parses as a variable declaration (T om)")
    @Test
    public void typeVariableNamePrefixSplit() {
        bugResource("type-variable-name-prefix-split.nl");
    }

    @KnownBug("functor without holes (Thing ::= n) in a transformation body makes the closing } of that body fail to parse")
    @Test
    public void holelessFunctorInTransformation() {
        bugResource("holeless-functor-in-transformation.nl");
    }

    @KnownBug("rule head variable of a narrower type (Female f) yields bindings of the wrong type for an open query (Who is a sister of Piet lists a Male)")
    @Test
    public void typedHeadVariableOpenQuery() {
        bugResource("typed-head-variable-open-query.nl");
    }

    @KnownBug("recursive relation whose if-guards compare two head variables (ok(n,d) <=> true if d>=n, ok(n,d+1) if d<n) does not terminate")
    @Test
    public void relationGuardOnTwoHeadVariablesHangs() {
        bugResource("relation-guard-on-two-head-variables-hangs.nl");
    }

    @KnownBug("intersection of an open and a closed set is closed although an element may be in the open set ({1,..} && {2} is {} instead of {..})")
    @Test
    public void openSetIntersectionClosed() {
        bugResource("open-set-intersection-closed.nl");
    }

    @KnownBug("= between a collection literal with a computed element and an equal literal is false ({1+1} = {2} gives [][()])")
    @Test
    public void computedCollectionElementEquality() {
        bugResource("computed-collection-element-equality.nl");
    }
}
