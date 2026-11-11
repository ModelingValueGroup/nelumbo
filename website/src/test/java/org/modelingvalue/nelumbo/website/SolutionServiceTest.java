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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class SolutionServiceTest {

    private final AtomicLong now     = new AtomicLong(1_000_000_000_000L);
    private SolutionService  service = service(SolutionService.MAX_TOTAL_BYTES);

    private SolutionService service(long max) {
        return new SolutionService(new SolutionStore(null, now::get), new BlockedWords(List.of("kut")), now::get, max);
    }

    @Test
    void aSavedSolutionLoadsByName() {
        assertEquals(new SolutionService.Response(200, "Shared as \"Frits\" (kept 7 days)"), service.save("Frits", "fact p(A)", "b1", "1.1.1.1"));
        assertEquals(new SolutionService.Response(200, "fact p(A)"), service.load("FRITS"));
    }

    @Test
    void onlyTheFirstBrowserMayUpdate() {
        service.save("Frits", "v1", "b1", "1.1.1.1");
        assertEquals(new SolutionService.Response(409, "\"frits\" is taken"), service.save("frits", "v2", "b2", "1.1.1.1"));
        assertEquals(200, service.save("Frits", "v3", "b1", "1.1.1.1").status());
        assertEquals("v3", service.load("Frits").body());
    }

    @Test
    void badNamesAreRefused() {
        for (String name : List.of("", "a b", "Zoë", "../etc", "a/b", "x".repeat(41))) {
            assertEquals(new SolutionService.Response(400, "Use only letters a-z, digits and hyphens"), service.save(name, "x", "b1", "1.1.1.1"), name);
        }
        assertEquals(200, service.save("x".repeat(40), "x", "b1", "1.1.1.1").status());
        assertEquals(400, service.load("../etc").status());
    }

    @Test
    void aSaveNeedsABrowserId() {
        assertEquals(new SolutionService.Response(400, "Missing browser id"), service.save("Frits", "x", null, "1.1.1.1"));
        assertEquals(400, service.save("Frits", "x", " ", "1.1.1.1").status());
    }

    @Test
    void theSizeIsMeasuredJsonEscaped() {
        assertEquals(200, service.save("a", "a".repeat(32 * 1024), "b1", "1.1.1.1").status());
        assertEquals(new SolutionService.Response(413, "Not shared: too large (max 32 KB)"), service.save("b", "a".repeat(32 * 1024 + 1), "b1", "1.1.1.1"));
        assertEquals(413, service.save("c", "\n".repeat(16 * 1024 + 1), "b1", "1.1.1.1").status());
    }

    @Test
    void blockedWordsAreRefusedInNameAndText() {
        assertEquals(new SolutionService.Response(422, "Not shared: contains a blocked word"), service.save("Kut", "x", "b1", "1.1.1.1"));
        assertEquals(422, service.save("Frits", "fact kut(A)", "b1", "1.1.1.1").status());
    }

    @Test
    void aBrowserSavesThirtyTimesPerHour() {
        for (int i = 0; i < 30; i++) {
            assertEquals(200, service.save("n" + i, "x", "b1", "1.1.1." + i).status());
        }
        assertEquals(new SolutionService.Response(429, "Too many shares, try again later"), service.save("n30", "x", "b1", "1.1.1.99"));
        now.addAndGet(3_600_000L);
        assertEquals(200, service.save("n30", "x", "b1", "1.1.1.99").status());
    }

    @Test
    void anIpCreatesTwoHundredNewNamesPerHourButMayUpdate() {
        for (int i = 0; i < 200; i++) {
            assertEquals(200, service.save("n" + i, "x", "b" + i, "1.1.1.1").status());
        }
        assertEquals(429, service.save("n200", "x", "b200", "1.1.1.1").status());
        assertEquals(200, service.save("n0", "y", "b0", "1.1.1.1").status());
    }

    @Test
    void growthCountsAgainstTheTotal() {
        service = service(Solution.ENTRY_BYTES + 10);
        assertEquals(200, service.save("a", "12345", "b1", "1.1.1.1").status());
        assertEquals(new SolutionService.Response(503, "Not shared: the server is full, try again later"), service.save("b", "123456", "b2", "1.1.1.1"));
        assertEquals(200, service.save("a", "1234567890", "b1", "1.1.1.1").status());
        assertEquals(503, service.save("b", "1", "b2", "1.1.1.1").status());
        assertEquals(200, service.save("a", "0987654321", "b1", "1.1.1.1").status());
        assertEquals(200, service.save("a", "123", "b1", "1.1.1.1").status());
    }

    @Test
    void aFullStoreRefusesANewEmptyText() {
        service = service(0);
        assertEquals(503, service.save("a", "", "b1", "1.1.1.1").status());
    }

    @Test
    void usageCountsTheSolutionsTheirBytesAndTheSaves() {
        service.save("a", "x", "b1", "1.1.1.1");
        service.save("b", "yy", "b2", "1.1.1.1");
        service.save("a", "xyz", "b1", "1.1.1.1");
        service.save("a", "nope", "b2", "1.1.1.1");
        assertEquals(new SolutionService.Usage(2, 2 * Solution.ENTRY_BYTES + 5, 3), service.usage());
    }

    @Test
    void expiredSolutionsLeaveTheUsageWithinAnHour() {
        service.save("a", "x", "b1", "1.1.1.1");
        now.addAndGet(SolutionStore.KEEP_MS + 3_600_000L);
        assertEquals(new SolutionService.Usage(0, 0, 1), service.usage());
    }

    @Test
    void anExpiredNameCanBeClaimed() {
        service.save("Frits", "v1", "b1", "1.1.1.1");
        now.addAndGet(SolutionStore.KEEP_MS + 1);
        assertEquals(200, service.save("Frits", "v2", "b2", "1.1.1.1").status());
    }

    @Test
    void anUnknownNameIsNotFound() {
        assertEquals(new SolutionService.Response(404, "No shared solution named \"Frits\""), service.load("Frits"));
    }

    @Test
    void anIpv6NetworkSharesOneNewNameBudget() {
        for (int i = 0; i < 200; i++) {
            assertEquals(200, service.save("n" + i, "x", "b" + i, "2001:db8:1:2::" + Integer.toHexString(i + 1)).status());
        }
        assertEquals(429, service.save("n200", "x", "b200", "2001:db8:1:2:ffff::1").status());
        assertEquals(200, service.save("n201", "x", "b201", "2001:db8:1:3::1").status());
    }

    @Test
    void theIpKeyOfAnIpv6AddressIsItsNetwork() {
        assertEquals("2001:db8:1:2", SolutionService.ipKey("2001:0db8:1:2::1"));
        assertEquals("2001:db8:1:2", SolutionService.ipKey("[2001:db8:1:2:3:4:5:6]"));
        assertEquals("2001:db8:0:0", SolutionService.ipKey("2001:db8::"));
        assertEquals("0:0:0:0", SolutionService.ipKey("::1"));
        assertEquals("fe80:0:0:0", SolutionService.ipKey("fe80::1%eth0"));
        assertEquals("1.2.3.4", SolutionService.ipKey("::ffff:1.2.3.4"));
        assertEquals("1.2.3.4", SolutionService.ipKey("1.2.3.4"));
        assertEquals("no:ip", SolutionService.ipKey("no:ip"));
    }

    @Test
    void clientIpIsTheLastForwardedEntry() {
        assertEquals("10.0.0.1", SolutionService.clientIp(null, "10.0.0.1"));
        assertEquals("10.0.0.1", SolutionService.clientIp(" ", "10.0.0.1"));
        assertEquals("5.6.7.8", SolutionService.clientIp("1.2.3.4, 5.6.7.8", "10.0.0.1"));
        assertEquals("9.9.9.9", SolutionService.clientIp(" 9.9.9.9 ", "10.0.0.1"));
    }

    @Test
    void concurrentSavesOfOneNameHaveOneOwner() throws Exception {
        List<Callable<Integer>> saves = IntStream.range(0, 20).<Callable<Integer>> mapToObj(i -> () -> service.save("Frits", "x" + i, "b" + i, "1.1.1." + i).status()).toList();
        try (ExecutorService pool = Executors.newFixedThreadPool(20)) {
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> f : pool.invokeAll(saves)) {
                statuses.add(f.get());
            }
            assertEquals(1, statuses.stream().filter(s -> s == 200).count());
            assertEquals(19, statuses.stream().filter(s -> s == 409).count());
        }
    }
}
