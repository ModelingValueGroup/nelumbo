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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * DocsSite turns the bundled markdown into the /docs pages. What matters for readers: every cross-reference in the
 * docs keeps working (the docs were written for GitHub, so the anchors and relative links follow GitHub's rules), and
 * the sidebar lets them find every page.
 */
class DocsSiteTest {

    private static final String TEMPLATE = "<title>@TITLE@</title><nav>@NAV@</nav><main>@CONTENT@</main>";

    private static DocsSite site() {
        Map<String, String> md = new LinkedHashMap<>();
        md.put("documentation.md", """
                # Nelumbo documentation

                Start with [reading a test](getting-started/reading-a-test.md) or the [README](../README.md).
                See the [examples](../src/main/resources/org/modelingvalue/nelumbo/examples/) too.
                """);
        md.put("getting-started/reading-a-test.md", """
                # Reading a query and test

                See [grammar](../reference/grammar.md#connected-token-groups---) and [fibonacci.nl](../../src/main/resources/org/modelingvalue/nelumbo/examples/fibonacci.nl).
                Back to the [overview](../documentation.md) or [up](#reading-a-query-and-test); [GitHub](https://github.com/x) stays.
                """);
        md.put("reference/grammar.md", """
                # Grammar

                ## Connected-token groups: `<[> ... <]>`

                ## Recipe 2 — comparison predicate

                ## Twice

                ## Twice

                | a | b |
                |---|---|
                | 1 | 2 |
                """);
        md.put("reference/stdlib/logic.md", "# `nelumbo.logic`\n\ntext\n");
        md.put("NELUMBO.md", "# Slides\n");
        return new DocsSite(md, TEMPLATE);
    }

    @Test
    void headingAnchorsFollowGithubSlugsSoCrossPageLinksInTheDocsResolve() {
        // the docs link to these exact anchors; GitHub keeps a hyphen for each space and drops other punctuation
        assertEquals("connected-token-groups---", DocsSite.slug("Connected-token groups: `<[> ... <]>`"));
        assertEquals("recipe-2--comparison-predicate", DocsSite.slug("Recipe 2 — comparison predicate"));
        assertEquals("nelumbologic", DocsSite.slug("nelumbo.logic"));

        String grammar = site().page("/docs/reference/grammar.html").orElseThrow();
        assertTrue(grammar.contains("<h2 id=\"connected-token-groups---\">"), grammar);
        assertTrue(grammar.contains("<h2 id=\"twice\">") && grammar.contains("<h2 id=\"twice-1\">"),
                "duplicate headings get numbered like on GitHub: " + grammar);
    }

    @Test
    void relativeMarkdownLinksBecomeDocsUrlsAndLinksOutOfTheDocsFolderGoToGithub() {
        String page = site().page("/docs/getting-started/reading-a-test.html").orElseThrow();
        assertTrue(page.contains("href=\"/docs/reference/grammar.html#connected-token-groups---\""), page);
        assertTrue(page.contains("href=\"https://github.com/ModelingValueGroup/nelumbo/blob/master/src/main/resources/org/modelingvalue/nelumbo/examples/fibonacci.nl\""), page);
        assertTrue(page.contains("href=\"/docs/\""), "documentation.md is the docs index: " + page);
        assertTrue(page.contains("href=\"#reading-a-query-and-test\""), page);
        assertTrue(page.contains("href=\"https://github.com/x\""), page);

        String index = site().page("/docs/").orElseThrow();
        assertTrue(index.contains("href=\"/docs/getting-started/reading-a-test.html\""), index);
        assertTrue(index.contains("href=\"https://github.com/ModelingValueGroup/nelumbo/blob/master/README.md\""), index);
        assertTrue(index.contains("href=\"https://github.com/ModelingValueGroup/nelumbo/tree/master/src/main/resources/org/modelingvalue/nelumbo/examples/\""), index);
    }

    @Test
    void sidebarListsEveryGroupedPageByTitleAndMarksTheCurrentOne() {
        String grammar = site().page("/docs/reference/grammar.html").orElseThrow();
        assertTrue(grammar.contains("<a href=\"/docs/\">Overview</a>"), grammar);
        assertTrue(grammar.contains("<a href=\"/docs/getting-started/reading-a-test.html\">Reading a query and test</a>"), grammar);
        assertTrue(grammar.contains("<a href=\"/docs/reference/grammar.html\" class=\"active\">Grammar</a>"), grammar);
        assertTrue(grammar.contains("<a href=\"/docs/reference/stdlib/logic.html\">nelumbo.logic</a>"), grammar);
        assertTrue(grammar.indexOf("Getting started") < grammar.indexOf("Reference")
                && grammar.indexOf("Reference") < grammar.indexOf("Standard library"),
                "groups keep the documented reading order: " + grammar);
        assertFalse(grammar.contains("Slides"), "root-level pages other than the index are not in the sidebar: " + grammar);
        assertTrue(site().page("/docs/NELUMBO.html").isPresent(), "...but they are still served for the links that point at them");
    }

    @Test
    void indexIsServedAtDocsRootAndUnknownPagesAreNot() {
        DocsSite site = site();
        assertTrue(site.page("/docs/").orElseThrow().contains("<title>Nelumbo documentation</title>"));
        assertTrue(site.page("/docs").isPresent());
        assertTrue(site.page("/docs/reference/grammar.html").orElseThrow().contains("<table>"), "GFM tables must render");
        assertTrue(site.page("/docs/reference/missing.html").isEmpty());
        assertTrue(site.page("/docs/documentation.html").isEmpty(), "the index has one URL: /docs/");
        assertTrue(site.notFoundPage().contains("<nav>"), "the 404 page keeps the sidebar so the reader is not stranded");
    }

    @Test
    void theBundledDocsAllRender() {
        DocsSite site = DocsSite.load();
        assertTrue(site.pageCount() > 25, "expected the whole docs tree to be bundled, got " + site.pageCount());
        String grammar = site.page("/docs/reference/grammar.html").orElseThrow();
        assertTrue(grammar.contains("<title>Grammar - Nelumbo docs</title>"), grammar.substring(0, 300));
        assertTrue(grammar.contains("<h1"), grammar.substring(0, 300));
    }

    @Test
    void everyInternalLinkAndAnchorInTheBundledDocsResolves() {
        // a moved or split page must not leave a link behind that 404s or lands on a missing heading
        DocsSite site = DocsSite.load();
        Pattern href = Pattern.compile("href=\"(/docs/[^\"#]*)?(?:#([^\"]*))?\"");
        Deque<String> todo = new ArrayDeque<>(List.of(DocsSite.URL_PREFIX));
        Set<String> seen = new HashSet<>(todo);
        List<String> broken = new ArrayList<>();
        while (!todo.isEmpty()) {
            String url = todo.pop();
            Matcher m = href.matcher(site.page(url).orElseThrow());
            while (m.find()) {
                if (m.group(1) == null && m.group(2) == null) {
                    continue;
                }
                String target = m.group(1) == null ? url : m.group(1);
                String fragment = m.group(2);
                Optional<String> page = site.page(target);
                if (page.isEmpty()) {
                    broken.add(url + " -> " + target);
                    continue;
                }
                if (fragment != null && !fragment.isEmpty() && !page.get().contains("id=\"" + fragment + "\"")) {
                    broken.add(url + " -> " + target + "#" + fragment);
                }
                if (seen.add(target)) {
                    todo.push(target);
                }
            }
        }
        assertTrue(broken.isEmpty(), "broken docs links:\n" + String.join("\n", broken));
        // NELUMBO.md is not in the sidebar but the overview links it, so every page is reachable
        assertEquals(site.pageCount(), seen.size(), "every page should be reachable from the overview; reached " + seen);
    }
}
