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

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.TypeDefinitionParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.Variable;
import org.modelingvalue.nelumbo.lsp.NlDocument;
import org.modelingvalue.nelumbo.lsp.NlDocumentManager;
import org.modelingvalue.nelumbo.lsp.U;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class DocumentTypeDefinitionService extends DocumentServiceAdapter {
    public DocumentTypeDefinitionService(NlDocumentManager documentManager) {
        super(documentManager);
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> typeDefinition(TypeDefinitionParams params) {
        String     uri      = params.getTextDocument().getUri();
        NlDocument document = documentManager.getDocument(uri);
        if (document == null) {
            return CompletableFuture.completedFuture(null);
        }
        Token token = document.tokenAt(params.getPosition());
        if (token == null) {
            return CompletableFuture.completedFuture(null);
        }
        // Determine the type name to look up
        String    typeName  = null;
        TokenType colorType = token.colorType();
        if (colorType == TokenType.TYPE) {
            typeName = token.text();
        } else if (colorType == TokenType.VARIABLE) {
            Variable var = token.variable();
            if (var != null) {
                typeName = var.type().rawName();
            }
        } else if (token.type() == TokenType.NAME) {
            typeName = token.text();
        }
        if (typeName == null) {
            return CompletableFuture.completedFuture(null);
        }
        // Find the declaring Type node in roots
        for (Node root : document.parserResult().roots()) {
            if (root instanceof Type type) {
                if (type.name().equals(typeName) || type.rawName().equals(typeName)) {
                    Token first = type.firstToken();
                    if (first != null) {
                        List<Location> locations = List.of(new Location(uri, U.range(first)));
                        return CompletableFuture.completedFuture(Either.forLeft(locations));
                    }
                }
            }
        }
        return CompletableFuture.completedFuture(null);
    }
}
