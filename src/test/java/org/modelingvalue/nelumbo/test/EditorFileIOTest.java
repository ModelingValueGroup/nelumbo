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

package org.modelingvalue.nelumbo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.modelingvalue.nelumbo.tools.EditorFileIO;

public class EditorFileIOTest {

    @Test
    void writeThenReadRoundTrips(@TempDir Path dir) throws IOException {
        Path   file    = dir.resolve("sample.nl");
        String content = "Person :: Object\npc(Hendrik, Juliana)\n";
        EditorFileIO.write(file, content);
        assertEquals(content, EditorFileIO.read(file));
    }

    @Test
    void writeOverwritesExistingContent(@TempDir Path dir) throws IOException {
        // Auto-save writes the whole file each time; the second write must fully replace the first.
        Path file = dir.resolve("sample.nl");
        EditorFileIO.write(file, "first version");
        EditorFileIO.write(file, "second");
        assertEquals("second", EditorFileIO.read(file));
    }

    @Test
    void roundTripsUtf8(@TempDir Path dir) throws IOException {
        // Nelumbo source uses non-ASCII operators (e.g. ∀, ⇔); they must survive a save/load cycle.
        Path   file    = dir.resolve("unicode.nl");
        String content = "café — π ∀x";
        EditorFileIO.write(file, content);
        assertEquals(content, EditorFileIO.read(file));
    }

    @Test
    void readMissingFileThrows(@TempDir Path dir) {
        // Opening a path that does not exist must fail loudly so the window can show an error.
        Path missing = dir.resolve("does-not-exist.nl");
        assertThrows(IOException.class, () -> EditorFileIO.read(missing));
    }
}
