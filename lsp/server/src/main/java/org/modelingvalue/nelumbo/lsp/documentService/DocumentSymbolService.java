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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.SymbolKind;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.modelingvalue.nelumbo.Fact;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Query;
import org.modelingvalue.nelumbo.Rule;
import org.modelingvalue.nelumbo.Transform;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.Variable;
import org.modelingvalue.nelumbo.lsp.NlDocument;
import org.modelingvalue.nelumbo.lsp.NlDocumentManager;
import org.modelingvalue.nelumbo.lsp.U;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.syntax.Token;

public class DocumentSymbolService extends DocumentServiceAdapter {

    public DocumentSymbolService(NlDocumentManager documentManager) {
        super(documentManager);
    }

    @Override
    public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(DocumentSymbolParams params) {
        NlDocument document = documentManager.getDocument(params.getTextDocument().getUri());
        if (document == null) {
            return CompletableFuture.completedFuture(null);
        }
        List<Either<SymbolInformation, DocumentSymbol>> result = new ArrayList<>();
        for (Node node : document.parserResult().roots()) {
            Token first = node.firstToken();
            Token last  = node.lastToken();
            if (first == null || last == null) {
                continue;
            }
            DocumentSymbol docSym = new DocumentSymbol();
            docSym.setRange(U.range(node));
            docSym.setSelectionRange(U.range(first));
            if (node instanceof Type type) {
                docSym.setName(type.name());
                docSym.setKind(SymbolKind.Class);
                StringBuilder detail = new StringBuilder();
                for (Type sup : type.supers()) {
                    if (!detail.isEmpty()) detail.append(", ");
                    detail.append(sup.name());
                }
                docSym.setDetail(detail.isEmpty() ? "type" : detail.toString());
            } else if (node instanceof Variable variable) {
                docSym.setName(variable.name());
                docSym.setKind(SymbolKind.Variable);
                docSym.setDetail(variable.type().name());
            } else if (node instanceof Functor functor) {
                docSym.setName(functor.name());
                docSym.setKind(SymbolKind.Function);
                docSym.setDetail(functor.resultType().name());
            } else if (node instanceof Rule) {
                docSym.setName(node.toString());
                docSym.setKind(SymbolKind.Method);
                docSym.setDetail("rule");
            } else if (node instanceof Fact fact) {
                docSym.setName(node.toString());
                docSym.setKind(SymbolKind.Constant);
                docSym.setDetail(fact.predicate().toString());
            } else if (node instanceof Query) {
                docSym.setName(node.toString());
                docSym.setKind(SymbolKind.Event);
                docSym.setDetail("query");
            } else if (node instanceof Transform) {
                docSym.setName(node.toString());
                docSym.setKind(SymbolKind.Operator);
                docSym.setDetail("transform");
            } else {
                docSym.setName(node.toString());
                docSym.setKind(SymbolKind.Variable);
                docSym.setDetail(node.type().name());
            }
            result.add(Either.forRight(docSym));
        }
        return CompletableFuture.completedFuture(result);
    }
}
