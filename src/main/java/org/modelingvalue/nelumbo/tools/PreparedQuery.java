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

package org.modelingvalue.nelumbo.tools;

import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboTimeoutException;
import org.modelingvalue.nelumbo.logic.InferResult;
import org.modelingvalue.nelumbo.logic.Query;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParserResult;

/** One query of a {@link PreparedDocument}, evaluated against the facts and rules above it only. */
public final class PreparedQuery {

    public record Span(int start, int end) {
    }

    public record Outcome(String result, long micros, ParseException exception, boolean timedOut, boolean cancelled) {
    }

    private final Query         query;
    private final Span          span;
    private final KnowledgeBase snapshot;

    private volatile boolean       cancelled;
    private volatile KnowledgeBase running;

    PreparedQuery(Query query, Span span, KnowledgeBase snapshot) {
        this.query    = query;
        this.span     = span;
        this.snapshot = snapshot;
    }

    public Query query() {
        return query;
    }

    public Span span() {
        return span;
    }

    public Outcome evaluate(long deadlineMs) {
        if (cancelled) {
            return new Outcome(null, -1, null, true, true);
        }
        ParseException[] exception = new ParseException[1];
        boolean[]        timedOut  = new boolean[1];
        String[]         result    = new String[1];
        snapshot.invoke(() -> {
            KnowledgeBase kb = KnowledgeBase.current();
            kb.setDeadlineNanos(deadlineMs > 0 ? System.nanoTime() + deadlineMs * 1_000_000L : 0);
            running = kb; // published after the deadline is set, so a cancel never gets overwritten
            if (cancelled) {
                kb.setDeadlineNanos(System.nanoTime());
            }
            try {
                query.evaluate(kb, new ParserResult(null, true));
            } catch (ParseException e) {
                exception[0] = e;
            } catch (NelumboTimeoutException e) {
                timedOut[0] = true;
            } catch (RuntimeException | StackOverflowError e) {
                exception[0] = new ParseException(e, "Internal error: " + PreparedDocument.innermost(e), query);
            } finally {
                running = null; // the run KB holds the memoization: do not keep it alive
            }
            InferResult ir = query.inferResult();
            result[0] = ir == null ? null : ir.toString();
        });
        return new Outcome(result[0], query.inferMicros(), exception[0], timedOut[0], cancelled);
    }

    public void cancel() {
        cancelled = true;
        KnowledgeBase kb = running;
        if (kb != null) {
            kb.setDeadlineNanos(System.nanoTime());
        }
    }
}
