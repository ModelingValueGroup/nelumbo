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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
        store.put(new Solution("Frits", "h1", now.get(), "fact p(A)", null));
        assertEquals("fact p(A)", store.get("FRITS").orElseThrow().text());
        assertEquals("Frits", store.get("frits").orElseThrow().name());
    }

    @Test
    void aSolutionExpiresSevenDaysAfterItsLastSave() {
        SolutionStore store = store(dir);
        store.put(new Solution("Frits", "h1", now.get(), "v1", null));
        now.addAndGet(SolutionStore.KEEP_MS - 1);
        store.put(new Solution("Frits", "h1", now.get(), "v2", null));
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
        store(dir).put(new Solution("Frits", "h1", now.get(), "Zoë\r\n", null));
        SolutionStore again = store(dir);
        assertEquals(new Solution("Frits", "h1", now.get(), "Zoë\r\n", null), again.get("frits").orElseThrow());
        assertEquals(Solution.ENTRY_BYTES + 6, again.totalBytes());
    }

    @Test
    void expiredFilesAreDeletedAtStart() throws IOException {
        store(dir).put(new Solution("Frits", "h1", now.get(), "x", null));
        now.addAndGet(SolutionStore.KEEP_MS + 1);
        store(dir);
        try (Stream<Path> files = Files.list(dir)) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void aBrokenFileIsSkipped() throws IOException {
        store(dir).put(new Solution("Frits", "h1", now.get(), "x", null));
        Files.writeString(dir.resolve("broken.json"), "{\"name\":\"bro");
        SolutionStore again = store(dir);
        assertTrue(again.get("broken").isEmpty());
        assertEquals("x", again.get("Frits").orElseThrow().text());
    }

    @Test
    void totalBytesFollowsReplacements() {
        SolutionStore store = store(dir);
        store.put(new Solution("Frits", "h1", now.get(), "ë", null));
        assertEquals(Solution.ENTRY_BYTES + 2, store.totalBytes());
        store.put(new Solution("frits", "h1", now.get(), "abc", null));
        assertEquals(Solution.ENTRY_BYTES + 3, store.totalBytes());
    }

    @Test
    void withoutADirItIsMemoryOnly() {
        SolutionStore store = store(null);
        store.put(new Solution("Frits", "h1", now.get(), "x", null));
        assertEquals("x", store.get("Frits").orElseThrow().text());
    }

    @Test
    void theIpIsStoredAndAnOldFileWithoutItLoads() throws Exception {
        store(dir).put(new Solution("Frits", "h1", now.get(), "x", "2001:db8::7"));
        assertEquals("2001:db8::7", store(dir).get("Frits").orElseThrow().ip());
        Files.writeString(dir.resolve("old.json"), "{\"name\":\"Old\",\"ownerHash\":\"h2\",\"savedAt\":" + now.get() + ",\"text\":\"y\"}");
        assertNull(store(dir).get("Old").orElseThrow().ip());
    }

    @Test
    void listIsNewestFirstWithoutExpired() {
        SolutionStore store = store(dir);
        store.put(new Solution("A", "h1", now.get(), "a", null));
        now.addAndGet(1000);
        store.put(new Solution("B", "h1", now.get(), "b", null));
        assertEquals(List.of("B", "A"), store.list().stream().map(Solution::name).toList());
        now.addAndGet(SolutionStore.KEEP_MS + 1);
        assertEquals(List.of(), store.list());
    }

    @Test
    void deleteFreesTheNameAndItsBytes() {
        SolutionStore store = store(dir);
        store.put(new Solution("Frits", "h1", now.get(), "abc", null));
        assertTrue(store.delete("FRITS"));
        assertFalse(store.delete("Frits"));
        assertEquals(0, store.totalBytes());
        assertFalse(Files.exists(dir.resolve("frits.json")));
        assertTrue(store(dir).get("Frits").isEmpty());
    }

    @Test
    void deleteAllCountsAndEmpties() {
        SolutionStore store = store(dir);
        store.put(new Solution("A", "h1", now.get(), "a", null));
        store.put(new Solution("B", "h2", now.get(), "b", null));
        assertEquals(2, store.deleteAll());
        assertEquals(0, store.count());
        assertEquals(0, store.totalBytes());
        assertEquals(0, store(dir).count());
    }

    @Test
    void theSharingSwitchSurvivesARestart() {
        Path fresh = dir.resolve("not-yet-there");
        assertTrue(store(fresh).sharingEnabled());
        store(fresh).setSharingEnabled(false);
        assertFalse(store(fresh).sharingEnabled());
        assertTrue(Files.exists(fresh.resolve(SolutionStore.SHARING_OFF_FILE)));
        store(fresh).setSharingEnabled(true);
        assertTrue(store(fresh).sharingEnabled());
        SolutionStore memory = store(null);
        memory.setSharingEnabled(false);
        assertFalse(memory.sharingEnabled());
    }
}
