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
import java.nio.file.Paths;
import java.util.List;

import org.eclipse.lsp4e.server.ProcessStreamConnectionProvider;

public class NelumboStreamConnectionProvider extends ProcessStreamConnectionProvider {
    public NelumboStreamConnectionProvider() throws IOException {
        var dir = Paths.get(System.getProperty("user.home"), "nelumbo-lsp");

        if (!dir.toFile().exists()) {
            Files.createDirectories(dir);
        }

        var localJarFile = dir.resolve("server.jar");

        try {
            if (Files.exists(localJarFile)) {
                Files.delete(localJarFile);
            }
            try (var inputStream = getClass().getClassLoader().getResourceAsStream("server.jar")) {
                assert inputStream != null;
                Files.copy(inputStream, localJarFile);
            }
        } catch (Exception ignored) {
            // ignored
        }

        if (!Files.exists(localJarFile)) {
            throw new IOException("Local server jar not found");
        }
        setCommands(List.of("java", "-cp", localJarFile.toFile().getAbsolutePath(), "org.modelingvalue.nelumbo.lsp.Main"));
    }
}
