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

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

public class NlDocumentManager {
    private final Workspace                             workspace;
    private final ConcurrentHashMap<String, NlDocument> documentCache = new ConcurrentHashMap<>();

    public NlDocumentManager(Workspace workspace) {
        this.workspace = workspace;
    }

    public Workspace workspace() {
        return workspace;
    }

    public void addDocument(String uri, String content, int version) {
        documentCache.put(uri, NlDocument.of(workspace, content, version, uri));
    }

    public void updateDocument(String uri, String content) {
        NlDocument document = getDocument(uri);
        if (document != null) {
            documentCache.put(uri, NlDocument.of(document, content));
        }
    }

    public NlDocument getDocument(String uri) {
        return documentCache.get(uri);
    }

    public void closeDocument(String uri) {
        documentCache.remove(uri);
    }

    public List<String> uris() {
        return documentCache.keySet().stream().toList();
    }
}
