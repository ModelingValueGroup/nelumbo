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

package org.modelingvalue.nelumbo.lsp.documentService;

import java.util.List;

import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.modelingvalue.nelumbo.lsp.NlDocumentManager;

public class DocumentSyncService extends DocumentServiceAdapter {
    public DocumentSyncService(NlDocumentManager documentManager) {
        super(documentManager);
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        TextDocumentItem textDocument = params.getTextDocument();
        add(textDocument.getText(), textDocument.getUri(), textDocument.getVersion());
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        TextDocumentIdentifier               textDocument   = params.getTextDocument();
        List<TextDocumentContentChangeEvent> contentChanges = params.getContentChanges();
        if (!contentChanges.isEmpty()) {
            if (1 < contentChanges.size()) {
                System.err.println("    multiple content changes not supported: " + contentChanges.size());
            }
            update(contentChanges.getFirst().getText(), textDocument.getUri());
        }
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        TextDocumentIdentifier textDocument = params.getTextDocument();
        documentManager.closeDocument(textDocument.getUri());
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        TextDocumentIdentifier textDocument = params.getTextDocument();
        update(params.getText(), textDocument.getUri());
    }

    private void add(String content, String uri, int version) {
        if (content != null && !content.isBlank()) {
            documentManager.addDocument(uri, content, version);
        }
    }

    private void update(String content, String uri) {
        if (content != null && !content.isBlank()) {
            documentManager.updateDocument(uri, content);
        }
    }
}
