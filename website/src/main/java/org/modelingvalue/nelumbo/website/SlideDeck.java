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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prepares the slide deck ({@code docs/NELUMBO.md}) for mkslides. reveal's highlight.js knows no Nelumbo, so every
 * {@code ```nelumbo} block (with the {@code nelumbo-context} comment before it) becomes a {@code pre/code} element
 * colored by {@link NelumboHighlighter}, the same colors as the docs and the editors. The class {@code nohighlight}
 * keeps highlight.js off it, {@code data-noescape} keeps reveal from escaping the spans, {@code hljs} keeps the deck's code style.
 */
public final class SlideDeck {

    private static final Pattern NELUMBO_BLOCK = Pattern.compile(
            "(" + Pattern.quote(NelumboHighlighter.CONTEXT_START) + "\\n.*?\\n" + NelumboHighlighter.CONTEXT_END + "\\n\\n)?```nelumbo\\n(.*?)\\n```",
            Pattern.DOTALL);

    /** A {@code ```nelumbo} block of the deck with its (possibly empty) context. */
    record Block(String context, String code) {
    }

    private SlideDeck() {
    }

    static List<Block> blocks(String markdown) {
        List<Block> blocks = new ArrayList<>();
        Matcher m = NELUMBO_BLOCK.matcher(unix(markdown));
        while (m.find()) {
            blocks.add(block(m));
        }
        return blocks;
    }

    static String render(String markdown) {
        Matcher m = NELUMBO_BLOCK.matcher(unix(markdown));
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            Block b = block(m);
            String html = "<pre><code class=\"hljs nohighlight\" data-noescape>" + NelumboHighlighter.shared().highlight(b.context(), b.code()) + "</code></pre>";
            m.appendReplacement(sb, Matcher.quoteReplacement(html));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static Block block(Matcher m) {
        return new Block(m.group(1) == null ? "" : NelumboHighlighter.context(m.group(1)), m.group(2));
    }

    private static String unix(String s) {
        return s.replace("\r\n", "\n");
    }

    /** {@code SlideDeck <deck.md> <out.md>}: writes the deck with its Nelumbo blocks colored. */
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("usage: SlideDeck <deck.md> <out.md>");
            System.exit(2);
        }
        String markdown = Files.readString(Path.of(args[0]), StandardCharsets.UTF_8);
        Path out = Path.of(args[1]);
        Files.createDirectories(out.toAbsolutePath().getParent());
        Files.writeString(out, render(markdown), StandardCharsets.UTF_8);
        System.exit(0); // the knowledge base's pool threads would keep the JVM alive
    }
}
