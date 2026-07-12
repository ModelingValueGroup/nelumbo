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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.mcp.DocSearch.Match;

public class DocSearchTest {

    private static DocSearch fake() {
        Map<String, String> docs = new LinkedHashMap<>();
        docs.put("a.md", """
                # Alpha
                Nothing relevant here.
                ## Precedence rules
                Functor alternatives on Root subtypes need explicit precedence.
                Precedence is written as #N.
                """);
        docs.put("b.md", """
                # Beta
                This mentions precedence once.
                """);
        return new DocSearch(docs);
    }

    private static DocSearch fakeMany() {
        Map<String, String> docs = new LinkedHashMap<>();
        docs.put("a.md", "# Alpha\nrule rule rule rule rule rule\n");
        docs.put("b.md", "# Beta\nrule rule rule rule rule\n");
        docs.put("c.md", "# Gamma\nrule rule rule rule\n");
        docs.put("d.md", "# Delta\nrule rule rule\n");
        docs.put("e.md", "# Epsilon\nrule rule\n");
        docs.put("f.md", "# Zeta\nrule\n");
        return new DocSearch(docs);
    }

    @Test
    public void ranksSectionWithMostHitsFirst() {
        List<Match> ms = fake().search("precedence");
        assertFalse(ms.isEmpty());
        assertEquals("a.md", ms.get(0).doc());
        assertEquals("Precedence rules", ms.get(0).heading());
        assertTrue(ms.get(0).snippet().contains("#N"));
    }

    @Test
    public void noHitsGivesEmptyList() {
        assertTrue(fake().search("zzzznothing").isEmpty());
    }

    @Test
    public void realBundledDocsAreSearchable() {
        List<Match> ms = new DocSearch().search("pattern");
        assertFalse(ms.isEmpty());
        assertTrue(ms.size() <= 5);
    }

    @Test
    public void shortAndBlankTermsAreIgnored() {
        assertTrue(fake().search("a of I").isEmpty());
        assertTrue(fake().search("   ").isEmpty());
        assertTrue(fake().search("").isEmpty());
    }

    @Test
    public void naturalLanguageQueryFindsRelevantSection() {
        List<Match> ms = new DocSearch().search("how do I write a rule");
        assertFalse(ms.isEmpty());
        boolean topIsWritingRules = ms.get(0).doc().contains("writing-rules");
        boolean topFiveHaveRule   = ms.stream().limit(5).anyMatch(m -> m.doc().contains("rule") || m.heading().toLowerCase().contains("rule"));
        assertTrue(topIsWritingRules || topFiveHaveRule, "expected a rule-related result in top 5, got: " + ms);
    }

    @Test
    public void resultsAreCappedAtFiveAndSortedByScore() {
        List<Match> ms = fakeMany().search("rule");
        assertTrue(ms.size() <= 5, "expected at most 5 results, got " + ms.size());
        for (int i = 1; i < ms.size(); i++) {
            assertTrue(ms.get(i - 1).score() >= ms.get(i).score(),
                    "scores not non-increasing at index " + i + ": " + ms.get(i - 1).score() + " < " + ms.get(i).score());
        }
    }
}
