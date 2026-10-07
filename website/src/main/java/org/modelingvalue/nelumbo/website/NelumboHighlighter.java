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

package org.modelingvalue.nelumbo.website;

import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;
import org.modelingvalue.nelumbo.syntax.Tokenizer;
import org.modelingvalue.nelumbo.syntax.Tokenizer.TokenizerResult;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator;

/**
 * Colors a Nelumbo snippet for the docs the way the editors do: the snippet is parsed against a knowledge base with
 * every shipped module imported, and each token gets a span for its {@link Token#colorType()}. Parse errors are
 * ignored - tokens the parser did not reach keep their plain token color - since docs snippets are often fragments.
 */
final class NelumboHighlighter {

    private static final String IMPORTS = """
            import nelumbo.logic
            import nelumbo.integers
            import nelumbo.strings
            import nelumbo.collections
            import nelumbo.rationals
            import nelumbo.datetime
            """;

    /** Bounds each snippet's parse, so one pathological block cannot stall the server's startup. */
    private static final long DEADLINE_MS = 5_000;

    private static NelumboHighlighter shared;

    private final KnowledgeBase base;

    private NelumboHighlighter() {
        base = NelumboEvaluator.evaluate(KnowledgeBase.BASE, IMPORTS, "<docs>", 0).kb();
    }

    /** One instance per JVM: loading the modules is the expensive part. */
    static synchronized NelumboHighlighter shared() {
        if (shared == null) {
            shared = new NelumboHighlighter();
        }
        return shared;
    }

    /**
     * {@code code} as escaped html with {@code <span class="nl-...">} around every colored token, or just escaped when
     * the engine cannot handle the snippet (it then never takes the docs down with it).
     */
    String highlight(String code) {
        try {
            return colored(code);
        } catch (RuntimeException | StackOverflowError e) {
            System.err.println("docs: cannot highlight a nelumbo block, showing it plain: " + e);
            return escape(code);
        }
    }

    private String colored(String code) {
        // a throwaway child per snippet, so declarations in one block never color another
        KnowledgeBase kb = new KnowledgeBase(base);
        kb.setDeadlineNanos(System.nanoTime() + DEADLINE_MS * 1_000_000L);
        return kb.get(() -> {
            TokenizerResult tokens = new Tokenizer(code, "<docs>").tokenize();
            new Parser(tokens).parseNonThrowing();
            StringBuilder sb = new StringBuilder();
            for (Token token : tokens.listAll()) {
                String text = escape(token.text());
                String css = cssClass(token.colorType());
                sb.append(css == null || text.isEmpty() ? text : "<span class=\"" + css + "\">" + text + "</span>");
            }
            return sb.toString();
        });
    }

    private static String cssClass(TokenType type) {
        return switch (type) {
            case KEYWORD -> "nl-keyword";
            case TYPE -> "nl-type";
            case VARIABLE -> "nl-variable";
            case NAME -> "nl-name";
            case STRING -> "nl-string";
            case NUMBER -> "nl-number";
            case OPERATOR -> "nl-operator";
            case META_OPERATOR -> "nl-meta";
            case END_LINE_COMMENT, IN_LINE_COMMENT -> "nl-comment";
            default -> null;
        };
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
