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

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.FoldingRangeRequestParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.SelectionRange;
import org.eclipse.lsp4j.SelectionRangeParams;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.TypeDefinitionParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.modelingvalue.nelumbo.lsp.documentService.DocumentCodeActionService;
import org.modelingvalue.nelumbo.lsp.documentService.DocumentCodeLensService;
import org.modelingvalue.nelumbo.lsp.documentService.DocumentCompletionService;
import org.modelingvalue.nelumbo.lsp.documentService.DocumentDefinitionService;
import org.modelingvalue.nelumbo.lsp.documentService.DocumentFoldingRangeService;
import org.modelingvalue.nelumbo.lsp.documentService.DocumentFormattingService;
import org.modelingvalue.nelumbo.lsp.documentService.DocumentHoverService;
import org.modelingvalue.nelumbo.lsp.documentService.DocumentSemanticTokensFullService;
import org.modelingvalue.nelumbo.lsp.documentService.DocumentSymbolService;
import org.modelingvalue.nelumbo.lsp.documentService.DocumentSyncService;
import org.modelingvalue.nelumbo.lsp.documentService.DocumentTypeDefinitionService;
import org.modelingvalue.nelumbo.lsp.documentService.SelectionRangeService;

public class NlTextDocumentService implements TextDocumentService {
    private final DocumentSyncService               documentSyncService;
    private final DocumentSemanticTokensFullService documentSemanticTokensFullService;
    private final DocumentFoldingRangeService       documentFoldingRangeService;
    private final DocumentCompletionService         documentCompletionService;
    private final DocumentSymbolService             documentSymbolService;
    private final DocumentFormattingService         documentFormattingService;
    private final DocumentHoverService              documentHoverService;
    private final DocumentDefinitionService         documentDefinitionService;
    private final DocumentCodeLensService           documentCodeLensService;
    private final DocumentCodeActionService         documentCodeActionService;
    private final DocumentTypeDefinitionService documentTypeDefinitionService;
    private final SelectionRangeService         selectionRangeService;

    public NlTextDocumentService(Workspace workspace) {
        NlDocumentManager documentManager = new NlDocumentManager(workspace);
        this.documentSyncService               = new DocumentSyncService(documentManager);
        this.documentSemanticTokensFullService = new DocumentSemanticTokensFullService(documentManager);
        this.documentFoldingRangeService       = new DocumentFoldingRangeService(documentManager);
        this.documentCompletionService         = new DocumentCompletionService(documentManager);
        this.documentSymbolService             = new DocumentSymbolService(documentManager);
        this.documentFormattingService         = new DocumentFormattingService(documentManager);
        this.documentHoverService              = new DocumentHoverService(documentManager);
        this.documentDefinitionService         = new DocumentDefinitionService(documentManager);
        this.documentCodeLensService           = new DocumentCodeLensService(documentManager);
        this.documentCodeActionService         = new DocumentCodeActionService(documentManager);
        this.documentTypeDefinitionService = new DocumentTypeDefinitionService(documentManager);
        this.selectionRangeService         = new SelectionRangeService(documentManager);
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
        System.err.println("~~~ didOpen: " + params.getTextDocument().getUri());
        documentSyncService.didOpen(params);
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
        System.err.println("~~~ didChange: " + params.getTextDocument().getUri());
        documentSyncService.didChange(params);
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
        System.err.println("~~~ didClose: " + params.getTextDocument().getUri());
        documentSyncService.didClose(params);
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
        System.err.println("~~~ didSave: " + params.getTextDocument().getUri());
        documentSyncService.didSave(params);
    }

    @Override
    public CompletableFuture<SemanticTokens> semanticTokensFull(SemanticTokensParams params) {
        System.err.println("~~~ semanticTokensFull: " + params.getTextDocument().getUri());
        return documentSemanticTokensFullService.semanticTokensFull(params);
    }

    @Override
    public CompletableFuture<List<FoldingRange>> foldingRange(FoldingRangeRequestParams params) {
        System.err.println("~~~ foldingRange: " + params.getTextDocument().getUri());
        return documentFoldingRangeService.foldingRange(params);
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params) {
        System.err.println("~~~ completion: " + params.getTextDocument().getUri() + " " + U.render(params.getPosition()));
        return documentCompletionService.completion(params);
    }

    @Override
    public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(DocumentSymbolParams params) {
        System.err.println("~~~ documentSymbol: " + params.getTextDocument().getUri());
        return documentSymbolService.documentSymbol(params);
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
        System.err.println("~~~ formatting: " + params.getTextDocument().getUri() + " " + params.getOptions());
        return documentFormattingService.formatting(params);
    }

    @Override
    public CompletableFuture<Hover> hover(HoverParams params) {
        System.err.println("~~~ hover: " + params.getTextDocument().getUri() + " " + U.render(params.getPosition()));
        return documentHoverService.hover(params);
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(DefinitionParams params) {
        System.err.println("~~~ definition: " + params.getTextDocument().getUri() + " " + U.render(params.getPosition()));
        return documentDefinitionService.definition(params);
    }

    @Override
    public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
        System.err.println("~~~ codeLens: " + params.getTextDocument().getUri());
        return documentCodeLensService.codeLens(params);
    }

    @Override
    public CompletableFuture<CodeLens> resolveCodeLens(CodeLens params) {
        System.err.println("~~~ resolveCodeLens: " + params.getCommand().getCommand());
        return documentCodeLensService.resolveCodeLens(params);
    }

    @Override
    public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
        System.err.println("~~~ codeAction: " + params.getTextDocument().getUri() + " " + U.render(params.getRange()));
        return documentCodeActionService.codeAction(params);
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> typeDefinition(TypeDefinitionParams params) {
        System.err.println("~~~ typeDefinition: " + params.getTextDocument().getUri() + " " + U.render(params.getPosition()));
        return documentTypeDefinitionService.typeDefinition(params);
    }

    @Override
    public CompletableFuture<List<SelectionRange>> selectionRange(SelectionRangeParams params) {
        System.err.println("~~~ selectionRange: " + params.getTextDocument().getUri() + " " + params.getPositions().stream().map(U::render).toList());
        return selectionRangeService.selectionRange(params);
    }
}
