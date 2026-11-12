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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClientAccessTest {

    @Test
    void forwardedForCountsOnlyWithTheSecret() {
        ClientAccess access = ClientAccess.of(null, "s3cret");
        assertEquals("5.6.7.8", access.clientIp("1.2.3.4, 5.6.7.8", "s3cret", "10.0.0.1"));
        assertEquals("10.0.0.1", access.clientIp("5.6.7.8", null, "10.0.0.1"));
        assertEquals("10.0.0.1", access.clientIp("5.6.7.8", "wrong", "10.0.0.1"));
        assertEquals("10.0.0.1", access.clientIp(" ", "s3cret", "10.0.0.1"));
        assertEquals("10.0.0.1", ClientAccess.of(null, null).clientIp("5.6.7.8", "", "10.0.0.1"));
    }

    @Test
    void loopbackIsAdminWithoutAnyConfiguration() {
        ClientAccess access = ClientAccess.of(null, null);
        assertTrue(access.isAdmin("127.0.0.1", "localhost:8080"));
        assertTrue(access.isAdmin("::1", "localhost:8080"));
        assertFalse(access.isAdmin("192.0.2.10", "localhost:8080"));
        assertFalse(access.isAdmin("localhost", "localhost:8080"));
    }

    @Test
    void aLoopbackClientNeedsALocalHostName() {
        ClientAccess access = ClientAccess.of("192.0.2.10", "s");
        assertTrue(access.isAdmin("127.0.0.1", "localhost:8080"));
        assertTrue(access.isAdmin("::1", "[::1]:8899"));
        assertTrue(access.isAdmin("127.0.0.1", "127.0.0.1"));
        assertFalse(access.isAdmin("127.0.0.1", "evil.example"));
        assertFalse(access.isAdmin("127.0.0.1", null));
        assertTrue(access.isAdmin("192.0.2.10", "nelumbo.nl"));
    }

    @Test
    void addressesAndRangesMatch() {
        ClientAccess access = ClientAccess.of("192.0.2.10, 2001:db8:1234::/48", "s");
        assertTrue(access.isAdmin("192.0.2.10", "localhost:8080"));
        assertFalse(access.isAdmin("192.0.2.11", "localhost:8080"));
        assertTrue(access.isAdmin("2001:db8:1234:5:6:7:8:9", "localhost:8080"));
        assertFalse(access.isAdmin("2001:db8:1235::1", "localhost:8080"));
        assertTrue(access.isAdmin("::ffff:192.0.2.10", "localhost:8080"));
    }

    @Test
    void peerAddressFormsAreNormalized() {
        ClientAccess access = ClientAccess.of("2001:db8::/32", "s");
        assertTrue(access.isAdmin("[0:0:0:0:0:0:0:1]", "localhost:8080"));
        assertTrue(access.isAdmin("0:0:0:0:0:0:0:1", "localhost:8080"));
        assertTrue(access.isAdmin("[2001:db8::5]", "localhost:8080"));
        assertTrue(access.isAdmin("2001:db8::5%eth0", "localhost:8080"));
    }

    @Test
    void badAndPrivateEntriesAreSkipped() {
        ClientAccess access = ClientAccess.of("nonsense, 10.1.2.3, 192.168.0.0/16, 172.17.0.1, fd00::1, fe80::/10, 1.2.3.4/33, example.com, 198.51.100.7", "s");
        assertFalse(access.isAdmin("10.1.2.3", "localhost:8080"));
        assertFalse(access.isAdmin("192.168.1.1", "localhost:8080"));
        assertFalse(access.isAdmin("172.17.0.1", "localhost:8080"));
        assertFalse(access.isAdmin("fd00::1", "localhost:8080"));
        assertTrue(access.isAdmin("198.51.100.7", "localhost:8080"));
    }

    @Test
    void tooWideRangesAreSkipped() {
        ClientAccess access = ClientAccess.of("0.0.0.0/0, ::/0, 1.0.0.0/1, 2000::/3, 203.0.0.0/15", "s");
        assertFalse(access.isAdmin("1.2.3.4", "localhost:8080"));
        assertFalse(access.isAdmin("2001:db8::1", "localhost:8080"));
        assertFalse(access.isAdmin("203.0.113.1", "localhost:8080"));
    }

    @Test
    void nonLiteralsNeverReachNameResolution() {
        ClientAccess access = ClientAccess.of("999.1.1.1, .:1", "s");
        assertFalse(access.isAdmin("999.1.1.1", "localhost:8080"));
        assertFalse(access.isAdmin(".:1", "localhost:8080"));
        assertFalse(access.isAdmin("300.1.2.3", "localhost:8080"));
    }

    @Test
    void privateNetworkAddressIsCheckedAfterMasking() {
        ClientAccess access = ClientAccess.of("0.0.0.1/16, ::2/32, fe80::/64, 127.0.0.1", "s");
        assertFalse(access.isAdmin("0.0.5.5", "localhost:8080"));
        assertFalse(access.isAdmin("0:0:5::1", "localhost:8080"));
        assertFalse(access.isAdmin("fe80::1", "localhost:8080"));
    }

    @Test
    void widthLimitsThemselvesAreAccepted() {
        ClientAccess access = ClientAccess.of("203.0.0.0/16, 2001:db8::/32", "s");
        assertTrue(access.isAdmin("203.0.5.5", "localhost:8080"));
        assertTrue(access.isAdmin("2001:db8:ffff::1", "localhost:8080"));
    }

    @Test
    void emptyLastForwardedEntryFallsBackToThePeer() {
        ClientAccess access = ClientAccess.of(null, "s");
        assertEquals("10.0.0.1", access.clientIp(",", "s", "10.0.0.1"));
        assertEquals("10.0.0.1", access.clientIp("1.2.3.4,", "s", "10.0.0.1"));
    }
}
