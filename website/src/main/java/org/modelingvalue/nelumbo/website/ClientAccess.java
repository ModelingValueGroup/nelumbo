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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/** Decides the client IP (the proxy's forwarded address counts only with the shared secret) and who is admin. */
final class ClientAccess {

    static final String PROXY_HEADER = "X-Nelumbo-Proxy";

    private record Range(byte[] network, int prefix) {

        boolean contains(InetAddress address) {
            byte[] bytes = address.getAddress();
            if (bytes.length != network.length) {
                return false;
            }
            for (int i = 0; i < prefix; i++) {
                int mask = 0x80 >> (i % 8);
                if ((bytes[i / 8] & mask) != (network[i / 8] & mask)) {
                    return false;
                }
            }
            return true;
        }
    }

    private static final String OCTET = "(25[0-5]|2[0-4]\\d|1?\\d?\\d)";
    private static final String IPV4  = OCTET + "(\\." + OCTET + "){3}";
    private static final String IPV6  = "[0-9A-Fa-f:][0-9A-Fa-f:.]*";

    private final List<Range>   ranges;
    private final String        proxySecret;
    private final AtomicBoolean warned = new AtomicBoolean();

    private ClientAccess(List<Range> ranges, String proxySecret) {
        this.ranges = ranges;
        this.proxySecret = proxySecret;
    }

    private static byte[] maskedNetwork(byte[] bytes, int prefix) {
        byte[] masked = bytes.clone();
        for (int i = prefix; i < masked.length * 8; i++) {
            masked[i / 8] &= (byte) ~(0x80 >> (i % 8));
        }
        return masked;
    }

    static ClientAccess of(String adminIps, String proxySecret) {
        String      secret = proxySecret == null || proxySecret.isBlank() ? null : proxySecret;
        List<Range> ranges = new ArrayList<>();
        if (adminIps != null && !adminIps.isBlank()) {
            if (secret == null) {
                System.err.println("admin: ADMIN_IPS is set without PROXY_SECRET; behind Traefik no client address will match");
            }
            for (String entry : adminIps.split(",")) {
                String stripped = entry.strip();
                if (!stripped.isEmpty()) {
                    String reason = addRange(stripped, ranges);
                    if (reason != null) {
                        System.err.println("admin: skipping ADMIN_IPS entry \"" + stripped + "\": " + reason);
                    }
                }
            }
        }
        return new ClientAccess(List.copyOf(ranges), secret);
    }

    // returns the reason the entry is skipped, or null when it was added
    private static String addRange(String entry, List<Range> ranges) {
        int         slash   = entry.indexOf('/');
        String      literal = slash < 0 ? entry : entry.substring(0, slash);
        InetAddress address = parse(literal);
        if (address == null) {
            return "not an IP address";
        }
        int bits = address.getAddress().length * 8;
        int prefix;
        if (slash < 0) {
            prefix = bits;
        } else {
            try {
                prefix = Integer.parseInt(entry.substring(slash + 1));
            } catch (NumberFormatException e) {
                return "bad prefix length";
            }
            if (prefix < 0 || prefix > bits) {
                return "bad prefix length";
            }
        }
        if (prefix < (bits == 32 ? 16 : 32)) {
            return "range too wide";
        }
        byte[] bytes = maskedNetwork(address.getAddress(), prefix);
        InetAddress network;
        try {
            network = InetAddress.getByAddress(bytes);
        } catch (UnknownHostException e) {
            return "not an IP address";
        }
        if (network.isLoopbackAddress() || network.isAnyLocalAddress() || network.isSiteLocalAddress() || network.isLinkLocalAddress()
                || bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc) {
            return "loopback or private address";
        }
        ranges.add(new Range(bytes, prefix));
        return null;
    }

    // null unless a literal address: getByName would resolve host names
    private static InetAddress parse(String text) {
        String  address = text.strip().replaceAll("^\\[|\\]$|%.*$", "");
        boolean literal = address.contains(":") ? address.matches(IPV6) : address.matches(IPV4);
        if (!literal) {
            return null;
        }
        try {
            return InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    String clientIp(String forwardedFor, String proxyHeader, String remoteIp) {
        if (forwardedFor == null) {
            return remoteIp;
        }
        if (proxySecret == null || proxyHeader == null || !secretMatches(proxyHeader)) {
            if (!warned.getAndSet(true)) {
                System.err.println("X-Forwarded-For ignored: no valid X-Nelumbo-Proxy header (set PROXY_SECRET in .env)");
            }
            return remoteIp;
        }
        String last = forwardedFor.substring(forwardedFor.lastIndexOf(',') + 1).strip();
        return last.isEmpty() ? remoteIp : last;
    }

    private boolean secretMatches(String proxyHeader) {
        return MessageDigest.isEqual(proxySecret.getBytes(StandardCharsets.UTF_8), proxyHeader.getBytes(StandardCharsets.UTF_8));
    }

    // a loopback client counts only for a local host name: a rebound domain must not reach the admin
    boolean isAdmin(String clientIp, String host) {
        InetAddress address = clientIp == null ? null : parse(clientIp);
        if (address == null) {
            return false;
        }
        return address.isLoopbackAddress() && localHost(host) || ranges.stream().anyMatch(r -> r.contains(address));
    }

    private static boolean localHost(String host) {
        if (host == null) {
            return false;
        }
        String name = host.strip().toLowerCase(Locale.ROOT).replaceAll("^(\\[[^\\]]*\\]|[^:]*):\\d+$", "$1").replaceAll("^\\[|\\]$", "");
        return name.equals("localhost") || name.equals("127.0.0.1") || name.equals("::1");
    }
}
