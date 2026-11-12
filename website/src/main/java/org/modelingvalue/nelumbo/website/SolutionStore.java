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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The shared solutions, one JSON file per name ({@code <lowercase name>.json}) in {@code dir}; without a dir in memory
 * only. A solution expires {@link #KEEP_MS} after its last save. Names are validated by {@link SolutionService}
 * before they get here, so they are safe file names.
 */
final class SolutionStore {

    static final long   KEEP_MS          = 7 * 86_400_000L;
    static final String SHARING_OFF_FILE = "sharing-off";

    private final Path                  dir;
    private final LongSupplier          clock;
    private final ObjectMapper          mapper    = new ObjectMapper();
    private final Map<String, Solution> solutions = new HashMap<>();
    private long                        totalBytes;
    private boolean                     sharingEnabled;

    SolutionStore(Path dir, LongSupplier clock) {
        this.dir   = dir;
        this.clock = clock;
        sharingEnabled = dir == null || !Files.exists(dir.resolve(SHARING_OFF_FILE));
        if (dir != null) {
            load();
        }
        expire();
    }

    synchronized Optional<Solution> get(String name) {
        Solution solution = solutions.get(key(name));
        return solution == null || expired(solution) ? Optional.empty() : Optional.of(solution);
    }

    synchronized void put(Solution solution) {
        Solution old = solutions.put(key(solution.name()), solution);
        totalBytes += solution.bytes() - (old == null ? 0 : old.bytes());
        if (dir != null) {
            write(solution);
        }
    }

    synchronized List<Solution> list() {
        return solutions.values().stream().filter(s -> !expired(s)).sorted(Comparator.comparingLong(Solution::savedAt).reversed()).toList();
    }

    synchronized boolean delete(String name) {
        Solution solution = solutions.remove(key(name));
        if (solution == null) {
            return false;
        }
        totalBytes -= solution.bytes();
        if (dir != null) {
            delete(file(name));
        }
        return true;
    }

    synchronized int deleteAll() {
        int count = solutions.size();
        for (Solution solution : solutions.values()) {
            if (dir != null) {
                delete(file(solution.name()));
            }
        }
        solutions.clear();
        totalBytes = 0;
        return count;
    }

    synchronized boolean sharingEnabled() {
        return sharingEnabled;
    }

    synchronized void setSharingEnabled(boolean enabled) {
        sharingEnabled = enabled;
        if (dir != null) {
            try {
                if (enabled) {
                    Files.deleteIfExists(dir.resolve(SHARING_OFF_FILE));
                } else {
                    Files.createDirectories(dir);
                    Files.write(dir.resolve(SHARING_OFF_FILE), new byte[0]);
                }
            } catch (IOException e) {
                System.err.println("solutions: cannot switch sharing in " + dir + ": " + e);
            }
        }
    }

    synchronized long totalBytes() {
        return totalBytes;
    }

    synchronized int count() {
        return solutions.size();
    }

    synchronized void expire() {
        Iterator<Solution> it = solutions.values().iterator();
        while (it.hasNext()) {
            Solution solution = it.next();
            if (expired(solution)) {
                it.remove();
                totalBytes -= solution.bytes();
                if (dir != null) {
                    delete(file(solution.name()));
                }
            }
        }
    }

    private boolean expired(Solution solution) {
        return clock.getAsLong() - solution.savedAt() > KEEP_MS;
    }

    private void load() {
        List<Path> files;
        try (Stream<Path> list = Files.list(dir)) {
            files = list.toList();
        } catch (NoSuchFileException e) {
            return; // the first put creates it
        } catch (IOException e) {
            System.err.println("solutions: cannot read " + dir + ": " + e);
            return;
        }
        for (Path file : files) {
            String fileName = file.getFileName().toString();
            if (fileName.endsWith(".tmp")) {
                delete(file); // a write cut off by a crash
            } else if (fileName.endsWith(".json")) {
                try {
                    Solution solution = mapper.readValue(file.toFile(), Solution.class);
                    long     bytes    = solution.bytes();
                    solutions.put(key(solution.name()), solution);
                    totalBytes += bytes;
                } catch (IOException | RuntimeException broken) {
                    // cut off by a crash or a full disk, or fields missing; the other files are still good
                }
            }
        }
    }

    private void write(Solution solution) {
        Path file = file(solution.name());
        Path tmp  = dir.resolve(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(dir);
            Files.writeString(tmp, mapper.writeValueAsString(solution), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            System.err.println("solutions: cannot write " + file + ": " + e);
        }
    }

    private static void delete(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            System.err.println("solutions: cannot delete " + file + ": " + e);
        }
    }

    private Path file(String name) {
        return dir.resolve(key(name) + ".json");
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
