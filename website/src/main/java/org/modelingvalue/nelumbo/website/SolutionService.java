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

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The rules of {@code PUT/GET /solutions/{name}}: a status and a short message the editor shows as is. A name belongs
 * to the browser that saved it first (only the SHA-256 of its id is kept); the limits count successful saves in the
 * last hour, in memory only. The checks run in a fixed order, the first failure wins.
 */
final class SolutionService {

    record Response(int status, String body) {
    }

    /** For the status page: the stored solutions, their bytes (as counted against the cap) and the saves since start. */
    record Usage(int solutions, long bytes, long saves) {
    }

    static final int  MAX_NAME_CHARS             = 40;
    // JSON-escaped, so a loaded text always fits a 64 K /lsp frame (LspWebSocket.MAX_MESSAGE_CHARS)
    static final int  MAX_TEXT_CHARS             = 32 * 1024;
    static final long MAX_TOTAL_BYTES            = 100L * 1024 * 1024;
    static final int  SAVES_PER_BROWSER_PER_HOUR = 30;
    static final int  NEW_NAMES_PER_IP_PER_HOUR  = 200;

    private static final long    HOUR_MS  = 3_600_000L;
    private static final Pattern NAME     = Pattern.compile("[A-Za-z0-9-]{1," + MAX_NAME_CHARS + "}");
    private static final String  BAD_NAME = "Use only letters a-z, digits and hyphens";

    private final SolutionStore store;
    private final BlockedWords  words;
    private final LongSupplier  clock;
    private final long          maxTotalBytes;
    private final ObjectMapper  mapper       = new ObjectMapper();
    private final HourlyCounter browserSaves = new HourlyCounter();
    private final HourlyCounter ipNewNames   = new HourlyCounter();
    private long                lastHousekeeping;
    private long                saves;

    SolutionService(SolutionStore store, BlockedWords words, LongSupplier clock, long maxTotalBytes) {
        this.store         = store;
        this.words         = words;
        this.clock         = clock;
        this.maxTotalBytes = maxTotalBytes;
    }

    synchronized Usage usage() {
        housekeep(clock.getAsLong());
        return new Usage(store.count(), store.totalBytes(), saves);
    }

    // at most once an hour: expired solutions leave the store, empty counters leave memory
    private void housekeep(long now) {
        if (now - lastHousekeeping >= HOUR_MS) {
            store.expire();
            browserSaves.prune(now);
            ipNewNames.prune(now);
            lastHousekeeping = now;
        }
    }

    synchronized Response save(String name, String text, String browserId, String ip) {
        long now = clock.getAsLong();
        housekeep(now);
        if (!NAME.matcher(name).matches()) {
            return new Response(400, BAD_NAME);
        }
        if (browserId == null || browserId.isBlank()) {
            return new Response(400, "Missing browser id");
        }
        if (escapedLength(text) > MAX_TEXT_CHARS) {
            return new Response(413, "Not shared: too large (max 32 KB)");
        }
        if (words.inName(name) || words.inText(text)) {
            return new Response(422, "Not shared: contains a blocked word");
        }
        String             owner = sha256(browserId);
        Optional<Solution> old   = store.get(name);
        if (old.isPresent() && !old.get().ownerHash().equals(owner)) {
            return new Response(409, "\"" + name + "\" is taken");
        }
        String network = ipKey(ip);
        if (browserSaves.count(owner, now) >= SAVES_PER_BROWSER_PER_HOUR || old.isEmpty() && ipNewNames.count(network, now) >= NEW_NAMES_PER_IP_PER_HOUR) {
            return new Response(429, "Too many shares, try again later");
        }
        Solution solution = new Solution(name, owner, now, text);
        long     growth   = solution.bytes() - old.map(Solution::bytes).orElse(0L);
        if (growth > 0 && store.totalBytes() + growth > maxTotalBytes) {
            return new Response(503, "Not shared: the server is full, try again later");
        }
        store.put(solution);
        saves++;
        browserSaves.add(owner, now);
        if (old.isEmpty()) {
            ipNewNames.add(network, now);
        }
        return new Response(200, "Shared as \"" + name + "\" (kept 7 days)");
    }

    Response load(String name) {
        if (!NAME.matcher(name).matches()) {
            return new Response(400, BAD_NAME);
        }
        return store.get(name).map(s -> new Response(200, s.text())).orElse(new Response(404, "No shared solution named \"" + name + "\""));
    }

    // Traefik appends the address it saw: earlier entries come from the client and can be forged
    static String clientIp(String forwardedFor, String remoteIp) {
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return remoteIp;
        }
        String[] entries = forwardedFor.split(",");
        return entries[entries.length - 1].strip();
    }

    // An IPv6 client usually has a whole /64 to pick addresses from: count per network (the first 4 groups).
    // An IPv4-mapped address counts as its IPv4 address; anything unparsable as itself.
    static String ipKey(String ip) {
        String address = ip.replaceAll("^\\[|\\]$|%.*$", "");
        if (!address.contains(":")) {
            return address;
        }
        if (address.contains(".")) {
            return address.substring(address.lastIndexOf(':') + 1);
        }
        try {
            String[]     halves = address.split("::", -1);
            List<String> groups = new ArrayList<>(groups(halves[0]));
            List<String> tail   = halves.length > 1 ? groups(halves[1]) : List.of();
            while (groups.size() + tail.size() < 8) {
                groups.add("0");
            }
            groups.addAll(tail);
            return groups.subList(0, 4).stream().map(g -> Integer.toHexString(Integer.parseInt(g, 16))).collect(Collectors.joining(":"));
        } catch (RuntimeException e) {
            return address;
        }
    }

    private static List<String> groups(String part) {
        return part.isEmpty() ? List.of() : List.of(part.split(":"));
    }

    private int escapedLength(String text) {
        try {
            return mapper.writeValueAsString(text).length() - 2;
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String sha256(String browserId) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(browserId.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Times of the successful saves per key within the last hour. */
    private static final class HourlyCounter {

        private final Map<String, ArrayDeque<Long>> times = new HashMap<>();

        int count(String key, long now) {
            ArrayDeque<Long> deque = times.get(key);
            if (deque == null) {
                return 0;
            }
            drop(deque, now);
            return deque.size();
        }

        void add(String key, long now) {
            times.computeIfAbsent(key, k -> new ArrayDeque<>()).addLast(now);
        }

        void prune(long now) {
            times.values().removeIf(deque -> {
                drop(deque, now);
                return deque.isEmpty();
            });
        }

        private static void drop(ArrayDeque<Long> deque, long now) {
            while (!deque.isEmpty() && now - deque.peekFirst() >= HOUR_MS) {
                deque.removeFirst();
            }
        }
    }
}
