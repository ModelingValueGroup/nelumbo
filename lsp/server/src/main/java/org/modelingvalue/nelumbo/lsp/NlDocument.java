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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.ParserResult;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;
import org.modelingvalue.nelumbo.syntax.Tokenizer;
import org.modelingvalue.nelumbo.syntax.Tokenizer.TokenizerResult;

public record NlDocument(Workspace workspace,
                         String content,
                         int version,
                         String uri,
                         TokenizerResult tokenizerResult,
                         ParserResult parserResult) {

    public static NlDocument of(NlDocument document, String newContent) {
        return of(document.workspace(), newContent, document.version(), document.uri());
    }

    public static NlDocument of(Workspace workspace, String content, int version, String uri) {
        TokenizerResult tokenizerResult = new Tokenizer(content, uri).tokenize();
        ParserResult    parserResult    = Parser.parse(tokenizerResult);
        publishDiagnosticsAsync(uri, tokenizerResult, parserResult);
        if (Main.debugging()) {
            U.errf("    #tokens    : %4d", tokenizerResult.listAll().size());
            U.errf("    #root-nodes: %4d", parserResult.roots().size());
            if (!parserResult.roots().isEmpty()) {
                TRACE_NODE(parserResult.root(), "    ");
            }
        }
        return new NlDocument(workspace, content, version, uri, tokenizerResult, parserResult);
    }

    private static void TRACE_NODE(AstElement node, String indent) {
        if (node instanceof Token t) {
            //noinspection RedundantStringFormatCall
            System.err.println(String.format("    %s%sT:%-16s  '%s'", U.renderSpan(t), indent, t.type(), t.textTraced()));
        } else if (node instanceof Node n) {
            Node   declaration = n.declaration();
            String decl        = declaration == null ? "<none>" : declaration.firstToken() == null ? "" + declaration : declaration.firstToken().fileName() + " @ " + U.renderSpan(declaration);
            //noinspection RedundantStringFormatCall
            System.err.println(String.format("    %s%sN:%-16s  '%s'  => %s", U.renderSpan(n), indent, n.type(), n, decl));
            n.astElements().forEach(e -> TRACE_NODE(e, indent + "  "));
        } else if (node != null) {
            System.err.println("                    " + indent + "????? " + node.getClass().getSimpleName() + "   " + node);
        } else {
            System.err.println("                    " + indent + "<null>");
        }
    }

    public List<Token> tokens() {
        return tokenizerResult.listAll().toList();
    }

    public Token tokenAt(Position position) {
        return U.findToken(position, tokens());
    }

    public List<Node> nodesAt(Position position) {
        //TODO TOM implement properly: return all nodes that contain the position
        List<Node> found = new ArrayList<>();
        List<Node> l     = parserResult.roots().toMutable();
        for (Node node : l) {
            if (U.contains(position, node)) {
                found.add(node);
                //l = node.children().toList();
                return found;
            }
        }
        return null;
    }

    private static void publishDiagnosticsAsync(String uri, TokenizerResult tokenizerResult, ParserResult parserResult) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        diagnostics.addAll(tokenizerResult.listAll()//
                                          .filter(t -> t.type() == TokenType.ERROR)//
                                          .map(t -> new Diagnostic(new Range(new Position(t.line(), t.position()), new Position(t.line(), t.position() + 1)), "illegal token: " + t.textTraced(), DiagnosticSeverity.Error, "nelumbo"))//
                                          .toList());
        diagnostics.addAll(parserResult.exceptions() //
                                       .map(e -> new Diagnostic(new Range(new Position(e.line(), e.position()), new Position(e.line(), e.position())), e.getMessage(), DiagnosticSeverity.Error, "nelumbo"))//
                                       .toList());
        if (Main.debugging() && !diagnostics.isEmpty()) {
            U.errf("    #errors    : %4d", diagnostics.size());
        }
        try (ExecutorService svc = Executors.newSingleThreadExecutor()) {
            svc.submit(() -> Main.client.publishDiagnostics(new PublishDiagnosticsParams(uri, diagnostics)));
        }
    }

    public Token next(Token t) {
        List<Token> tokens = tokens();
        if (t != null && tokens.size() > 1) {
            for (int i = 0; i < tokens.size() - 1; i++) {
                if (tokens.get(i) == t) {
                    return tokens.get(i + 1);
                }
            }
        }
        return null; // not found
    }

    @SuppressWarnings("unused")
    public Token prev(Token t) {
        List<Token> tokens = tokens();
        if (t != null && tokens.size() > 1) {
            for (int i = 1; i < tokens.size(); i++) {
                if (tokens.get(i) == t) {
                    return tokens.get(i - 1);
                }
            }
        }
        return null; // not found
    }
}
