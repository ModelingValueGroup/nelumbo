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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.lsp4j.Diagnostic;
import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.KnowledgeBase;

/**
 * The IDE already shows a diagnostic at its line in its file, so the message must not repeat the
 * location (ParseException.getMessage() appends ", line=.., position=.., file=..").
 */
public class DiagnosticMessageTest {
    @Test
    public void parseErrorMessageDoesNotRepeatTheLocation() throws Exception {
        NelumboLanguageServer server = new NelumboLanguageServer(KnowledgeBase.BASE, 0, () -> {
        });
        RecordingClient client = new RecordingClient();
        server.connect(client);
        try {
            server.getWorkspace().getDocumentManager().addDocument("inmemory://bad.nl", "import nelumbo.logic\nflurb @@ blarg\n", 1);
            assertTrue(client.awaitDiagnostics(10), "expected publishDiagnostics");
            List<Diagnostic> all = client.diagnostics.stream().flatMap(p -> p.getDiagnostics().stream()).toList();
            assertFalse(all.isEmpty(), "the parse error is reported");
            for (Diagnostic d : all) {
                String message = d.getMessage().isLeft() ? d.getMessage().getLeft() : d.getMessage().getRight().getValue();
                assertFalse(message.contains("line=") || message.contains("file="), message);
            }
        } finally {
            server.getWorkspace().dispose();
        }
    }
}
