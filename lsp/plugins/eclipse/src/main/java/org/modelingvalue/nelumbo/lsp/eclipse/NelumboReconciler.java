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

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.presentation.IPresentationDamager;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.IPresentationRepairer;

public class NelumboReconciler implements IPresentationReconciler, ITextInputListener {
    private ITextViewer                    viewer;
    private NelumboSemanticDamagerRepairer damagerRepairer;

    @Override
    public void install(ITextViewer textViewer) {
        this.viewer = textViewer;
        damagerRepairer = new NelumboSemanticDamagerRepairer(textViewer);
        textViewer.addTextInputListener(this);
    }

    @Override
    public void uninstall() {
        if (viewer != null) {
            viewer.removeTextInputListener(this);
        }
        if (damagerRepairer != null) {
            damagerRepairer.uninstall();
            damagerRepairer = null;
        }
    }

    @Override
    public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
    }

    @Override
    public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
        if (damagerRepairer != null) {
            damagerRepairer.setDocument(newInput);
        }
    }

    @Override
    public IPresentationDamager getDamager(String contentType) {
        return damagerRepairer;
    }

    @Override
    public IPresentationRepairer getRepairer(String contentType) {
        return damagerRepairer;
    }
}
