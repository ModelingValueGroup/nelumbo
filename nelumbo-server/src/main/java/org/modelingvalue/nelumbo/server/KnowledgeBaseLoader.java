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

package org.modelingvalue.nelumbo.server;

import java.util.ArrayList;
import java.util.List;

import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.ParserResult;
import org.modelingvalue.nelumbo.syntax.Tokenizer;

/**
 * Builds the server's base {@link KnowledgeBase} from a set of {@code .nl} sources. Each source is parsed and evaluated
 * into a fresh child of {@link KnowledgeBase#BASE}, so all their declarations accumulate. Any parse/evaluation error
 * aborts loading with a {@link LoadException} listing every problem.
 */
public final class KnowledgeBaseLoader {

    private KnowledgeBaseLoader() {
    }

    /** Raised when one or more sources fail to parse or evaluate. The message lists every {@code file:line:col} problem. */
    public static final class LoadException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public LoadException(String message) {
            super(message);
        }
    }

    public static KnowledgeBase load(List<NamedSource> sources) {
        List<String> problems = new ArrayList<>();
        // run(...) creates a fresh child of BASE, runs the body with that child as CURRENT (so all
        // declarations/imports register into it), and returns that populated child.
        KnowledgeBase base = KnowledgeBase.BASE.run(() -> {
            for (NamedSource source : sources) {
                String src = source.content().endsWith("\n") ? source.content() : source.content() + "\n";
                ParserResult result = new Parser(new Tokenizer(src, source.name()).tokenize()).parseNonThrowing();
                try {
                    result.evaluate();
                } catch (ParseException e) {
                    problems.add(format(source.name(), e));
                }
                for (ParseException e : result.exceptions()) {
                    problems.add(format(source.name(), e));
                }
            }
        });
        if (!problems.isEmpty()) {
            throw new LoadException("Failed to load knowledge base:\n  " + String.join("\n  ", problems));
        }
        return base;
    }

    private static String format(String name, ParseException e) {
        return name + ":" + (e.line() + 1) + ":" + (e.position() + 1) + ": " + e.getShortMessage();
    }
}
