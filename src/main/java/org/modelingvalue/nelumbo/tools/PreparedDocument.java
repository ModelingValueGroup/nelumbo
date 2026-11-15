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

import java.util.ArrayList;
import java.util.List;

import org.modelingvalue.nelumbo.Evaluatable;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboTimeoutException;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.logic.Query;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.ParserResult;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.Tokenizer.TokenizerResult;

/**
 * A parsed document whose facts and rules are already walked in document order. Every query holds its own
 * snapshot of the knowledge base at its position, so it can be evaluated (and cancelled) on its own.
 */
public record PreparedDocument(KnowledgeBase knowledgeBase, ParserResult parserResult, List<ParseException> statementErrors, List<PreparedQuery> queries) {

    public static PreparedDocument prepare(KnowledgeBase base, TokenizerResult tokenizerResult, long parseDeadlineMs) {
        KnowledgeBase doc = new KnowledgeBase(base);
        if (parseDeadlineMs > 0) {
            doc.setDeadlineNanos(System.nanoTime() + parseDeadlineMs * 1_000_000L);
        }
        return doc.get(() -> {
            Parser       parser = new Parser(tokenizerResult);
            ParserResult parsed;
            try {
                parsed = parser.parseNonThrowing();
            } catch (NelumboTimeoutException e) {
                throw e;
            } catch (RuntimeException | StackOverflowError e) {
                throw new Crash(parser.crashToken(), e);
            }
            List<ParseException> errors   = new ArrayList<>();
            List<PreparedQuery>  queries  = new ArrayList<>();
            ParserResult         throwing = new ParserResult(null, true);
            for (Node root : parsed.roots()) {
                if (root instanceof Query query) {
                    Token first = query.firstToken();
                    Token last  = query.lastToken();
                    // a query without tokens has no place in the text for its result
                    if (first != null && last != null) {
                        PreparedQuery.Span span = new PreparedQuery.Span(first.index(), last.index() + last.text().length());
                        queries.add(new PreparedQuery(query, span, new KnowledgeBase(doc)));
                    }
                } else if (root instanceof Evaluatable eval) {
                    try {
                        eval.evaluate(doc, throwing);
                    } catch (ParseException e) {
                        errors.add(e);
                    } catch (RuntimeException | StackOverflowError e) {
                        errors.add(new ParseException(e, "Internal error: " + innermost(e), root));
                    }
                }
            }
            doc.setDeadlineNanos(0);
            return new PreparedDocument(doc, parsed, errors, queries);
        });
    }

    public static PreparedDocument empty(KnowledgeBase base, TokenizerResult tokenizerResult) {
        KnowledgeBase doc = new KnowledgeBase(base);
        return new PreparedDocument(doc, new ParserResult(tokenizerResult, false), List.of(), List.of());
    }

    static String innermost(Throwable e) {
        while (e.getCause() != null) {
            e = e.getCause();
        }
        return e.toString();
    }

    /** A crash of the parser itself, at the first token of the innermost node being parsed. */
    public static final class Crash extends RuntimeException {
        private final Token crashToken;

        Crash(Token crashToken, Throwable cause) {
            super(cause);
            this.crashToken = crashToken;
        }

        public Token crashToken() {
            return crashToken;
        }
    }
}
