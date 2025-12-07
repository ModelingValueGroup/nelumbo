package org.modelingvalue.nelumbo.lsp.intellij.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

public class ViewLogs extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project != null) {
            ToolWindow tw = ToolWindowManager.getInstance(project).getToolWindow("Language Servers");
            if (tw != null) {
                tw.show();
            }
        }
    }
}
