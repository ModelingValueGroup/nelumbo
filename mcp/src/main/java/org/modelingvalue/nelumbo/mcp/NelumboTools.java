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

package org.modelingvalue.nelumbo.mcp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.modelingvalue.nelumbo.mcp.Hints.Hint;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator.Diagnostic;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator.EvalResult;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator.QueryOutcome;

/**
 * The four MCP tool handlers, free of any MCP SDK types: each returns a JSON-ready
 * Map. Main serializes them and owns all protocol concerns.
 */
public final class NelumboTools {

    private static final int CASCADE_THRESHOLD = 3;

    private final long      deadlineMs;
    private final DocSearch docSearch;

    public NelumboTools(long deadlineMs) {
        this(deadlineMs, new DocSearch());
    }

    NelumboTools(long deadlineMs, DocSearch docSearch) {
        this.deadlineMs = deadlineMs;
        this.docSearch  = docSearch;
    }

    public Map<String, Object> evalNl(String content, String name) {
        if (content == null) {
            throw new IllegalArgumentException("eval_nl requires a 'content' argument");
        }
        EvalResult result   = NelumboEvaluator.evaluate(content, name == null || name.isBlank() ? "model.nl" : name, deadlineMs);
        String[]   lines    = content.split("\n", -1);
        List<Map<String, Object>> diagnostics = new ArrayList<>();
        for (Diagnostic d : result.diagnostics()) {
            diagnostics.add(toDiagnosticMap(d, lines));
        }
        if (diagnostics.size() > CASCADE_THRESHOLD) {
            Map<String, Object> first = diagnostics.get(0);
            String              note  = "Note: " + (diagnostics.size() - 1) + " further errors follow; they usually cascade from this first one - fix it and re-evaluate.";
            first.merge("hint", note, (old, n) -> old + " " + n);
        }
        List<Map<String, Object>> queries = new ArrayList<>();
        for (QueryOutcome q : result.queries()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("query", q.query());
            m.put("result", q.result());
            if (q.expectationMatched() != null) {
                m.put("expectationMatched", q.expectationMatched());
            }
            queries.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", result.ok());
        out.put("diagnostics", diagnostics);
        out.put("queryResults", queries);
        return out;
    }

    private static Map<String, Object> toDiagnosticMap(Diagnostic d, String[] lines) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("line", d.line());
        m.put("col", d.col());
        m.put("message", d.message());
        if (d.line() >= 1 && d.line() <= lines.length) {
            String sourceLine = lines[d.line() - 1];
            m.put("sourceLine", sourceLine);
            int col = Math.min(Math.max(d.col(), 1), sourceLine.length() + 1);
            m.put("caret", " ".repeat(col - 1) + "^".repeat(Math.max(d.length(), 1)));
        }
        Hint hint = Hints.hintFor(d.message());
        if (hint != null) {
            m.put("hint", hint.text());
            m.put("docRef", hint.docRef());
        }
        return m;
    }

    public Map<String, Object> searchDocs(String query) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (DocSearch.Match match : docSearch.search(query == null ? "" : query)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("doc", match.doc());
            m.put("heading", match.heading());
            m.put("snippet", match.snippet());
            results.add(m);
        }
        return Map.of("results", results);
    }

    public Map<String, Object> getExample(String name) {
        if (name == null || name.isBlank()) {
            List<Map<String, Object>> examples = new ArrayList<>();
            for (ExampleCatalog.Example e : ExampleCatalog.list()) {
                examples.add(Map.of("name", e.name(), "description", e.description()));
            }
            return Map.of("examples", examples);
        }
        String content = ExampleCatalog.content(name);
        if (content == null) {
            List<String> names = ExampleCatalog.list().stream().map(ExampleCatalog.Example::name).toList();
            return Map.of("error", "unknown example '" + name + "'", "available", names);
        }
        return Map.of("name", name, "content", content);
    }

    public Map<String, Object> newModel(String title) {
        return Map.of("content", ModelSkeleton.skeleton(title));
    }
}
