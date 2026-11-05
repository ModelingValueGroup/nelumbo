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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The /library page's catalogue: the standard library modules, each with its {@code .nl} source (from the core jar)
 * and the opening paragraph of its reference page as explanation, so the text is written once, in the docs.
 */
public final class Library {

    /** {@code name} as in {@code import nelumbo.<name>}; {@code docsPath} is the module's reference page. */
    public record Module(String name, String group, String docsPath, String intro, String source) {

        public Map<String, Object> json() {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("name", name);
            json.put("group", group);
            json.put("intro", intro);
            json.put("docs", DocsSite.urlOf(docsPath));
            return json;
        }
    }

    /** In sidebar order: the bootstrap levels, then the packages in docs order. */
    private static final List<String[]> MODULES = List.of(
            new String[]{"lang", "Core", "reference/lang/index.md"},
            new String[]{"logic", "Core", "reference/logic/index.md"},
            new String[]{"integers", "Packages", "reference/packages/integers.md"},
            new String[]{"rationals", "Packages", "reference/packages/rationals.md"},
            new String[]{"strings", "Packages", "reference/packages/strings.md"},
            new String[]{"collections", "Packages", "reference/packages/collections.md"},
            new String[]{"datetime", "Packages", "reference/packages/datetime.md"});

    static final String SOURCE_ROOT = "/org/modelingvalue/nelumbo/";

    private final Map<String, Module> modules = new LinkedHashMap<>();

    /** Fails when a module's source or reference page is missing, so a rename breaks the build, not the page. */
    public static Library load(DocsSite docs) {
        Library library = new Library();
        for (String[] m : MODULES) {
            String intro = docs.intro(m[2]).orElseThrow(() -> new IllegalStateException("no intro paragraph in docs/" + m[2]));
            library.modules.put(m[0], new Module(m[0], m[1], m[2], intro, readSource(m[0])));
        }
        return library;
    }

    public List<Module> modules() {
        return List.copyOf(modules.values());
    }

    public Optional<Module> module(String name) {
        return Optional.ofNullable(modules.get(name));
    }

    /** The {@code /library} response: the modules in sidebar order, without their sources. */
    public Map<String, Object> json() {
        return Map.of("modules", modules.values().stream().map(Module::json).toList());
    }

    private static String readSource(String name) {
        String path = SOURCE_ROOT + name + "/" + name + ".nl";
        try (InputStream in = Library.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("library module not found on classpath: " + path);
            }
            // always \n line endings, like the examples (NelumboServer.exampleSource)
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + path, e);
        }
    }
}
