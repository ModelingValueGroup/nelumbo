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
//     Wim Bast, Tom Brus, Victor Lap                                                                                  ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.nelumbo.lsp.documentService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.lang.Functor;
import org.modelingvalue.nelumbo.lang.Type;
import org.modelingvalue.nelumbo.lang.Variable;
import org.modelingvalue.nelumbo.lsp.Main;
import org.modelingvalue.nelumbo.lsp.NlDocument;
import org.modelingvalue.nelumbo.lsp.NlDocumentManager;
import org.modelingvalue.nelumbo.lsp.U;
import org.modelingvalue.nelumbo.patterns.Pattern;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class DocumentHoverService extends DocumentServiceAdapter {
    public DocumentHoverService(NlDocumentManager documentManager) {
        super(documentManager);
    }

    @Override
    public CompletableFuture<Hover> hover(HoverParams params) {
        NlDocument document = documentManager.getDocument(params.getTextDocument().getUri());
        if (document == null) {
            return CompletableFuture.completedFuture(null);
        }
        Position pos = params.getPosition();
        // right at the end of a query, where its result is shown: QueryResult.hover() (the time, ...). The website
        // editor shows this for a result instead of the result's own tooltip, together with a problem marker
        // there, so that it is one hover around the result and on it (resultsHoverAtTheirQuery in nelumbo-fields.ts)
        for (InlayHint hint : documentManager.queryResultCache().hints(params.getTextDocument().getUri())) {
            if (hint.getPosition().equals(pos)) {
                return CompletableFuture.completedFuture(hint.getData() instanceof String hover && !hover.isEmpty()
                        ? new Hover(new MarkupContent(MarkupKind.PLAINTEXT, hover)) : null);
            }
        }
        Token token = document.tokenAt(pos);
        if (token == null) {
            return CompletableFuture.completedFuture(null);
        }
        StringBuilder text = new StringBuilder();

        if (Main.debugging()) {
            List<Node> debugNodes = document.nodesAt(pos);
            U.DEBUG("    hover [%s]:", U.render(pos));
            text.append(String.format("under [%s]:", U.render(pos)));
            U.DEBUG("     - token %s", token);
            text.append(
                    String.format("\n<br> - [%s] TOKEN %s", U.renderSpan(token), U.escapeMarkdown(token.textTraced())));
            if (debugNodes.isEmpty()) {
                U.DEBUG("        <no nodes found>");
                text.append("\n<br> - <i>no nodes found</i>");
            } else {
                for (Node node : debugNodes) {
                    U.DEBUG("        - [%s} %s", U.renderSpan(node), node);
                    text.append(String.format("\n<br> - [%s] NODE %s", U.renderSpan(node),
                            U.escapeMarkdown(node.toString())));
                }
            }
            text.append("\n\n---\n\n");
        }

        // only types, variables and expressions get a hover; keywords, spaces, comments, punctuation and the parts
        // of statements, patterns and declarations would only repeat what the editor already shows
        String info = switch (token.colorType()) {
            case TYPE     -> typeInfo(document, pos, token);
            case VARIABLE -> variableInfo(token);
            default       -> expressionInfo(token);
        };
        if (info.isEmpty() && !Main.debugging()) {
            return CompletableFuture.completedFuture(null);
        }
        text.append(info);

        MarkupContent mc = new MarkupContent(MarkupKind.MARKDOWN, text.toString());
        Range range = U.range(token);
        Hover hover = new Hover(mc, range);
        return CompletableFuture.completedFuture(hover);
    }

    // the token of a type declaration holds the raw declaration node (Parser keeps it when init changes the
    // node's type), whose arguments are laid out differently: take the declared type from the parse tree
    private static String typeInfo(NlDocument document, Position pos, Token token) {
        Type type = token.getNode() instanceof Type t && t.typeInfo() != null ? t : null;
        if (type == null) {
            for (Node node : document.nodesAt(pos)) {
                if (node instanceof Type t && t.typeInfo() != null && t.rawName().equals(token.text())) {
                    type = t;
                    break;
                }
            }
        }
        if (type == null) {
            return "**type** " + U.codeSpan(token.text());
        }
        StringBuilder supers = new StringBuilder();
        for (Type sup : type.supersDeclaration()) {
            if (sup.clss() == null) { // a Java-class type ($Object) is internal
                supers.append(supers.isEmpty() ? "" : ", ").append(U.codeSpan(sup.name()));
            }
        }
        return "**type** " + U.codeSpan(type.name()) + (supers.isEmpty() ? "" : "\n\nSupertypes: " + supers);
    }

    // the name is the token itself: the variable node of a declaration is raw and a reference has no arguments
    private static String variableInfo(Token token) {
        Variable var = token.variable();
        return "**variable** " + U.codeSpan(token.text()) + (var == null ? "" : " : " + U.codeSpan(display(var.type())));
    }

    // a word, operator or literal of an expression: its functor (or the literal itself) and its type
    private static String expressionInfo(Token token) {
        TokenType tokenType = token.type();
        Node      node      = token.getNode();
        boolean   word      = tokenType == TokenType.NAME || tokenType == TokenType.OPERATOR || tokenType == TokenType.NUMBER || tokenType == TokenType.STRING;
        if (!word || node == null || node instanceof Type || node instanceof Pattern || node instanceof Functor || Type.ROOT.isAssignableFrom(node.type())) {
            return "";
        }
        String name = node.functor() != null && !node.functor().name().isEmpty() ? node.functor().name() : token.text();
        return U.codeSpan(name) + " : " + U.codeSpan(display(node.type()));
    }

    // {Function,Integer}, {Integer,Literal} and {Integer,Variable} are all an Integer to the reader
    private static String display(Type type) {
        return type.nonFunction().nonLiteral().nonVariable().name();
    }
}
