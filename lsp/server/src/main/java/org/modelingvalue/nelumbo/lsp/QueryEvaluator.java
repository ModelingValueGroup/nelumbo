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

package org.modelingvalue.nelumbo.lsp;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboTimeoutException;
import org.modelingvalue.nelumbo.logic.Query;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.Tokenizer;
import org.modelingvalue.nelumbo.tools.PreparedDocument;
import org.modelingvalue.nelumbo.tools.PreparedQuery;

/**
 * Evaluates every {@code Query} in a document one after the other, each against
 * the facts and rules above it (see {@code QueryExecutionFlowTest}): a
 * sequential wrapper on {@link PreparedDocument} for the "Query" code lens
 * popup and the tests. {@link #toResult} maps one query's outcome, also for the
 * inlay hints of {@code QueryResultCache}, so both render identical results.
 */
public final class QueryEvaluator {

    private QueryEvaluator() {
    }

    /**
     * @return the result of every query, in document order.
     */
    public static Map<Query, QueryResult> evaluate(String content, String uri) {
        return evaluate(KnowledgeBase.BASE, 0, content, uri);
    }

    /**
     * Same, but declarations are resolved against {@code base} (a loaded KB for
     * embedded servers) and, when {@code deadlineMs > 0}, each query's inference
     * stops past the deadline, counted per query: that query gets an ERROR
     * result, the others go on. A timeout while preparing gives an empty map.
     */
    public static Map<Query, QueryResult> evaluate(KnowledgeBase base, long deadlineMs, String content, String uri) {
        return evaluate(base, deadlineMs, content, uri, false);
    }

    /**
     * Same, where {@code overloadBudget} means {@code deadlineMs} is the short budget of a busy server: a query past
     * it gets an OVERLOADED result instead of an ERROR, and a timeout while preparing is rethrown instead of returning
     * empty results, so the caller can still report the overload. The budget counts per query from when it gets an
     * inference pool worker, not from when it is queued: a busy server is one whose pool is full, and the wait for a
     * worker is not the query's own cost.
     */
    public static Map<Query, QueryResult> evaluate(KnowledgeBase base, long deadlineMs, String content, String uri, boolean overloadBudget) {
        return evaluate(base, deadlineMs, content, uri, overloadBudget, error -> {
        });
    }

    /**
     * Same, where {@code statementErrors} receives the error of a fact or rule that failed to evaluate (for example
     * an inconsistent fact). Evaluation goes on past it: the queries after it still get their results.
     */
    public static Map<Query, QueryResult> evaluate(KnowledgeBase base, long deadlineMs, String content, String uri, boolean overloadBudget,
            Consumer<ParseException> statementErrors) {
        Map<Query, QueryResult> results = new QueryResults();
        PreparedDocument        prepared;
        try {
            prepared = PreparedDocument.prepare(base, new Tokenizer(content, uri).tokenize(), deadlineMs);
        } catch (NelumboTimeoutException tex) {
            if (overloadBudget) {
                throw tex;
            }
            return results;
        }
        prepared.statementErrors().forEach(statementErrors);
        for (PreparedQuery query : prepared.queries()) {
            results.put(query.query(), toResult(query, query.evaluate(deadlineMs), overloadBudget, deadlineMs));
        }
        return results;
    }

    public static QueryResult toResult(PreparedQuery query, PreparedQuery.Outcome outcome, boolean overloadBudget, long deadlineMs) {
        if (outcome.timedOut()) {
            return overloadBudget ? QueryResult.overloaded(deadlineMs) : QueryResult.timeout(deadlineMs);
        }
        ParseException exc = outcome.exception();
        if (exc != null) {
            String shortMsg = exc.getShortMessage();
            if (shortMsg != null && shortMsg.startsWith("Expected result ")) {
                return QueryResult.mismatch(outcome.result() == null ? "" : outcome.result(), shortMsg, expectedRange(query.query()), outcome.micros());
            }
            String message = "Problem executing " + query.query().getClass().getSimpleName() + ": " + shortMsg;
            // an internal error is the engine's, anything else (an inconsistency, an error in the model) the model's
            return shortMsg != null && shortMsg.startsWith("Internal error: ") ? QueryResult.error(message, outcome.micros()) : QueryResult.failed(message, outcome.micros());
        }
        if (outcome.result() == null) {
            return QueryResult.error("Infer resulted in nothing", outcome.micros());
        }
        return query.query().hasExpected() ? QueryResult.match(outcome.result(), outcome.micros()) : QueryResult.result(outcome.result(), outcome.micros());
    }

    /**
     * The result of every query in document order, one entry per query in the source. A query equals another with
     * the same text ({@code Node} equality is structural), so a plain map gave two equal queries one entry: the
     * second's result shown on the first, nothing on the second, although a rule between them can change the answer.
     * Each query is put once.
     */
    private static final class QueryResults extends AbstractMap<Query, QueryResult> {
        private final List<Map.Entry<Query, QueryResult>> entries = new ArrayList<>();

        @Override
        public QueryResult put(Query query, QueryResult result) {
            entries.add(new SimpleImmutableEntry<>(query, result));
            return null;
        }

        @Override
        public Set<Map.Entry<Query, QueryResult>> entrySet() {
            return new AbstractSet<>() {
                @Override
                public Iterator<Map.Entry<Query, QueryResult>> iterator() {
                    return entries.iterator();
                }

                @Override
                public int size() {
                    return entries.size();
                }
            };
        }
    }

    /**
     * Source span of the query's expected clause (everything after the {@code ?}),
     * to be underlined on a mismatch.
     */
    static Range expectedRange(Query query) {
        org.modelingvalue.collections.List<AstElement> els = query.astElements();
        if (els.size() <= 2) {
            return null;
        }
        Token from = els.get(2).firstToken();
        Token to = query.lastToken();
        if (from == null || to == null) {
            return null;
        }
        return new Range(new Position(from.line(), from.position()), new Position(to.lastLine(), to.positionEnd()));
    }
}
