package org.modelingvalue.nelumbo.lsp.intellij.setting;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.FormBuilder;
import com.redhat.devtools.lsp4ij.LanguageServerManager;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.jdesktop.swingx.JXTitledSeparator;
import org.jetbrains.annotations.Nls;
import org.modelingvalue.nelumbo.lsp.intellij.Constants;
import org.modelingvalue.nelumbo.lsp.intellij.setting.PluginSetting.Setting.Nelumbo.OptionSubject1.FutureOptionAEnum;

public class SettingConfigurable implements Configurable {

    private final ComboBox<FutureOptionAEnum> futureOptionCombo   = new ComboBox<>();
    private final JBCheckBox                  optACheckbox        = new JBCheckBox("Option 1");
    private final JBCheckBox                  optBCheckbox        = new JBCheckBox("Option 2");
    private final JBCheckBox                  useJetBrainsRuntime = new JBCheckBox("Use Jetbrains Runtime");

    public SettingConfigurable() {
        for (FutureOptionAEnum v : FutureOptionAEnum.values()) {
            futureOptionCombo.addItem(v);
        }
    }

    @Override
    public JComponent createComponent() {
        FormBuilder builder = FormBuilder.createFormBuilder();
        builder.addComponent(new JXTitledSeparator("OptionSubject1"));
        builder.addLabeledComponent(new JLabel("Future Option"), futureOptionCombo, 1, false);
        builder.addComponent(new JXTitledSeparator("OptionSubject2"));
        builder.addComponent(optACheckbox, 1);
        builder.addComponent(optBCheckbox, 1);
        builder.addComponent(new JXTitledSeparator("Other"));
        return builder.addComponentFillVertically(new JPanel(), 0).getPanel();
    }

    @Override
    public boolean isModified() {
        PluginSetting.Setting settings = PluginSetting.getInstance().getState();
        if (settings != null) {
            return settings.nelumbo.optionSubject1.futureOptionA != futureOptionCombo.getSelectedItem() //
                   || settings.nelumbo.optionSubject2.optA != optACheckbox.isSelected()//
                   || settings.nelumbo.optionSubject2.optB != optBCheckbox.isSelected()//
                   || settings.useJetBrainsRuntime != useJetBrainsRuntime.isSelected();
        }
        return false;
    }

    @Override
    public void apply() {
        PluginSetting.Setting settings = PluginSetting.getInstance().getState();
        if (settings != null) {
            settings.nelumbo.optionSubject1.futureOptionA = (FutureOptionAEnum) futureOptionCombo.getSelectedItem();
            settings.nelumbo.optionSubject2.optA          = optACheckbox.isSelected();
            settings.nelumbo.optionSubject2.optB          = optBCheckbox.isSelected();
            settings.useJetBrainsRuntime                  = useJetBrainsRuntime.isSelected();
            if (ProjectManager.getInstance().getOpenProjects().length > 0) {
                var project = ProjectManager.getInstance().getOpenProjects()[0];
                LanguageServerManager.getInstance(project).getLanguageServer(Constants.SERVER_ID).thenApply(server -> {
                    if (server != null) {
                        Gson       gson         = new GsonBuilder().setFieldNamingStrategy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();
                        JsonObject asJsonObject = gson.toJsonTree(PluginSetting.getInstance().getState().nelumbo).getAsJsonObject();
                        server.getWorkspaceService().didChangeConfiguration(new DidChangeConfigurationParams(asJsonObject));
                    }
                    return null;
                });
            }
        }
    }

    @Override
    public void reset() {
        PluginSetting.Setting settings = PluginSetting.getInstance().getState();
        if (settings != null) {
            futureOptionCombo.setSelectedItem(settings.nelumbo.optionSubject1.futureOptionA);
            optACheckbox.setSelected(settings.nelumbo.optionSubject2.optA);
            optBCheckbox.setSelected(settings.nelumbo.optionSubject2.optB);
            useJetBrainsRuntime.setSelected(settings.useJetBrainsRuntime);
        }
    }

    @Override
    @Nls(capitalization=Nls.Capitalization.Title)
    public String getDisplayName() {
        return Constants.NAME;
    }
}
