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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SolutionStoreTest {

    @TempDir
    Path                     dir;
    private final AtomicLong now = new AtomicLong(1_000_000_000_000L);

    private SolutionStore store(Path d) {
        return new SolutionStore(d, now::get);
    }

    @Test
    void namesIgnoreCase() {
        SolutionStore store = store(dir);
        store.put(new Solution("Frits", "h1", now.get(), "fact p(A)"));
        assertEquals("fact p(A)", store.get("FRITS").orElseThrow().text());
        assertEquals("Frits", store.get("frits").orElseThrow().name());
    }

    @Test
    void aSolutionExpiresSevenDaysAfterItsLastSave() {
        SolutionStore store = store(dir);
        store.put(new Solution("Frits", "h1", now.get(), "v1"));
        now.addAndGet(SolutionStore.KEEP_MS - 1);
        store.put(new Solution("Frits", "h1", now.get(), "v2"));
        now.addAndGet(2);
        assertEquals("v2", store.get("Frits").orElseThrow().text());
        now.addAndGet(SolutionStore.KEEP_MS);
        assertTrue(store.get("Frits").isEmpty());
        store.expire();
        assertEquals(0, store.totalBytes());
        assertFalse(Files.exists(dir.resolve("frits.json")));
    }

    @Test
    void solutionsSurviveARestart() {
        store(dir).put(new Solution("Frits", "h1", now.get(), "Zoë\r\n"));
        SolutionStore again = store(dir);
        assertEquals(new Solution("Frits", "h1", now.get(), "Zoë\r\n"), again.get("frits").orElseThrow());
        assertEquals(Solution.ENTRY_BYTES + 6, again.totalBytes());
    }

    @Test
    void expiredFilesAreDeletedAtStart() throws IOException {
        store(dir).put(new Solution("Frits", "h1", now.get(), "x"));
        now.addAndGet(SolutionStore.KEEP_MS + 1);
        store(dir);
        try (Stream<Path> files = Files.list(dir)) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void aBrokenFileIsSkipped() throws IOException {
        store(dir).put(new Solution("Frits", "h1", now.get(), "x"));
        Files.writeString(dir.resolve("broken.json"), "{\"name\":\"bro");
        SolutionStore again = store(dir);
        assertTrue(again.get("broken").isEmpty());
        assertEquals("x", again.get("Frits").orElseThrow().text());
    }

    @Test
    void totalBytesFollowsReplacements() {
        SolutionStore store = store(dir);
        store.put(new Solution("Frits", "h1", now.get(), "ë"));
        assertEquals(Solution.ENTRY_BYTES + 2, store.totalBytes());
        store.put(new Solution("frits", "h1", now.get(), "abc"));
        assertEquals(Solution.ENTRY_BYTES + 3, store.totalBytes());
    }

    @Test
    void withoutADirItIsMemoryOnly() {
        SolutionStore store = store(null);
        store.put(new Solution("Frits", "h1", now.get(), "x"));
        assertEquals("x", store.get("Frits").orElseThrow().text());
    }
}
