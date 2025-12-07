//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2025 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
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

package org.modelingvalue.nelumbo.lsp.intellij.status;

import java.util.List;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.modelingvalue.nelumbo.lsp.intellij.Constants;
import org.modelingvalue.nelumbo.lsp.intellij.NelumboFileType;

public class StatusBarItem extends EditorBasedStatusBarPopup {
    public StatusBarItem(@NotNull Project project) {
        super(project, false);
    }

    @Override
    @NotNull
    public String ID() {
        return Constants.LANGUAGE_ID + "BarItem";
    }

    @Override
    @NotNull
    protected StatusBarWidget createInstance(@NotNull Project project) {
        return new StatusBarItem(project);
    }

    @Override
    @NotNull
    protected ListPopup createPopup(@NotNull DataContext context) {
        AnAction                          viewLogs    = ActionManager.getInstance().getAction("org.modelingvalue.nelumbo.lsp.intellij.action.ViewLogs");
        DefaultActionGroup                group       = new DefaultActionGroup(List.of(viewLogs));
        JBPopupFactory.ActionSelectionAid speedsearch = JBPopupFactory.ActionSelectionAid.SPEEDSEARCH;
        return JBPopupFactory.getInstance().createActionGroupPopup(Constants.NAME, group, context, speedsearch, false);
    }

    @Override
    @NotNull
    protected WidgetState getWidgetState(@Nullable VirtualFile file) {
        WidgetState state = new WidgetState(Constants.NAME, null, true);
        state.setIcon(Constants.ICON);
        return state;
    }

    @Override
    protected boolean isEnabledForFile(@Nullable VirtualFile file) {
        return file != null && file.getFileType() == NelumboFileType.INSTANCE;
    }
}
