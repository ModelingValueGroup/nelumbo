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

package org.modelingvalue.nelumbo.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.modelingvalue.json.Json;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator.QueryOutcome;

/**
 * The completeness of a query result reaches every NelumboCli output: the text
 * outputs show it as {@code ..}, the JSON outputs as {@code complete}. Without it
 * an empty facts/falsehoods list could mean "none" or "unknown".
 */
class NelumboCliTest {

    /** One query per combination of the two completeness flags. */
    static final String COMPLETENESS = """
            import  nelumbo.integers

            Integer ::= fib(<Integer>)

            Integer n, f, a

            fib(n)=f <=>  f=n                 if n>=0 & n<=1,
                          f=fib(n-1)+fib(n-2) if n>1

            1+1=2       ? [()][]
            fib(5)=f    ? [(f=5)][..]
            !(fib(5)=f) ? [..][(f=5)]
            a>3 ?
            """;

    // {facts, falsehoods} complete per query of COMPLETENESS
    private static final boolean[][] EXPECTED = { { true, true }, { true, false }, { false, true }, { false, false } };

    @Test
    void jsonOutputCarriesTheCompletenessFlags() {
        NelumboCli.JsonOutput json = new NelumboCli.JsonOutput();
        assertTrue(NelumboCli.runSource(COMPLETENESS, "complete.nl", false, json, null), () -> "errors: " + json.errors);
        assertCompleteness(json.queries);
        // and they survive the serialization --json prints
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> parsed = (List<Map<String, Object>>) Json.fromJson(Json.toJson(json.queries));
        assertCompleteness(parsed);
    }

    @Test
    void windowJsonTabCarriesTheCompletenessFlags() {
        Map<String, Object> object = NelumboCli.jsonObject(NelumboEvaluator.evaluate(COMPLETENESS, "complete.nl", 0), "complete.nl");
        assertEquals(List.of(), object.get("errors"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> queries = (List<Map<String, Object>>) object.get("queries");
        assertCompleteness(queries);
    }

    @Test
    void queryWithoutResultHasNoCompleteness() {
        Map<String, Object> entry = NelumboCli.queryEntry(new QueryOutcome("q", null, null, List.of(), List.of(), null, null));
        Map<String, Object> complete = new java.util.LinkedHashMap<>();
        complete.put("facts", null);
        complete.put("falsehoods", null);
        assertEquals(complete, entry.get("complete"));
        assertTrue(Json.toJson(entry).contains("\"complete\":{\"facts\":null,\"falsehoods\":null}"), () -> Json.toJson(entry));
    }

    @Test
    void textOutputShowsTheCompleteness() {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        boolean ok;
        try {
            System.setOut(new PrintStream(bytes, true, StandardCharsets.UTF_8));
            ok = NelumboCli.runSource(COMPLETENESS, "complete.nl", false, null, null);
        } finally {
            System.setOut(original);
        }
        assertTrue(ok);
        assertCompletenessText(bytes.toString(StandardCharsets.UTF_8));
    }

    @Test
    void windowTextTabShowsTheCompleteness() {
        assertCompletenessText(NelumboCli.renderText(NelumboEvaluator.evaluate(COMPLETENESS, "complete.nl", 0)));
    }

    @SuppressWarnings("unchecked")
    private static void assertCompleteness(List<Map<String, Object>> queries) {
        assertEquals(EXPECTED.length, queries.size(), () -> "queries: " + queries);
        for (int i = 0; i < EXPECTED.length; i++) {
            Map<String, Object> complete = (Map<String, Object>) queries.get(i).get("complete");
            assertEquals(Map.of("facts", EXPECTED[i][0], "falsehoods", EXPECTED[i][1]), complete, queries.get(i).toString());
        }
        // an empty list is "none" only when complete: !(fib(5)=f) has no facts, but they are not complete
        assertEquals(List.of(), queries.get(2).get("facts"));
        assertEquals(List.of(Map.of("f", "5")), queries.get(2).get("falsehoods"));
    }

    static void assertCompletenessText(String text) {
        List<String> lines = text.lines().toList();
        assertTrue(lines.contains("1+1=2 ? [()][]"), text);
        assertTrue(lines.contains("fib(5)=f ? [(f=5)][..]"), text);
        assertTrue(lines.contains("!(fib(5)=f) ? [..][(f=5)]"), text);
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("a>3 ? [..][") && l.endsWith(",..]")), text);
    }
}
