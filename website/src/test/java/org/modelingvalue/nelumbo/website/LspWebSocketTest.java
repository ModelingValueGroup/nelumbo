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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.KnowledgeBase;

public class LspWebSocketTest {

    private static final String FIB_SEED = """
            import nelumbo.integers
            Integer ::= fib(<Integer>)
            Integer n, f
            fib(n)=f <=> f=n if n<=1, f=fib(n-1)+fib(n-2) if n>1
            """;

    private static NelumboHttpServer server;
    private static int               port;

    @BeforeAll
    static void start() {
        KnowledgeBase kb = KnowledgeBaseLoader.load(List.of(new NamedSource("seed.nl", FIB_SEED)));
        server = new NelumboHttpServer(kb, List.of("seed.nl"), 30_000, 2);
        port   = server.start(0);
    }

    @AfterAll
    static void stop() {
        server.stop();
    }

    /** Reassembles fragmented text frames and queues complete messages. */
    static final class Frames implements WebSocket.Listener {
        final BlockingQueue<String> messages    = new LinkedBlockingQueue<>();
        final CountDownLatch        closed      = new CountDownLatch(1);
        volatile int                closeStatus = -1;
        private final StringBuilder partial     = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            partial.append(data);
            if (last) {
                messages.add(partial.toString());
                partial.setLength(0);
            }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            closeStatus = statusCode;
            closed.countDown();
            return null;
        }

