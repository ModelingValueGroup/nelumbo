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
import org.modelingvalue.collections.util.Pair;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.ParserResult;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;
import org.modelingvalue.nelumbo.syntax.Tokenizer;
import org.modelingvalue.nelumbo.syntax.Tokenizer.TokenizerResult;

public record NlDocument(String content,
                         int version,
                         String uri,
                         TokenizerResult tokenizerResult,
                         List<Node> nodeList) {

    public static NlDocument of(NlDocument document, String content) {
        return of(content, document.version(), document.uri());
    }

    public static NlDocument of(String content, int version, String uri) {
        TokenizerResult tokenizerResult = new Tokenizer(content, uri).tokenize();
        List<Pair<String, Range>> errors = new ArrayList<>(tokenizerResult.listAll()//
                                                                          .filter(t -> t.type() == TokenType.ERROR)//
                                                                          .map(t -> Pair.of("illegal token: " + t.textTraced(), new Range(new Position(t.line(), t.position()), new Position(t.line(), t.position() + 1)))).toList());
        List<Node> nodes = parse(tokenizerResult, errors);
        System.err.println("    NlDocument.of: " + tokenizerResult.listAll().size() + " tokens, " + nodes.size() + " nodes, " + errors.size() + " errors");
        TRACE_NODES(nodes, "");
        if (!errors.isEmpty()) {
            publishDiagnosticsAsync(uri, errors);
        }
        return new NlDocument(content, version, uri, tokenizerResult, nodes);
    }

    private static void TRACE_NODES(List<? extends AstElement> nodes, String indent) {
        nodes.forEach(a -> {
            if (a instanceof Token t) {
                System.err.println(indent + "T:" + t.type() + ":" + t);
            } else if (a instanceof Node n) {
                System.err.println(indent + "N:" + n.type() + ":" + n + (n.functor() == null ? "" : "  -> " + n.functor()));
                TRACE_NODES(n.astElements().toMutable(), indent + "  ");
            } else {
                System.err.println(indent + "?:" + a.getClass().getSimpleName() + ":" + a);
            }
        });
    }

    private static List<Node> parse(TokenizerResult tokenizerResult, List<Pair<String, Range>> errors) {
        List<Node> l = new ArrayList<>();
        KnowledgeBase.BASE.run(() -> {
            ParserResult parserResult = new Parser(tokenizerResult).parseNonThrowing();
            if (!parserResult.exceptions().isEmpty()) {
                errors.addAll(parserResult.exceptions().map(e -> //
                                                                    Pair.of(e.getMessage(), new Range(new Position(e.line(), e.position()), new Position(e.line(), e.position())))//
                                                           ).toList());
            }
            System.err.println("===== " + parserResult.roots().size() + " roots ===== " + parserResult.exceptions().size() + " exceptions =====");
            l.addAll(parserResult.roots().toMutable());
        });
        return l;
    }

    public List<Token> tokens() {
        return tokenizerResult.listAll().toList();
    }

    public Token tokenAt(Position position) {
        return U.findToken(position, tokens());
    }

    public Node nodeAt(Position position) {
        System.err.println("NlDocument.nodeAt: " + tokens().size() + " tokens");
        return nodeList.stream().peek(node -> System.err.println("NlDocument.nodeAt: " + node + " of tokens: " + U.render(node.tokens().toList()))).filter(node -> U.findToken(position, node.tokens().toList()) != null).findFirst().orElse(null);
    }

    private static void publishDiagnosticsAsync(String uri, List<Pair<String, Range>> errors) {
        try (ExecutorService svc = Executors.newSingleThreadExecutor()) {
            svc.submit(() -> {
                List<Diagnostic> l = errors.stream()//
                                           .map(p -> new Diagnostic(p.b(), p.a(), DiagnosticSeverity.Error, "nelumbo"))//
                                           .toList();
                Main.client.publishDiagnostics(new PublishDiagnosticsParams(uri, l));
            });
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
