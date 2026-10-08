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
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;

class StatsStreamTest {
    private static class FakeClient implements StatsStream.Client {
        final List<String> events = new CopyOnWriteArrayList<>();
        volatile boolean   closed;
        volatile boolean   failing;
        Runnable           onClose;

        @Override
        public void send(String event, String data) {
            if (failing) {
                throw new IllegalStateException("broken pipe");
            }
            events.add(event + ":" + data);
        }

        @Override
        public void close() {
            closed = true;
        }

        @Override
        public void onClose(Runnable runnable) {
            onClose = runnable;
        }
    }

    @Test
    void aNewClientGetsTheLatestSampleAndLaterBroadcasts() {
        StatsStream stream = new StatsStream();
        FakeClient  client = new FakeClient();
        stream.connect(client, "{\"a\":1}");
        stream.broadcast("{\"a\":2}");
        awaitEvents(client, 2);
        assertEquals(List.of("stats:{\"a\":1}", "stats:{\"a\":2}"), client.events);
        stream.close();
    }

    @Test
    void clientsOverTheCapGetAnErrorAndAreClosed() {
        StatsStream stream = new StatsStream();
        for (int i = 0; i < StatsStream.MAX_CLIENTS; i++) {
            stream.connect(new FakeClient(), null);
        }
        FakeClient extra = new FakeClient();
        stream.connect(extra, null);
        assertEquals(List.of("error:too many stream clients"), extra.events);
        assertTrue(extra.closed);
        assertEquals(StatsStream.MAX_CLIENTS, stream.clients());
    }

    @Test
    void streamDropsAClientThatFails() {
        StatsStream stream = new StatsStream();
        FakeClient  good   = new FakeClient();
        FakeClient  bad    = new FakeClient();
        stream.connect(good, null);
        stream.connect(bad, null);
        bad.failing = true;
        stream.broadcast("{}");
        awaitEvents(good, 1);
        awaitClients(stream, 1);
        assertEquals(List.of("stats:{}"), good.events);
        stream.close();
    }

    @Test
    void aStalledClientDoesNotBlockBroadcastOrOtherClients() throws Exception {
        StatsStream    stream  = new StatsStream();
        CountDownLatch release = new CountDownLatch(1);
        FakeClient     stuck   = new FakeClient() {
                                   @Override
                                   public void send(String event, String data) {
                                       try {
                                           release.await();
                                       } catch (InterruptedException e) {
                                           Thread.currentThread().interrupt();
                                       }
                                   }
                               };
        FakeClient     good    = new FakeClient();
        stream.connect(stuck, null);
        stream.connect(good, null);
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> stream.broadcast("{\"n\":1}"));
        awaitEvents(good, 1);
        // good's slot may still be finishing the first send (busy resets after the event is recorded): retry, bounded
        long end = System.nanoTime() + 5_000_000_000L;
        while (!good.events.contains("stats:{\"n\":2}") && System.nanoTime() < end) {
            assertTimeoutPreemptively(Duration.ofSeconds(5), () -> stream.broadcast("{\"n\":2}"));
            Thread.onSpinWait();
        }
        assertTrue(good.events.contains("stats:{\"n\":2}"), "got " + good.events);
        release.countDown();
        stream.close();
    }

    private static void awaitEvents(FakeClient client, int count) {
        long end = System.nanoTime() + 5_000_000_000L;
        while (client.events.size() < count && System.nanoTime() < end) {
            Thread.onSpinWait();
        }
        assertEquals(count, client.events.size());
    }

    private static void awaitClients(StatsStream stream, int count) {
        long end = System.nanoTime() + 5_000_000_000L;
        while (stream.clients() != count && System.nanoTime() < end) {
            Thread.onSpinWait();
        }
        assertEquals(count, stream.clients());
    }

    @Test
    void aClosedClientIsRemoved() {
        StatsStream stream = new StatsStream();
        FakeClient  client = new FakeClient();
        stream.connect(client, null);
        client.onClose.run();
        assertEquals(0, stream.clients());
    }
}
