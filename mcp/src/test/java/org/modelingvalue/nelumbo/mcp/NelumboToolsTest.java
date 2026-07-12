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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class NelumboToolsTest {

    private final NelumboTools tools = new NelumboTools(10_000);

    @Test
    @SuppressWarnings("unchecked")
    public void evalNlOnGoodModel() {
        Map<String, Object> r = tools.evalNl(ModelSkeleton.skeleton("t"), "t.nl");
        assertEquals(Boolean.TRUE, r.get("ok"));
        assertTrue(((List<Object>) r.get("diagnostics")).isEmpty());
        List<Map<String, Object>> queries = (List<Map<String, Object>>) r.get("queryResults");
        assertEquals(4, queries.size());
        assertEquals(Boolean.TRUE, queries.get(0).get("expectationMatched"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void evalNlEnrichesDiagnostics() {
        Map<String, Object> r = tools.evalNl("flurb @@ blarg\n", "bad.nl");
        assertEquals(Boolean.FALSE, r.get("ok"));
        List<Map<String, Object>> ds = (List<Map<String, Object>>) r.get("diagnostics");
        Map<String, Object> d = ds.get(0);
        assertEquals("flurb @@ blarg", d.get("sourceLine"));
        assertNotNull(d.get("caret"));
        assertTrue(((Integer) d.get("line")) >= 1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void evalNlAddsCascadeNoteWhenManyDiagnostics() {
        String bad = """
                import  nelumbo.integers

                Integer ::= fib(<Integer>)

                Integer n, f

                fib(n)=f <=>  f=n                 if n>=0 & n<=1,
                              f=fib(n-1)+fib(n-2) if n>1

                fib(0)=f ? [(f=99)][..]
                fib(1)=f ? [(f=99)][..]
                fib(2)=f ? [(f=99)][..]
                fib(3)=f ? [(f=99)][..]
                """;
        Map<String, Object> r = tools.evalNl(bad, "cascade.nl");
        List<Map<String, Object>> ds = (List<Map<String, Object>>) r.get("diagnostics");
        assertTrue(ds.size() > 3, "expected >3 diagnostics but got: " + ds);
        assertTrue(String.valueOf(ds.get(0).get("hint")).contains("cascade"), "first diagnostic hint should mention cascade: " + ds.get(0));
        assertTrue(!String.valueOf(ds.get(1).get("hint")).contains("cascade"), "non-first diagnostic hint should not mention cascade: " + ds.get(1));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void searchDocsReturnsResults() {
        Map<String, Object> r = tools.searchDocs("precedence");
        assertTrue(((List<Object>) r.get("results")).size() > 0);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getExampleListsAndFetches() {
        Map<String, Object> listing = tools.getExample(null);
        assertTrue(((List<Object>) listing.get("examples")).size() > 10);
        Map<String, Object> one = tools.getExample("family");
        assertTrue(String.valueOf(one.get("content")).contains("pc(Hendrik, Juliana)"));
        Map<String, Object> unknown = tools.getExample("nope");
        assertNotNull(unknown.get("error"));
    }

    @Test
    public void newModelReturnsSkeleton() {
        Map<String, Object> r = tools.newModel("Loan check");
        assertTrue(String.valueOf(r.get("content")).contains("Loan check"));
    }

    @Test
    public void evalNlRejectsNullContent() {
        assertThrows(IllegalArgumentException.class, () -> tools.evalNl(null, "x.nl"));
    }
}
