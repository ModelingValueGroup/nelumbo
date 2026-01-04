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
import org.modelingvalue.nelumbo.lsp.intellij.setting.PluginSetting.Setting.Nelumbo.Formatting.PropsSpaceLine;

public class SettingConfigurable implements Configurable {

    private final ComboBox<PropsSpaceLine> propsSpaceLineCombo       = new ComboBox<>();
    private final JBCheckBox               findConfigurationCheckbox = new JBCheckBox("Find Configuration");
    private final JBCheckBox               findOtherProjectCheckbox  = new JBCheckBox("Find Other Project");
    private final JBCheckBox               debuggingCheckbox         = new JBCheckBox("Debugging");
    private final JBCheckBox               useJetBrainsRuntime       = new JBCheckBox("Use Jetbrains Runtime");

    public SettingConfigurable() {
        for (PropsSpaceLine v : PropsSpaceLine.values()) {
            propsSpaceLineCombo.addItem(v);
        }
    }

    @Override
    public JComponent createComponent() {
        FormBuilder builder = FormBuilder.createFormBuilder();
        builder.addComponent(new JXTitledSeparator("Formatting"));
        builder.addLabeledComponent(new JLabel("Props Space Line"), propsSpaceLineCombo, 1, false);
        builder.addComponent(new JXTitledSeparator("Classpath"));
        builder.addComponent(findConfigurationCheckbox, 1);
        builder.addComponent(findOtherProjectCheckbox, 1);
        builder.addComponent(new JXTitledSeparator("Other"));
        builder.addComponent(debuggingCheckbox, 1);
        return builder.addComponentFillVertically(new JPanel(), 0).getPanel();
    }

    @Override
    public boolean isModified() {
        PluginSetting.Setting settings = PluginSetting.getInstance().getState();
        if (settings != null) {
            return settings.nelumbo.formatting.propsSpaceLine != propsSpaceLineCombo.getSelectedItem() //
                   || settings.nelumbo.classpath.findConfiguration != findConfigurationCheckbox.isSelected()//
                   || settings.nelumbo.classpath.findOtherProject != findOtherProjectCheckbox.isSelected()//
                   || settings.nelumbo.debugging != debuggingCheckbox.isSelected()//
                   || settings.useJetBrainsRuntime != useJetBrainsRuntime.isSelected();
        }
        return false;
    }

    @Override
    public void apply() {
        PluginSetting.Setting settings = PluginSetting.getInstance().getState();
        if (settings != null) {
            settings.nelumbo.formatting.propsSpaceLine   = (PropsSpaceLine) propsSpaceLineCombo.getSelectedItem();
            settings.nelumbo.classpath.findConfiguration = findConfigurationCheckbox.isSelected();
            settings.nelumbo.classpath.findOtherProject  = findOtherProjectCheckbox.isSelected();
            settings.nelumbo.debugging                   = debuggingCheckbox.isSelected();
            settings.useJetBrainsRuntime                 = useJetBrainsRuntime.isSelected();
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
            propsSpaceLineCombo.setSelectedItem(settings.nelumbo.formatting.propsSpaceLine);
            findConfigurationCheckbox.setSelected(settings.nelumbo.classpath.findConfiguration);
            findOtherProjectCheckbox.setSelected(settings.nelumbo.classpath.findOtherProject);
            debuggingCheckbox.setSelected(settings.nelumbo.debugging);
            useJetBrainsRuntime.setSelected(settings.useJetBrainsRuntime);
        }
    }

    @Override
    @Nls(capitalization = Nls.Capitalization.Title)
    public String getDisplayName() {
        return Constants.NAME;
    }
}
