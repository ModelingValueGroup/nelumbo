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

package org.modelingvalue.nelumbo.lsp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.eclipse.lsp4j.InlayHint;
import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.KnowledgeBase;

public class EmbeddedServerTest {

    @Test
    public void embeddedConstructorSeedsWorkspace() {
        KnowledgeBase         kb     = KnowledgeBase.BASE.run(() -> {
        });
        NelumboLanguageServer server = new NelumboLanguageServer(kb, 1234, () -> {
        });
        assertSame(kb, server.getWorkspace().getBaseKnowledgeBase());
        assertEquals(1234, server.getWorkspace().getEvalDeadlineMs());
    }

    @Test
    public void connectStoresClientPerInstance() {
        NelumboLanguageServer serverA = new NelumboLanguageServer(KnowledgeBase.BASE, 0, () -> {
        });
        NelumboLanguageServer serverB = new NelumboLanguageServer(KnowledgeBase.BASE, 0, () -> {
        });
        RecordingClient       clientA = new RecordingClient();
        RecordingClient       clientB = new RecordingClient();
        serverA.connect(clientA);
        serverB.connect(clientB);
        assertSame(clientA, serverA.getWorkspace().getClient());
        assertSame(clientB, serverB.getWorkspace().getClient());
    }

    @Test
    public void defaultConstructorUsesBase() {
        assertSame(KnowledgeBase.BASE, new NelumboLanguageServer().getWorkspace().getBaseKnowledgeBase());
        assertEquals(0, new NelumboLanguageServer().getWorkspace().getEvalDeadlineMs());
    }

    @Test
    public void diagnosticsGoToTheInstanceClient() throws InterruptedException {
        NelumboLanguageServer server = new NelumboLanguageServer(KnowledgeBase.BASE, 0, () -> {
        });
        RecordingClient       client = new RecordingClient();
        server.connect(client);
        try {
            server.getWorkspace().getDocumentManager().addDocument("inmemory://t.nl", "import nelumbo.logic\ntrue ?\n", 1);
            org.junit.jupiter.api.Assertions.assertTrue(client.awaitDiagnostics(10), "expected publishDiagnostics on the per-instance client");
            org.junit.jupiter.api.Assertions.assertTrue(client.awaitInlayHintRefresh(15), "expected refreshInlayHints after the debounced evaluation");
        } finally {
            server.getWorkspace().dispose();
        }
    }

    @Test
    public void inlayHintsCarryFullResultTooltips() throws InterruptedException {
        NelumboLanguageServer server = new NelumboLanguageServer(KnowledgeBase.BASE, 0, () -> {
        });
        RecordingClient       client = new RecordingClient();
        server.connect(client);
        try {
            server.getWorkspace().getDocumentManager().addDocument("inmemory://t.nl", "import nelumbo.logic\ntrue ?\n", 1);
            org.junit.jupiter.api.Assertions.assertTrue(client.awaitInlayHintRefresh(15), "expected refreshInlayHints after the debounced evaluation");
            List<InlayHint> hints = server.getWorkspace().getDocumentManager().queryResultCache().hints("inmemory://t.nl");
            assertEquals(1, hints.size(), "one hint for the single query");
            assertNotNull(hints.get(0).getTooltip(), "the hint carries a tooltip");
            assertEquals("[()][]", hints.get(0).getTooltip().getLeft(), "the tooltip is the full inferred result");
        } finally {
            server.getWorkspace().dispose();
        }
    }

    @Test
    public void documentCacheIsCappedPerSession() {
        NelumboLanguageServer server = new NelumboLanguageServer(KnowledgeBase.BASE, 0, () -> {
        });
        server.connect(new RecordingClient());
        try {
            NlDocumentManager dm = server.getWorkspace().getDocumentManager();
            for (int i = 0; i < NlDocumentManager.MAX_DOCUMENTS; i++) {
                dm.addDocument("inmemory://field-" + i + ".nl", "import nelumbo.logic\ntrue ?\n", 1);
            }
            assertNotNull(dm.getDocument("inmemory://field-0.nl"), "documents up to the cap are accepted");
            dm.addDocument("inmemory://overflow.nl", "import nelumbo.logic\ntrue ?\n", 1);
            assertNull(dm.getDocument("inmemory://overflow.nl"), "a new document past the cap is refused");
            assertEquals(NlDocumentManager.MAX_DOCUMENTS, dm.uris().size(), "the cap bounds the cache size");
        } finally {
            server.getWorkspace().dispose();
        }
    }
}
