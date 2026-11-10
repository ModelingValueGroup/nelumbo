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
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.modelingvalue.nelumbo.NelumboConstants;

/**
 * Assembles the tutorial page: every editor in {@code tutorial.html} refers to a {@code .nl} file in the core jar
 * ({@code <div class="nelumbo-field" ... data-src="logic/2-exercise.nl"></div>}), and its text is filled in here,
 * once at server start. The browser gets the editors with their text inline, as before. A missing file fails the
 * server start, like a missing library module.
 */
public final class TutorialPage {

    /** Classpath folder of the tutorial's {@code .nl} files: one folder per section. */
    public static final String  SOURCE_ROOT = NelumboConstants.NELUMBO_TUTORIAL;

    private static final Pattern EDITOR      = Pattern
            .compile("<(div|pre) class=\"(nelumbo-field|nelumbo-solution)\"([^>]*?) data-src=\"([^\"]+)\"></\\1>");

    private TutorialPage() {
    }

    /** The page with the text of every referenced {@code .nl} file in its editor. */
    public static String render(String page) {
        Matcher matcher = EDITOR.matcher(page);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String tag = matcher.group(1);
            String editor = "<" + tag + " class=\"" + matcher.group(2) + "\"" + matcher.group(3) + ">"
                    + escape(source(matcher.group(4))) + "</" + tag + ">";
            matcher.appendReplacement(out, Matcher.quoteReplacement(editor));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /** The text of one tutorial file, always with \n line endings (a Windows checkout has CRLF). */
    public static String source(String name) {
        String path = SOURCE_ROOT + name;
        try (InputStream in = TutorialPage.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("tutorial file not found on classpath: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + path, e);
        }
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
