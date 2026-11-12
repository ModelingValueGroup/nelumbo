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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The swear word check of shared solutions ({@code /blocked-words.txt}, one lowercase word per line). A name is short
 * and often a first name: a word of 4+ letters is blocked anywhere in a run of letters ("fuckyou123"), a shorter one
 * only as a whole run, so "Hassan" passes "ass". A text is checked on whole words only, so code and comments do not
 * trip over words inside words.
 */
final class BlockedWords {

    private static final Pattern LETTER_RUNS = Pattern.compile("[a-z]+");
    private static final Pattern NON_LETTERS = Pattern.compile("\\P{L}+");

    private final Set<String> words;

    BlockedWords(Collection<String> words) {
        this.words = Set.copyOf(words);
    }

    static BlockedWords load() {
        try (InputStream in = BlockedWords.class.getResourceAsStream("/blocked-words.txt")) {
            if (in == null) {
                throw new IllegalStateException("resource not found on classpath: /blocked-words.txt");
            }
            List<String> words = new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().map(String::strip)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#")).toList();
            return new BlockedWords(words);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read /blocked-words.txt", e);
        }
    }

    boolean inName(String name) {
        Matcher runs = LETTER_RUNS.matcher(name.toLowerCase(Locale.ROOT));
        while (runs.find()) {
            String run = runs.group();
            for (String word : words) {
                if (word.length() >= 4 ? run.contains(word) : run.equals(word)) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean inText(String text) {
        return NON_LETTERS.splitAsStream(text.toLowerCase(Locale.ROOT)).anyMatch(words::contains);
    }
}
