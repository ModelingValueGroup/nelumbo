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

package org.modelingvalue.nelumbo.lsp;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Evaluatable;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboTimeoutException;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.logic.InferResult;
import org.modelingvalue.nelumbo.logic.Query;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.ParserResult;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.Tokenizer;

/**
 * Evaluates every {@code Query} in a document against a given {@code KnowledgeBase}, so the
 * user-declared types, patterns, facts and rules are all registered in the same inference KB
 * (see {@code QueryExecutionFlowTest}). Shared by the inline inlay hints and the "Query" code lens
 * popup so both render identical results.
 */
public final class QueryEvaluator {

    private QueryEvaluator() {
    }

    /** @return result per query, in document order. Queries that could not be reached are absent. */
    public static Map<Query, QueryResult> evaluate(String content, String uri) {
        return evaluate(KnowledgeBase.BASE, 0, content, uri);
    }

    /**
     * Same, but declarations are resolved against {@code base} (a loaded KB for embedded servers) and, when
     * {@code deadlineMs > 0}, inference self-aborts past the deadline. On timeout, queries already evaluated keep
     * their results; the first unreached query gets an ERROR result; remaining queries are absent from the map.
     */
    public static Map<Query, QueryResult> evaluate(KnowledgeBase base, long deadlineMs, String content, String uri) {
        Map<Query, QueryResult> results = new LinkedHashMap<>();
        KnowledgeBase           evalKb  = new KnowledgeBase(base);
        if (deadlineMs > 0) {
            evalKb.setDeadlineNanos(System.nanoTime() + deadlineMs * 1_000_000L);
        }
        try {
            evalKb.run(() -> {
                KnowledgeBase knowledgeBase = KnowledgeBase.CURRENT.get();
                ParserResult  parsed        = new Parser(new Tokenizer(content, uri).tokenize()).parseNonThrowing();
                ParserResult  throwing      = new ParserResult(null, true);
                for (Node root : parsed.roots()) {
                    if (!(root instanceof Evaluatable eval)) {
                        continue;
                    }
                    try {
                        eval.evaluate(knowledgeBase, throwing);
                        if (eval instanceof Query query) {
                            InferResult ir = query.inferResult();
                            if (ir == null) {
                                results.put(query, QueryResult.error("Infer resulted in nothing"));
                            } else if (query.hasExpected()) {
                                results.put(query, QueryResult.match(ir.toString()));
                            } else {
                                results.put(query, QueryResult.result(ir.toString()));
                            }
                        }
                    } catch (NelumboTimeoutException tex) {
                        if (eval instanceof Query query) {
                            results.put(query, QueryResult.error("evaluation exceeded the deadline"));
                        }
                        break;
                    } catch (ParseException exc) {
                        if (eval instanceof Query query) {
                            results.put(query, toResult(query, exc));
                        } else {
                            // a fact/rule failed to evaluate: later queries can't be trusted, stop here.
                            System.err.println("query evaluation aborted at " + eval.getClass().getSimpleName() + ": " + exc.getMessage());
                            break;
                        }
                    }
                }
            });
        } catch (NelumboTimeoutException ignored) {
            // partial results already in the map; return them as-is
        }
        return results;
    }

    private static QueryResult toResult(Query query, ParseException exc) {
        String shortMsg = exc.getShortMessage();
        if (shortMsg != null && shortMsg.startsWith("Expected result ")) {
            InferResult ir       = query.inferResult(); // the calculated result; set before the mismatch is raised
            String      inferred = ir == null ? "" : ir.toString();
            return QueryResult.mismatch(inferred, shortMsg, expectedRange(query));
        }
        return QueryResult.error("Problem executing " + query.getClass().getSimpleName() + ": " + exc.getMessage());
    }

    /** Source span of the query's expected clause (everything after the {@code ?}), to be underlined on a mismatch. */
    private static Range expectedRange(Query query) {
        org.modelingvalue.collections.List<AstElement> els = query.astElements();
        if (els.size() <= 2) {
            return null;
        }
        Token from = els.get(2).firstToken();
        Token to   = query.lastToken();
        if (from == null || to == null) {
            return null;
        }
        return new Range(new Position(from.line(), from.position()), new Position(to.lastLine(), to.positionEnd()));
    }
}