        String await(String needle, long seconds) throws InterruptedException {
            long deadline = System.nanoTime() + seconds * 1_000_000_000L;
            while (System.nanoTime() < deadline) {
                String m = messages.poll(250, TimeUnit.MILLISECONDS);
                if (m != null && m.contains(needle)) {
                    return m;
                }
            }
            return null;
        }
    }

    private static WebSocket connect(Frames frames) throws Exception {
        return HttpClient.newHttpClient().newWebSocketBuilder()
                .buildAsync(URI.create("ws://localhost:" + port + "/lsp"), frames)
                .get(10, TimeUnit.SECONDS);
    }

    @Test
    public void initializeDidOpenDiagnosticsAndInlayHints() throws Exception {
        Frames    frames = new Frames();
        WebSocket ws     = connect(frames);

        ws.sendText("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"processId\":null,\"rootUri\":null,\"capabilities\":{}}}", true).join();
        assertNotNull(frames.await("\"id\":1", 15), "initialize response expected");

        ws.sendText("{\"jsonrpc\":\"2.0\",\"method\":\"initialized\",\"params\":{}}", true).join();
        String doc = "Integer r\\nfib(5)=r ?\\n";
        ws.sendText("{\"jsonrpc\":\"2.0\",\"method\":\"textDocument/didOpen\",\"params\":{\"textDocument\":{\"uri\":\"inmemory://field-1.nl\",\"languageId\":\"nelumbo\",\"version\":1,\"text\":\"" + doc + "\"}}}", true).join();

        assertNotNull(frames.await("textDocument/publishDiagnostics", 15), "diagnostics expected after didOpen");

        // inlay hints carry the auto-evaluated query result; poll until the debounced evaluation lands
        String hints = null;
        for (int attempt = 2; attempt < 30 && hints == null; attempt++) {
            ws.sendText("{\"jsonrpc\":\"2.0\",\"id\":" + attempt + ",\"method\":\"textDocument/inlayHint\",\"params\":{\"textDocument\":{\"uri\":\"inmemory://field-1.nl\"},\"range\":{\"start\":{\"line\":0,\"character\":0},\"end\":{\"line\":10,\"character\":0}}}}", true).join();
            String response = frames.await("\"id\":" + attempt, 5);
            if (response != null && response.contains("r\\u003d5")) {
                hints = response;
            } else {
                Thread.sleep(500);
            }
        }
        assertNotNull(hints, "inlay hint with the fib(5) result expected (KB seeding)");
        ws.sendText("{\"jsonrpc\":\"2.0\",\"id\":99,\"method\":\"shutdown\",\"params\":null}", true).join();
        ws.abort();
    }

    private static final String INITIALIZE = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"processId\":null,\"rootUri\":null,\"capabilities\":{}}}";

    @Test
    public void closingASessionReleasesItsSlot() throws Exception {
        // cap of 1: the second connect can only succeed once the first slot is released on close
        NelumboHttpServer capped     = new NelumboHttpServer(KnowledgeBase.BASE, List.of(), 30_000, 1);
        int               cappedPort = capped.start(0);
        try {
            Frames    a  = new Frames();
            WebSocket wsA = HttpClient.newHttpClient().newWebSocketBuilder()
                    .buildAsync(URI.create("ws://localhost:" + cappedPort + "/lsp"), a).get(10, TimeUnit.SECONDS);
            wsA.sendText(INITIALIZE, true).join();
            assertNotNull(a.await("\"id\":1", 15), "first initialize response expected");

            // clean shutdown then normal close; the server frees the slot asynchronously
            wsA.sendText("{\"jsonrpc\":\"2.0\",\"id\":99,\"method\":\"shutdown\",\"params\":null}", true).join();
            wsA.sendClose(WebSocket.NORMAL_CLOSURE, "bye").join();

            // slot release is async: poll the second connect for up to ~10s until it succeeds
            String initialized = null;
            long   deadline    = System.nanoTime() + 10_000_000_000L;
            while (initialized == null && System.nanoTime() < deadline) {
                Frames b = new Frames();
                WebSocket wsB = HttpClient.newHttpClient().newWebSocketBuilder()
                        .buildAsync(URI.create("ws://localhost:" + cappedPort + "/lsp"), b).get(10, TimeUnit.SECONDS);
                wsB.sendText(INITIALIZE, true).join();
                initialized = b.await("\"id\":1", 2);
                if (initialized == null) {
                    wsB.abort();
                    Thread.sleep(250);
                } else {
                    wsB.abort();
                }
            }
            assertNotNull(initialized, "second session must succeed after the first releases its slot");
        } finally {
            capped.stop();
        }
    }

    @Test
    public void garbageJsonDropsTheSession() throws Exception {
        NelumboHttpServer capped     = new NelumboHttpServer(KnowledgeBase.BASE, List.of(), 30_000, 2);
        int               cappedPort = capped.start(0);
        try {
            Frames    frames = new Frames();
            WebSocket ws     = HttpClient.newHttpClient().newWebSocketBuilder()
                    .buildAsync(URI.create("ws://localhost:" + cappedPort + "/lsp"), frames).get(10, TimeUnit.SECONDS);
            ws.sendText("not json at all", true).join();
            assertTrue(frames.closed.await(5, TimeUnit.SECONDS), "session must be closed on garbage input");
            assertEquals(LspWebSocket.CLOSE_PROTOCOL_ERROR, frames.closeStatus);
            ws.abort();
        } finally {
            capped.stop();
        }
    }

    @Test
    public void sessionCapRejectsExcessConnections() throws Exception {
        // own server instance: the other test's sessions must not influence the cap
        NelumboHttpServer capped     = new NelumboHttpServer(KnowledgeBase.BASE, List.of(), 30_000, 1);
        int               cappedPort = capped.start(0);
        try {
            Frames     a    = new Frames();
            Frames     b    = new Frames();
            HttpClient http = HttpClient.newHttpClient();
            WebSocket wsA = http.newWebSocketBuilder().buildAsync(URI.create("ws://localhost:" + cappedPort + "/lsp"), a).get(10, TimeUnit.SECONDS);
            WebSocket wsB = http.newWebSocketBuilder().buildAsync(URI.create("ws://localhost:" + cappedPort + "/lsp"), b).get(10, TimeUnit.SECONDS);
            assertTrue(b.closed.await(10, TimeUnit.SECONDS), "second session must be rejected (cap is 1)");
            assertEquals(1013, b.closeStatus);
            wsA.abort();
            wsB.abort();
        } finally {
            capped.stop();
        }
    }
}
