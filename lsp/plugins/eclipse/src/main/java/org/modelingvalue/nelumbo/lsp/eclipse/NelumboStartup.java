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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.lsp4e.VersionedEdits;
import org.eclipse.lsp4e.operations.format.LSPFormatter;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Registers a listener on the workbench Save / Save-All commands that runs the LSP formatter on
 * {@code .nl} documents before they are written, when the "Format on save" preference is enabled.
 */
public class NelumboStartup implements IStartup {
    private static final String  SAVE      = "org.eclipse.ui.file.save";
    private static final String  SAVE_ALL  = "org.eclipse.ui.file.saveAll";
    private static final long    TIMEOUT_S = 2;
    private static final LSPFormatter FORMATTER = new LSPFormatter();

    @Override
    public void earlyStartup() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        workbench.getDisplay().asyncExec(() -> {
            ICommandService commandService = workbench.getService(ICommandService.class);
            if (commandService != null) {
                commandService.addExecutionListener(new SaveListener());
            }
        });
    }

    private static final class SaveListener implements IExecutionListener {
        @Override
        public void preExecute(String commandId, ExecutionEvent event) {
            // This listener fires for *every* command. Do the cheap id check first and guard the whole
            // body: an exception escaping here would break command dispatch (and thus key bindings) globally.
            if (!SAVE.equals(commandId) && !SAVE_ALL.equals(commandId)) {
                return;
            }
            try {
                if (!NelumboPreferences.isFormatOnSave()) {
                    return;
                }
                if (SAVE.equals(commandId)) {
                    format(HandlerUtil.getActiveEditor(event));
                } else {
                    for (IEditorPart editor : dirtyEditors()) {
                        format(editor);
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace(System.err);
            }
        }

        @Override
        public void notHandled(String commandId, NotHandledException exception) {
        }

        @Override
        public void postExecuteFailure(String commandId, ExecutionException exception) {
        }

        @Override
        public void postExecuteSuccess(String commandId, Object returnValue) {
        }
    }

    /** Format a single editor's document in place, but only if it is a {@code .nl} text editor. */
    private static void format(IEditorPart editor) {
        if (!(editor instanceof ITextEditor textEditor)) {
            return;
        }
        IEditorInput input = editor.getEditorInput();
        if (input == null || !input.getName().endsWith(".nl")) {
            return;
        }
        IDocument document = textEditor.getDocumentProvider().getDocument(input);
        if (document == null) {
            return;
        }
        try {
            // Empty selection at offset 0 => whole-document formatting. The request is served on a
            // language-server thread; we block briefly (we are on the UI thread, before the save runs)
            // so the edits land in the document that is about to be written.
            Optional<VersionedEdits> edits = FORMATTER.requestFormatting(document, new TextSelection(document, 0, 0)).get(TIMEOUT_S, TimeUnit.SECONDS);
            if (edits.isPresent()) {
                edits.get().apply();
            }
        } catch (Exception e) {
            // Never block the save itself if formatting fails or times out.
            e.printStackTrace(System.err);
        }
    }

    private static List<IEditorPart> dirtyEditors() {
        List<IEditorPart> result = new ArrayList<>();
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) {
            return result;
        }
        IWorkbenchPage page = window.getActivePage();
        if (page == null) {
            return result;
        }
        for (IEditorReference ref : page.getEditorReferences()) {
            IEditorPart editor = ref.getEditor(false);
            if (editor != null && editor.isDirty()) {
                result.add(editor);
            }
        }
        return result;
    }
}
