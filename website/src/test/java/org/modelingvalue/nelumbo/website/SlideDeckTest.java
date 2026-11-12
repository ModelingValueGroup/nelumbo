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

package org.modelingvalue.nelumbo.website;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator.EvalResult;
import org.modelingvalue.nelumbo.tools.NelumboEvaluator.QueryOutcome;

/**
 * The slide deck ({@code docs/NELUMBO.md}) shows Nelumbo as a workshop's first impression: its code must be valid,
 * the results written next to its queries must be the engine's, and its colors must be the editors'.
 */
class SlideDeckTest {

    /** A query line with its result written as a comment: {@code fib(7) ?   // [(13)][..]}. */
    private static final Pattern RESULT_COMMENT = Pattern.compile("\\?\\s*//\\s*(\\[\\S*)");

    private static String deck() throws IOException {
        try (InputStream in = SlideDeckTest.class.getResourceAsStream(DocsSite.RESOURCE_ROOT + "NELUMBO.md")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void everyNelumboBlockOfTheDeckEvaluatesAndShowsTheEnginesResults() throws IOException {
        List<SlideDeck.Block> blocks = SlideDeck.blocks(deck());
        assertTrue(blocks.size() >= 10, "the deck's nelumbo blocks: " + blocks.size());
        for (SlideDeck.Block block : blocks) {
            String source = block.context().isEmpty() ? block.code() : block.context() + "\n" + block.code();
            EvalResult result = NelumboEvaluator.evaluate(source, "<slide>", 30_000, NelumboHighlighter.IMPORTS);
            assertTrue(result.diagnostics().isEmpty(), result.diagnostics() + " in\n" + block.code());
            List<String> shown = new ArrayList<>();
            for (String line : block.code().split("\n")) {
                Matcher m = RESULT_COMMENT.matcher(line);
                if (m.find()) {
                    shown.add(m.group(1));
                }
            }
            List<String> actual = result.queries().stream().map(QueryOutcome::result).toList();
            assertTrue(actual.containsAll(shown), "shown " + shown + ", engine " + actual + " in\n" + block.code());
        }
    }

    @Test
    void aContextColorsTheFragmentButIsNotShown() {
        String md = """
                <!-- nelumbo-context
                Integer ::= twice(<Integer>)
                Integer n
                -->

                ```nelumbo
                twice(n) == n+n
                ```
                """;
        String rendered = SlideDeck.render(md);
        assertTrue(rendered.startsWith("<pre><code class=\"hljs nohighlight\" data-noescape>"), rendered);
        assertTrue(rendered.contains("<span class=\"nl-variable\">n</span>"), rendered);
        assertFalse(rendered.contains("nelumbo-context") || rendered.contains("Integer"), rendered);
        assertFalse(rendered.contains("```"), rendered);
    }

    @Test
    void theDocsPageOfTheDeckUsesTheContextToo() {
        String md = "# Slides\n\n<!-- nelumbo-context\nInteger ::= twice(<Integer>)\nInteger n\n-->\n\n```nelumbo\ntwice(n) == n+n\n```\n";
        DocsSite site = new DocsSite(java.util.Map.of("NELUMBO.md", md), "<main>@CONTENT@</main>@TITLE@@NAV@");
        String page = site.page("/docs/NELUMBO.html").orElseThrow();
        assertTrue(page.contains("<span class=\"nl-variable\">n</span>"), page);
        assertFalse(page.replaceAll("(?s)<!--.*?-->", "").contains("Integer ::="), "the context is not shown: " + page);
    }

    @Test
    void otherBlocksAndTextStayAsTheyAre() {
        String md = "## Java\n\n```java\nint a = 1;\n```\n\n<!-- .slide: class=\"dense\" -->\n";
        assertEquals(md, SlideDeck.render(md));
    }

    @Test
    void theDeckKeepsNoNelumboFenceAfterRendering() throws IOException {
        String rendered = SlideDeck.render(deck());
        assertFalse(rendered.contains("```nelumbo"), "every nelumbo block is colored");
        assertFalse(rendered.contains("nelumbo-context"), "no context is shown");
    }
}
