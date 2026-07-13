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

package org.modelingvalue.nelumbo.tools;

import java.util.ArrayList;
import java.util.List;

import org.modelingvalue.nelumbo.Evaluatable;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboTimeoutException;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.logic.InferResult;
import org.modelingvalue.nelumbo.logic.Query;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.ParserResult;
import org.modelingvalue.nelumbo.syntax.Tokenizer;

/**
 * Parses and evaluates a self-contained .nl source and returns structured results
 * (instead of printing, like {@code NelumboCli}, or producing LSP ranges, like the
 * LSP {@code QueryEvaluator}). Line and column in diagnostics are 1-based.
 */
public final class NelumboEvaluator {

    public record Diagnostic(int line, int col, int length, String message) {
    }

    /** expectationMatched is null when the query carries no expected result. */
    public record QueryOutcome(String query, String result, Boolean expectationMatched) {
    }

    public record EvalResult(boolean ok, List<Diagnostic> diagnostics, List<QueryOutcome> queries) {
    }

    private NelumboEvaluator() {
    }

    /** deadlineMs <= 0 means no deadline. */
    public static EvalResult evaluate(String source, String name, long deadlineMs) {
        String src = source.endsWith("\n") ? source : source + "\n";
        List<Diagnostic> diagnostics = new ArrayList<>();
        List<QueryOutcome> queries = new ArrayList<>();
        KnowledgeBase evalKb = new KnowledgeBase(KnowledgeBase.BASE);
        if (deadlineMs > 0) {
            evalKb.setDeadlineNanos(System.nanoTime() + deadlineMs * 1_000_000L);
        }
        try {
            evalKb.run(() -> {
                KnowledgeBase kb = KnowledgeBase.CURRENT.get();
                ParserResult parsed = new Parser(new Tokenizer(src, name).tokenize()).parseNonThrowing();
                for (ParseException e : parsed.exceptions()) {
                    diagnostics.add(toDiagnostic(e));
                }
                ParserResult throwing = new ParserResult(null, true);
                for (Node root : parsed.roots()) {
                    if (!(root instanceof Evaluatable eval)) {
                        continue;
                    }
                    try {
                        eval.evaluate(kb, throwing);
                        if (eval instanceof Query query) {
                            queries.add(toOutcome(query, true));
                        }
                    } catch (NelumboTimeoutException tex) {
                        diagnostics.add(deadlineDiagnostic(deadlineMs));
                        break;
                    } catch (ParseException exc) {
                        diagnostics.add(toDiagnostic(exc));
                        if (eval instanceof Query query) {
                            queries.add(toOutcome(query, !isMismatch(exc)));
                        } else {
                            break; // a fact/rule failed to evaluate: later results can't be trusted
                        }
                    }
                }
            });
        } catch (NelumboTimeoutException tex) {
            diagnostics.add(deadlineDiagnostic(deadlineMs));
        }
        return new EvalResult(diagnostics.isEmpty(), diagnostics, queries);
    }

    private static boolean isMismatch(ParseException exc) {
        String msg = exc.getShortMessage();
        return msg != null && msg.startsWith("Expected result ");
    }

    private static Diagnostic deadlineDiagnostic(long deadlineMs) {
        return new Diagnostic(1, 1, 1, "evaluation exceeded the deadline of " + deadlineMs + " ms");
    }

    private static Diagnostic toDiagnostic(ParseException e) {
        return new Diagnostic(e.line() + 1, e.position() + 1, Math.max(e.length(), 1), e.getShortMessage());
    }

    private static QueryOutcome toOutcome(Query query, boolean matched) {
        StringBuffer sb = new StringBuffer();
        query.predicate().deparse(sb);
        InferResult ir = query.inferResult();
        Boolean expectation = query.hasExpected() ? matched : null;
        return new QueryOutcome(sb.toString().trim(), ir == null ? null : ir.toString(), expectation);
    }
}
