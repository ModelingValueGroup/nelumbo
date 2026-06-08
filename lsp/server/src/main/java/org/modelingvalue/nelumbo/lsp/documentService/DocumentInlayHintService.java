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
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.InlayHintParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.modelingvalue.nelumbo.lsp.NlDocumentManager;

/**
 * Serves the inline query-result inlay hints. Results are computed (debounced) by
 * {@code QueryResultCache} after edits; this just hands back the cached hints inside the
 * requested range.
 */
public class DocumentInlayHintService extends DocumentServiceAdapter {

    public DocumentInlayHintService(NlDocumentManager documentManager) {
        super(documentManager);
    }

    @Override
    public CompletableFuture<List<InlayHint>> inlayHint(InlayHintParams params) {
        String          uri   = params.getTextDocument().getUri();
        Range           range = params.getRange();
        List<InlayHint> hints = documentManager.queryResultCache().hints(uri).stream()//
                                               .filter(h -> within(h.getPosition(), range))//
                                               .toList();
        return CompletableFuture.completedFuture(hints);
    }

    private static boolean within(Position p, Range range) {
        return range == null || (!before(p, range.getStart()) && !before(range.getEnd(), p));
    }

    private static boolean before(Position a, Position b) {
        return a.getLine() < b.getLine() || (a.getLine() == b.getLine() && a.getCharacter() < b.getCharacter());
    }
}
