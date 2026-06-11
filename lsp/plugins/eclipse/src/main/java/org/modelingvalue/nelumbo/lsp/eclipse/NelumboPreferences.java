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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * The plugin's settings, stored as a plain Java properties file under {@code ~/nelumbo-lsp/}
 * (the same directory the server jar is unpacked to). Deliberately uses no Eclipse/OSGi preference
 * API, so it loads from any context (including the global command execution listener) without
 * depending on bundle/package visibility.
 */
public final class NelumboPreferences {
    public static final String  FORMAT_ON_SAVE         = "formatOnSave";
    public static final boolean FORMAT_ON_SAVE_DEFAULT = true;

    private static final Path FILE = Paths.get(System.getProperty("user.home"), "nelumbo-lsp", "settings.properties");

    private NelumboPreferences() {
    }

    public static synchronized boolean isFormatOnSave() {
        String value = load().getProperty(FORMAT_ON_SAVE);
        return value == null ? FORMAT_ON_SAVE_DEFAULT : Boolean.parseBoolean(value);
    }

    public static synchronized void setFormatOnSave(boolean value) {
        Properties properties = load();
        properties.setProperty(FORMAT_ON_SAVE, Boolean.toString(value));
        store(properties);
    }

    private static Properties load() {
        Properties properties = new Properties();
        if (Files.exists(FILE)) {
            try (InputStream in = Files.newInputStream(FILE)) {
                properties.load(in);
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }
        return properties;
    }

    private static void store(Properties properties) {
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream out = Files.newOutputStream(FILE)) {
                properties.store(out, "Nelumbo plugin settings");
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }
}
