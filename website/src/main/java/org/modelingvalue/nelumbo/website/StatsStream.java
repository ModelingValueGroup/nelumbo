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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import io.javalin.http.sse.SseClient;

/** The /stats/stream clients: every sample goes to all of them; a client that fails a send is dropped. */
final class StatsStream {
    static final int MAX_CLIENTS = 50;

    /** The part of a Javalin {@link SseClient} the stream uses, so it can be tested without a server. */
    interface Client {
        void send(String event, String data);

        void close();

        void onClose(Runnable runnable);

        static Client of(SseClient sse) {
            return new Client() {
                @Override
                public void send(String event, String data) {
                    sse.sendEvent(event, data);
                }

                @Override
                public void close() {
                    sse.close();
                }

                @Override
                public void onClose(Runnable runnable) {
                    sse.onClose(runnable);
                }
            };
        }
    }

    private static final class Slot {
        final Client        client;
        final AtomicBoolean busy = new AtomicBoolean();

        Slot(Client client) {
            this.client = client;
        }
    }

    private final Set<Slot>      slots    = ConcurrentHashMap.newKeySet();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    void connect(Client client, String latestJson) {
        if (slots.size() >= MAX_CLIENTS) {
            client.send("error", "too many stream clients");
            client.close();
            return;
        }
        Slot slot = new Slot(client);
        client.onClose(() -> slots.remove(slot));
        slots.add(slot);
        if (latestJson != null) {
            client.send("stats", latestJson);
        }
    }

    // sends run on virtual threads so a stalled client cannot block the sampler; a client still busy skips this sample
    void broadcast(String json) {
        for (Slot slot : slots) {
            if (slot.busy.compareAndSet(false, true)) {
                try {
                    executor.execute(() -> {
                        try {
                            slot.client.send("stats", json);
                        } catch (RuntimeException e) {
                            // defensive: real Javalin clients are removed through onClose
                            slots.remove(slot);
                        } finally {
                            slot.busy.set(false);
                        }
                    });
                } catch (RejectedExecutionException e) {
                    slot.busy.set(false);
                    return;
                }
            }
        }
    }

    int clients() {
        return slots.size();
    }

    void close() {
        executor.shutdownNow();
    }
}
