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

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/** Preferences page for the Nelumbo language plugin (Window &gt; Preferences &gt; Nelumbo). */
public class NelumboPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
    private Button formatOnSave;

    public NelumboPreferencePage() {
        setDescription("Settings for the Nelumbo language editor.");
    }

    @Override
    public void init(IWorkbench workbench) {
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite area = new Composite(parent, SWT.NONE);
        area.setLayout(new GridLayout(1, false));
        formatOnSave = new Button(area, SWT.CHECK);
        formatOnSave.setText("Format Nelumbo (.nl) files on save");
        formatOnSave.setSelection(NelumboPreferences.isFormatOnSave());
        return area;
    }

    @Override
    protected void performDefaults() {
        if (formatOnSave != null) {
            formatOnSave.setSelection(NelumboPreferences.FORMAT_ON_SAVE_DEFAULT);
        }
        super.performDefaults();
    }

    @Override
    public boolean performOk() {
        if (formatOnSave != null) {
            NelumboPreferences.setFormatOnSave(formatOnSave.getSelection());
        }
        return super.performOk();
    }
}
