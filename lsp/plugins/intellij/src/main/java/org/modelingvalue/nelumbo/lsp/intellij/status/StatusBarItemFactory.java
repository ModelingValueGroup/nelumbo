package org.modelingvalue.nelumbo.lsp.intellij.status;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.widget.StatusBarEditorBasedWidgetFactory;
import org.jetbrains.annotations.NotNull;
import org.modelingvalue.nelumbo.lsp.intellij.Constants;

public class StatusBarItemFactory extends StatusBarEditorBasedWidgetFactory {
    @Override
    @NotNull
    public String getDisplayName() {
        return Constants.NAME;
    }

    @Override
    @NotNull
    public String getId() {
        return Constants.LANGUAGE_ID + "StatusBarFactory";
    }

    @Override
    @NotNull
    public StatusBarWidget createWidget(@NotNull Project project) {
        return new StatusBarItem(project);
    }
}
