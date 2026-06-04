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

package org.modelingvalue.nelumbo.lsp;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ConcurrentHashMap;

import org.modelingvalue.nelumbo.KnowledgeBase;

/**
 * Materialises bundled {@code .nl} library files (e.g. {@code /org/modelingvalue/nelumbo/lang/lang.nl})
 * onto disk so the LSP client can navigate into them. Tokens originating from a classpath-loaded
 * resource carry their classpath path as their fileName; this class converts that path to a
 * {@code file://} URI by lazily extracting the resource into a per-session temp directory,
 * preserving the original relative path so different libraries don't collide.
 * <p>
 * Extraction is per-process and cached: a given classpath path is read once and reused for
 * every subsequent navigation in the same LSP server lifetime.
 */
public final class BundledFileCache {
    private static final ConcurrentHashMap<String, String> URI_CACHE = new ConcurrentHashMap<>();
    private static volatile Path                           ROOT;

    private BundledFileCache() {}

    /**
     * Returns a {@code file://} URI for the given classpath resource path, or {@code null}
     * if the resource cannot be found or extraction fails.
     */
    public static String classpathToFileUri(String classpathPath) {
        if (classpathPath == null || classpathPath.isEmpty()) {
            return null;
        }
        return URI_CACHE.computeIfAbsent(classpathPath, BundledFileCache::extract);
    }

    private static String extract(String classpathPath) {
        try {
            Path   root = root();
            String rel  = classpathPath.startsWith("/") ? classpathPath.substring(1) : classpathPath;
            Path   target = root.resolve(rel).normalize();
            // Defensive: make sure the resolved path stays inside our temp root.
            if (!target.startsWith(root)) {
                return null;
            }
            if (!Files.exists(target)) {
                Files.createDirectories(target.getParent());
                try (InputStream in = KnowledgeBase.class.getResourceAsStream(classpathPath)) {
                    if (in == null) {
                        return null;
                    }
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
                // Library files are bundled & read-only — discourage editing.
                target.toFile().setReadOnly();
            }
            return target.toUri().toString();
        } catch (Exception e) {
            if (Main.debugging()) {
                System.err.println("BundledFileCache: failed to extract " + classpathPath + ": " + e);
            }
            return null;
        }
    }

    private static Path root() throws java.io.IOException {
        Path r = ROOT;
        if (r == null) {
            synchronized (BundledFileCache.class) {
                r = ROOT;
                if (r == null) {
                    r = Files.createTempDirectory("nelumbo-lsp-bundled-");
                    r.toFile().deleteOnExit();
                    ROOT = r;
                }
            }
        }
        return r;
    }
}
