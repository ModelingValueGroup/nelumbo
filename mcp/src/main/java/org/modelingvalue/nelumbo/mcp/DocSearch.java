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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Keyword search over the markdown docs bundled under /nelumbo-docs/ (see copyDocs in build.gradle.kts). */
public final class DocSearch {

    private record Section(String doc, String heading, String body) {
    }

    public record Match(String doc, String heading, String snippet, int score) {
    }

    private static final int MAX_RESULTS    = 5;
    private static final int SNIPPET_LENGTH = 800;

    private final List<Section> sections = new ArrayList<>();

    /** Loads the bundled docs from the classpath. */
    public DocSearch() {
        this(loadBundledDocs());
    }

    /** Visible for tests: doc name -> markdown content. */
    DocSearch(Map<String, String> docs) {
        docs.forEach((doc, content) -> {
            String        heading = doc;
            StringBuilder body    = new StringBuilder();
            for (String line : content.split("\n", -1)) {
                if (line.startsWith("#")) {
                    addSection(doc, heading, body);
                    heading = line.replaceFirst("^#+\\s*", "").trim();
                    body    = new StringBuilder();
                } else {
                    body.append(line).append('\n');
                }
            }
            addSection(doc, heading, body);
        });
    }

    private void addSection(String doc, String heading, StringBuilder body) {
        String text = body.toString().strip();
        if (!text.isEmpty() || !heading.equals(doc)) {
            sections.add(new Section(doc, heading, text));
        }
    }

    private static Map<String, String> loadBundledDocs() {
        Map<String, String> docs = new LinkedHashMap<>();
        try {
            for (String path : read("/nelumbo-docs/index.txt").split("\n")) {
                if (!path.isBlank()) {
                    docs.put(path, read("/nelumbo-docs/" + path));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return docs;
    }

    private static String read(String resource) throws IOException {
        try (InputStream in = DocSearch.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("missing classpath resource " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public List<Match> search(String query) {
        String[]    terms   = query.toLowerCase().split("\\s+");
        List<Match> matches = new ArrayList<>();
        for (Section s : sections) {
            String headingLower = s.heading().toLowerCase();
            String bodyLower    = s.body().toLowerCase();
            int    score        = 0;
            for (String term : terms) {
                if (term.length() < 3) {
                    continue;
                }
                score += 5 * count(headingLower, term) + count(bodyLower, term);
            }
            if (score > 0) {
                String snippet = s.body().length() <= SNIPPET_LENGTH ? s.body() : s.body().substring(0, SNIPPET_LENGTH) + "...";
                matches.add(new Match(s.doc(), s.heading(), snippet, score));
            }
        }
        matches.sort(Comparator.comparingInt(Match::score).reversed());
        return matches.size() <= MAX_RESULTS ? matches : matches.subList(0, MAX_RESULTS);
    }

    private static int count(String text, String term) {
        int n = 0;
        for (int i = text.indexOf(term); i >= 0; i = text.indexOf(term, i + term.length())) {
            n++;
        }
        return n;
    }
}
