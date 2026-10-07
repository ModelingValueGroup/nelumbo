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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Code blocks in the docs are colored like the editors color them: by parsing the snippet against the shipped modules,
 * so a type, a variable and a keyword look different even though all three are plain names to the tokenizer. Snippets
 * in docs are often fragments, so whatever does not parse must still come out complete and escaped.
 */
class NelumboHighlighterTest {

    private static final NelumboHighlighter HIGHLIGHTER = NelumboHighlighter.shared();

    /** The highlighted html with the spans removed and the entities decoded, i.e. the text a reader sees. */
    private static String visibleText(String html) {
        return html.replaceAll("<[^>]+>", "").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&amp;", "&");
    }

    @Test
    void typesVariablesAndKeywordsGetTheirOwnColorsFromTheParse() {
        String html = HIGHLIGHTER.highlight("""
                Integer n, f
                fact n=1
                """);
        assertTrue(html.contains("<span class=\"nl-type\">Integer</span>"), html);
        assertTrue(html.contains("<span class=\"nl-variable\">n</span>"), html);
        assertTrue(html.contains("<span class=\"nl-keyword\">fact</span>"), html);
        assertTrue(html.contains("<span class=\"nl-number\">1</span>"), html);
    }

    @Test
    void declarationsInsideTheSnippetColorItsLaterLines() {
        String html = HIGHLIGHTER.highlight("""
                Person :: Object
                Person a
                """);
        assertTrue(html.contains("<span class=\"nl-type\">Person</span> <span class=\"nl-operator\">::</span>"), html);
        assertTrue(html.contains("<span class=\"nl-variable\">a</span>"), html);
    }

    @Test
    void commentsAndStringsAreColoredByTheTokenizer() {
        String html = HIGHLIGHTER.highlight("\"a<b\" // note & more\n");
        assertTrue(html.contains("<span class=\"nl-string\">&quot;a&lt;b&quot;</span>"), html);
        assertTrue(html.contains("<span class=\"nl-comment\">// note &amp; more</span>"), html);
    }

    @Test
    void aFragmentThatDoesNotParseStillRendersEveryCharacter() {
        String code = """
                attr OT AN AT ::> {
                    AT ::= <OT>.AN
                    ...
                }
                fib(n) = f <=> f = fib(n-1) + fib(n-2) if n > 1
                """;
        String html = HIGHLIGHTER.highlight(code);
        assertEquals(code, visibleText(html));
        assertTrue(html.contains("<span class=\"nl-"), "unparsed tokens still get their token color: " + html);
    }

    @Test
    void theSnippetDoesNotLeakIntoTheNextOne() {
        // each block is parsed on its own, so a type declared in one is unknown in the next
        HIGHLIGHTER.highlight("Gadget :: Object\n");
        String html = HIGHLIGHTER.highlight("Gadget g\n");
        assertTrue(!html.contains("<span class=\"nl-type\">Gadget</span>"), html);
    }

    @Test
    void aSnippetTheEngineCrashesOnIsShownPlainInsteadOfFailingTheDocs() {
        // re-declaring logic.nl on top of the imported logic overflows the stack in the engine (seen in the stdlib
        // tour, 2026-10-07); the docs must still render that block
        String code = """
                import nelumbo.lang

                Boolean   :: Object
                FactType  :: Boolean
                Function  :: Object
                Literal   :: Object

                private Boolean ::= eq(<Literal>,<Literal>)                 @nelumbo.logic.Equal

                Boolean ::= true                                            @nelumbo.logic.NBoolean,
                            false                                           @nelumbo.logic.NBoolean,
                            unknown                                         @nelumbo.logic.NBoolean,
                            ! <Boolean>                             #25     @nelumbo.logic.Not,
                            <Boolean> & <Boolean>                   #22     @nelumbo.logic.And,
                            <Boolean> | <Boolean>                   #20     @nelumbo.logic.Or,
                            E[<(> <Variable#100> <,> , <)+>](<Boolean#0>)   @nelumbo.logic.ExistentialQuantifier,
                            A[<(> <Variable#100> <,> , <)+>](<Boolean#0>)   @nelumbo.logic.UniversalQuantifier,
                            <Object> =  <Object>                    #30     @nelumbo.logic.NIs,
                            <Object> != <Object>                    #30,
                            <Boolean> -> <Boolean>                  #18,
                            <Boolean> "<->" <Boolean>               #16

                pattern BINDING ::= [ ... ]

                // Top-level statement forms — declared here, not in the Java core
                Root ::= "fact" <Boolean#0>, ...                                       @nelumbo.logic.Fact,
                         <Boolean#0> "<=>" (<Boolean#0> ("if" <Boolean#0>)?), ...      @nelumbo.logic.Rule,
                         <Boolean#0> ? (<BINDING> <BINDING>)?                          @nelumbo.logic.Query

                Boolean p1, p2
                p1 -> p2  <=> !p1 | p2
                p1 <-> p2 <=> (p1 -> p2) & (p2 -> p1)

                Literal  l1, l2
                Function f1, f2
                Object   n1, n2

                l1 = l2  <=> eq(l1, l2)
                l1 = f1  <=> f1 = l1
                n1 != n2 <=> !(n1 = n2)
                """;
        assertEquals(code, visibleText(HIGHLIGHTER.highlight(code)));
    }
}
