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
import org.eclipse.lsp4j.services.LanguageClient;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboTimeoutException;
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
        ParserResult    parserResult    = parse(workspace, tokenizerResult);
        publishDiagnosticsAsync(workspace, uri, tokenizerResult, parserResult);

        U.DEBUG("    #tokens    : %4d", tokenizerResult.listAll().size());
        U.DEBUG("    #root-nodes: %4d", parserResult.roots().size());
        try {
            if (!parserResult.roots().isEmpty()) {
                U.DEBUG_NODE(parserResult.root(), "    ");
            }
        } catch (Exception e) {
            U.DEBUG("    DEBUG_NODE failed: %s", e);
        }

        return new NlDocument(workspace, content, version, uri, tokenizerResult, parserResult);
    }

    /**
     * Parse against a deadline-bearing child KB when the workspace carries one (public /lsp), so a pathological
     * document cannot occupy the shared engine pool indefinitely. A timeout yields an empty (root-less) result.
     */
    private static ParserResult parse(Workspace workspace, TokenizerResult tokenizerResult) {
        long deadlineMs = workspace.getEvalDeadlineMs();
        if (deadlineMs <= 0) {
            return Parser.parse(workspace.getBaseKnowledgeBase(), tokenizerResult);
        }
        KnowledgeBase parseKb = new KnowledgeBase(workspace.getBaseKnowledgeBase());
        parseKb.setDeadlineNanos(System.nanoTime() + deadlineMs * 1_000_000L);
        try {
            return Parser.parse(parseKb, tokenizerResult);
        } catch (NelumboTimeoutException e) {
            U.DEBUG("    parse timed out after %d ms", deadlineMs);
            return new ParserResult(tokenizerResult, false);
        }
    }

    public List<Token> tokens() {
        return tokenizerResult.listAll().toList();
    }

    public Token tokenAt(Position position) {
        return U.findToken(position, tokens());
    }

    public List<Node> nodesAt(Position position) {
        return findNodes(position, parserResult.roots()).toMutable();
    }

    private static org.modelingvalue.collections.List<Node> findNodes(Position position, org.modelingvalue.collections.List<? extends AstElement> in) {
        for (AstElement a : in) {
            if (a instanceof Node node && U.contains(position, node)) {
                org.modelingvalue.collections.List<AstElement> astElements = node.astElements();
                return findNodes(position, astElements).add(node);
            }
        }
        return org.modelingvalue.collections.List.of();
    }

    private static void publishDiagnosticsAsync(Workspace workspace, String uri, TokenizerResult tokenizerResult, ParserResult parserResult) {
        publishDiagnostics(workspace, uri, baseDiagnostics(tokenizerResult, parserResult));
    }

    /** Syntax/parse diagnostics for the document; query-result diagnostics are added on top by {@link QueryResultCache}. */
    public static List<Diagnostic> baseDiagnostics(TokenizerResult tokenizerResult, ParserResult parserResult) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        diagnostics.addAll(tokenizerResult.listAll()//
                                          .filter(t -> t.type() == TokenType.ERROR)//
                                          .map(t -> new Diagnostic(new Range(new Position(t.line(), t.position()), new Position(t.line(), t.position() + 1)), "illegal token: " + t.textTraced(), DiagnosticSeverity.Error, "nelumbo"))//
                                          .toList());
        diagnostics.addAll(parserResult.exceptions() //
                                       .map(e -> new Diagnostic(new Range(new Position(e.line(), e.position()), new Position(e.line(), e.position())), e.getMessage(), DiagnosticSeverity.Error, "nelumbo"))//
                                       .toList());
        return diagnostics;
    }

    public static void publishDiagnostics(Workspace workspace, String uri, List<Diagnostic> diagnostics) {
        LanguageClient client = workspace.getClient();
        if (client == null) {
            return;
        }
        if (Main.debugging() && !diagnostics.isEmpty()) {
            U.DEBUG("    #errors    : %4d", diagnostics.size());
        }
        try (ExecutorService svc = Executors.newSingleThreadExecutor()) {
            svc.submit(() -> client.publishDiagnostics(new PublishDiagnosticsParams(uri, diagnostics)));
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
