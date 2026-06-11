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

package org.modelingvalue.nelumbo.lsp.eclipse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.eclipse.lsp4e.server.ProcessStreamConnectionProvider;

public class NelumboStreamConnectionProvider extends ProcessStreamConnectionProvider {
    public NelumboStreamConnectionProvider() throws IOException {
        var dir = Paths.get(System.getProperty("user.home"), "nelumbo-lsp");

        if (!dir.toFile().exists()) {
            Files.createDirectories(dir);
        }

        var localJarFile = dir.resolve("server.jar");

        syncServerJar(localJarFile);

        setCommands(List.of("java", "-cp", localJarFile.toFile().getAbsolutePath(), "org.modelingvalue.nelumbo.lsp.Main"));
    }

    /**
     * Make {@code localJarFile} match the {@code server.jar} bundled in this plugin.
     * <p>
     * Eclipse re-creates this provider every time the language server starts, including on restart while an
     * {@code .nl} file is still open. On Windows the JVM that ran the previous server keeps {@code server.jar}
     * locked for the lifetime of its process, so blindly deleting/overwriting it fails with "The process cannot
     * access the file because it is being used by another process". We therefore:
     * <ol>
     *   <li>skip the copy entirely when the on-disk jar already has the same content (the common restart case), and</li>
     *   <li>only attempt to replace it when the content differs, tolerating a lock by reusing the existing jar.</li>
     * </ol>
     */
    private static void syncServerJar(Path localJarFile) throws IOException {
        var tmpFile = localJarFile.resolveSibling("server.jar.tmp");
        try (var inputStream = NelumboStreamConnectionProvider.class.getClassLoader().getResourceAsStream("server.jar")) {
            if (inputStream == null) {
                throw new IOException("server.jar not found in plugin bundle");
            }
            Files.copy(inputStream, tmpFile, StandardCopyOption.REPLACE_EXISTING);

            // Already up to date: leave the (possibly locked) running jar untouched.
            if (Files.exists(localJarFile) && Files.mismatch(tmpFile, localJarFile) == -1L) {
                return;
            }

            try {
                Files.move(tmpFile, localJarFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                // The current jar is locked by a still-running server (Windows). If we have any usable jar
                // on disk, carry on with it rather than failing language-server startup outright.
                if (!Files.exists(localJarFile)) {
                    throw e;
                }
            }
        } finally {
            Files.deleteIfExists(tmpFile);
        }
    }
}
