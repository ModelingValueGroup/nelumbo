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

package org.modelingvalue.nelumbo.browser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator;
import org.teavm.jso.JSExport;

/**
 * Entry point of the TeaVM browser build: exports {@link #evaluateNl(String)} to JavaScript.
 */
public final class NelumboBrowser {

    private static final long DEADLINE_MS = 10_000;

    private static boolean initialized;

    private NelumboBrowser() {
    }

    /**
     * Evaluates a self-contained .nl source and returns a JSON string:
     * {@code {"ok":bool,"diagnostics":[{"line","col","length","message"}],
     * "queries":[{"query","result","expectationMatched","facts","falsehoods"}]}}.
     * Never throws into JS: internal errors come back as a diagnostic.
     */
    @JSExport
    public static String evaluateNl(String source) {
        try {
            init();
            NelumboEvaluator.EvalResult result = NelumboEvaluator.evaluate(source, "<browser>", DEADLINE_MS);
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("ok", result.ok());
            List<Object> diagnostics = new ArrayList<>();
            for (NelumboEvaluator.Diagnostic d : result.diagnostics()) {
                Map<String, Object> dj = new LinkedHashMap<>();
                dj.put("line", d.line());
                dj.put("col", d.col());
                dj.put("length", d.length());
                dj.put("message", d.message());
                diagnostics.add(dj);
            }
            json.put("diagnostics", diagnostics);
            List<Object> queries = new ArrayList<>();
            for (NelumboEvaluator.QueryOutcome q : result.queries()) {
                Map<String, Object> qj = new LinkedHashMap<>();
                qj.put("query", q.query());
                qj.put("result", q.result());
                qj.put("expectationMatched", q.expectationMatched());
                qj.put("facts", q.facts());
                qj.put("falsehoods", q.falsehoods());
                queries.add(qj);
            }
            json.put("queries", queries);
            return Json.write(json);
        } catch (Throwable t) {
            return Json.write(Map.of(
                    "ok", false,
                    "diagnostics", List.of(Map.of(
                            "line", 1, "col", 1, "length", 1,
                            "message", "internal error: " + t))));
        }
    }

    private static void init() {
        if (!initialized) {
            ReflectionKeep.link();
            KnowledgeBase.registerResolver(new EmbeddedStdlibResolver());
            initialized = true;
        }
    }

    public static void main(String[] args) {
        // no-op: the exported evaluateNl is the API; TeaVM needs a main class to link from
    }
}
