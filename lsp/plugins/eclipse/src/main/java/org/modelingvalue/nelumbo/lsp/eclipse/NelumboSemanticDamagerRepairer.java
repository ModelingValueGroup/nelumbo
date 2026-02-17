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

package org.modelingvalue.nelumbo.lsp.eclipse;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.presentation.IPresentationDamager;
import org.eclipse.jface.text.presentation.IPresentationRepairer;
import org.eclipse.lsp4e.LanguageServerWrapper;
import org.eclipse.lsp4e.LanguageServiceAccessor;
import org.eclipse.lsp4e.LanguageServiceAccessor.LSPDocumentInfo;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensLegend;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public class NelumboSemanticDamagerRepairer implements IPresentationDamager, IPresentationRepairer {
    // Colors matching NelumboEditor's DEFAULT_TOKEN_COLORS
    private static final Map<String, int[]> TOKEN_COLORS = Map.ofEntries(
            Map.entry("comment",   new int[]{0xCC, 0xCC, 0xCC}),  // END_LINE_COMMENT / IN_LINE_COMMENT
            Map.entry("string",    new int[]{0x00, 0x66, 0x33}),  // STRING
            Map.entry("number",    new int[]{0x00, 0x00, 0x77}),  // NUMBER / DECIMAL
            Map.entry("keyword",   new int[]{0x00, 0x00, 0xFF}),  // KEYWORD
            Map.entry("modifier",  new int[]{0x00, 0x00, 0xFF}),  // KEYWORD
            Map.entry("variable",  new int[]{0x33, 0x99, 0x00}),  // VARIABLE
            Map.entry("property",  new int[]{0x00, 0x00, 0xFF}),  // NAME
            Map.entry("type",      new int[]{0x88, 0x00, 0x88}),  // TYPE
            Map.entry("operator",  new int[]{0x33, 0x33, 0x33}),  // OPERATOR
            Map.entry("decorator", new int[]{0x00, 0xCC, 0xCC})   // META_OPERATOR
    );

    private static final Map<String, Integer> TOKEN_STYLES = Map.of(
            "keyword", SWT.BOLD,
            "modifier", SWT.BOLD,
            "operator", SWT.BOLD
    );

    private static final int             MAX_RETRIES    = 5;
    private static final int             RETRY_DELAY_MS = 500;

    // Dedicated thread avoids ForkJoinPool deadlock with LSP4E's internal locking
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "NelumboSemanticTokens");
        t.setDaemon(true);
        return t;
    });

    private final ITextViewer          viewer;
    private IDocument                  document;
    private SemanticTokens             cachedTokens;
    private SemanticTokensLegend       cachedLegend;
    private volatile Future<?>         pendingRequest;

    private final IDocumentListener documentListener = new IDocumentListener() {
        @Override
        public void documentAboutToBeChanged(DocumentEvent event) {
        }

        @Override
        public void documentChanged(DocumentEvent event) {
            fetchTokens();
        }
    };

    public NelumboSemanticDamagerRepairer(ITextViewer viewer) {
        this.viewer = viewer;
    }

    public void uninstall() {
        if (pendingRequest != null) {
            pendingRequest.cancel(true);
        }
        if (document != null) {
            document.removeDocumentListener(documentListener);
        }
    }

    @Override
    public void setDocument(IDocument document) {
        if (this.document != null) {
            this.document.removeDocumentListener(documentListener);
        }
        this.document = document;
        if (document != null) {
            document.addDocumentListener(documentListener);
            fetchTokens();
        }
    }

    private void fetchTokens() {
        IDocument doc = this.document;
        if (doc == null) {
            return;
        }

        if (pendingRequest != null) {
            pendingRequest.cancel(true);
        }

        pendingRequest = EXECUTOR.submit(() -> {
            try {
                List<LSPDocumentInfo> infos = LanguageServiceAccessor.getLSPDocumentInfosFor(
                        doc, capabilities -> capabilities.getSemanticTokensProvider() != null
                );
                if (infos.isEmpty()) {
                    return;
                }
                LSPDocumentInfo info = infos.get(0);

                SemanticTokensWithRegistrationOptions provider = info.getCapabilites().getSemanticTokensProvider();
                if (provider == null) {
                    return;
                }
                cachedLegend = provider.getLegend();

                SemanticTokensParams params = new SemanticTokensParams(
                        new TextDocumentIdentifier(info.getFileUri().toString())
                );

                LanguageServerWrapper wrapper = info.getLanguageServerWrapper();
                requestTokens(wrapper, params, 0);
            } catch (Throwable t) {
                t.printStackTrace(System.err);
            }
        });
    }

    private void requestTokens(LanguageServerWrapper wrapper, SemanticTokensParams params, int attempt) {
        wrapper.execute(server -> server.getTextDocumentService().semanticTokensFull(params))
                .thenAccept(tokens -> {
                    if (tokens != null) {
                        cachedTokens = tokens;
                        applyPresentation();
                    } else if (attempt < MAX_RETRIES) {
                        // Server returned null — likely didOpen hasn't been sent yet; retry after a delay
                        EXECUTOR.submit(() -> {
                            try {
                                Thread.sleep(RETRY_DELAY_MS);
                            } catch (InterruptedException e) {
                                return;
                            }
                            requestTokens(wrapper, params, attempt + 1);
                        });
                    }
                })
                .exceptionally(e -> {
                    e.printStackTrace(System.err);
                    return null;
                });
    }

    private void applyPresentation() {
        Display display = viewer.getTextWidget().getDisplay();
        if (display.isDisposed()) {
            return;
        }
        display.asyncExec(() -> {
            try {
                if (viewer.getTextWidget().isDisposed()) {
                    return;
                }
                IDocument doc = document;
                if (doc == null) {
                    return;
                }
                TextPresentation presentation = new TextPresentation(new Region(0, doc.getLength()), 100);
                createPresentation(presentation, null);
                if (!viewer.getTextWidget().isDisposed()) {
                    viewer.changeTextPresentation(presentation, false);
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        });
    }

    @Override
    public IRegion getDamageRegion(ITypedRegion partition, DocumentEvent event, boolean documentPartitioningChanged) {
        return new Region(0, document != null ? document.getLength() : 0);
    }

    @Override
    public void createPresentation(TextPresentation presentation, ITypedRegion damage) {
        SemanticTokens tokens = cachedTokens;
        SemanticTokensLegend legend = cachedLegend;
        IDocument doc = document;

        if (tokens == null || legend == null || doc == null) {
            return;
        }

        List<Integer> data = tokens.getData();
        if (data == null || data.isEmpty()) {
            return;
        }

        List<String> tokenTypes = legend.getTokenTypes();
        int line = 0;
        int character = 0;

        for (int i = 0; i + 4 < data.size(); i += 5) {
            int deltaLine   = data.get(i);
            int deltaChar   = data.get(i + 1);
            int length      = data.get(i + 2);
            int tokenType   = data.get(i + 3);
            // data.get(i + 4) is tokenModifiers — not used

            if (deltaLine > 0) {
                line += deltaLine;
                character = deltaChar;
            } else {
                character += deltaChar;
            }

            if (tokenType < 0 || tokenType >= tokenTypes.size()) {
                continue;
            }

            String typeName = tokenTypes.get(tokenType);
            int[] rgb = TOKEN_COLORS.get(typeName);
            if (rgb == null) {
                continue;
            }

            try {
                int offset = doc.getLineOffset(line) + character;
                Color color = new Color(Display.getCurrent(), rgb[0], rgb[1], rgb[2]);
                int style = TOKEN_STYLES.getOrDefault(typeName, SWT.NORMAL);

                StyleRange styleRange       = new StyleRange();
                styleRange.start            = offset;
                styleRange.length           = length;
                styleRange.foreground       = color;
                styleRange.fontStyle        = style;
                presentation.addStyleRange(styleRange);
            } catch (Exception e) {
                // line/offset out of bounds — skip this token
            }
        }
    }
}
