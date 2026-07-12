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

package org.modelingvalue.nelumbo.http;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.javalin.websocket.WsConfig;
import org.modelingvalue.nelumbo.KnowledgeBase;

/** The /lsp endpoint: one embedded language server per WebSocket connection, with public-deployment guards. */
final class LspWebSocket {
    static final int      CLOSE_TOO_MANY_SESSIONS = 1013;
    static final int      CLOSE_MESSAGE_TOO_BIG   = 1009;
    static final int      CLOSE_PROTOCOL_ERROR    = 1002;
    static final int      CLOSE_INTERNAL_ERROR    = 1011;
    static final int      MAX_MESSAGE_CHARS       = 64 * 1024;
    static final Duration IDLE_TIMEOUT            = Duration.ofMinutes(10);

    private final KnowledgeBase          baseKb;
    private final long                   evalDeadlineMs;
    private final int                    maxSessions;
    private final Map<String, LspBridge> sessions     = new ConcurrentHashMap<>();
    // counts accepted sessions (incremented before bridge creation, decremented on close/error)
    private final AtomicInteger          sessionCount = new AtomicInteger(0);

    LspWebSocket(KnowledgeBase baseKb, long evalDeadlineMs, int maxSessions) {
        this.baseKb         = baseKb;
        this.evalDeadlineMs = evalDeadlineMs;
        this.maxSessions    = maxSessions;
    }

    void configure(WsConfig ws) {
        ws.onConnect(ctx -> {
            // atomically claim a slot: increment only if under the cap, otherwise reject
            int count = sessionCount.getAndUpdate(n -> n < maxSessions ? n + 1 : n);
            if (count >= maxSessions) {
                ctx.closeSession(CLOSE_TOO_MANY_SESSIONS, "too many LSP sessions, try again later");
                return;
            }
            ctx.session.setIdleTimeout(IDLE_TIMEOUT);
            // slot is claimed above; if the bridge fails to build, release it here so the cap never leaks
            LspBridge bridge;
            try {
                bridge = new LspBridge(baseKb, evalDeadlineMs, ctx);
            } catch (RuntimeException e) {
                sessionCount.decrementAndGet();
                System.err.println("LSP bridge init failed: " + e);
                ctx.closeSession(CLOSE_INTERNAL_ERROR, "LSP init failed");
                return;
            }
            sessions.put(ctx.sessionId(), bridge);
        });
        ws.onMessage(ctx -> {
            LspBridge bridge = sessions.get(ctx.sessionId());
            if (bridge == null) {
                return; // session was rejected or bridge not yet ready
            }
            // defense-in-depth: Jetty already rejects oversized frames (maxTextMessageSize set in NelumboHttpServer)
            if (ctx.message().length() > MAX_MESSAGE_CHARS) {
                ctx.closeSession(CLOSE_MESSAGE_TOO_BIG, "message too large");
                return;
            }
            try {
                bridge.onMessage(ctx.message());
            } catch (RuntimeException e) {
                System.err.println("LSP session dropped on malformed message: " + e);
                ctx.closeSession(CLOSE_PROTOCOL_ERROR, "protocol error");
            }
        });
        ws.onClose(ctx -> {
            LspBridge bridge = sessions.remove(ctx.sessionId());
            if (bridge != null) {
                sessionCount.decrementAndGet();
                bridge.close();
            }
        });
        ws.onError(ctx -> {
            LspBridge bridge = sessions.remove(ctx.sessionId());
            if (bridge != null) {
                sessionCount.decrementAndGet();
                bridge.close();
            }
        });
    }
}
