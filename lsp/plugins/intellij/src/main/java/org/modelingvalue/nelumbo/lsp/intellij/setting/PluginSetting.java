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

package org.modelingvalue.nelumbo.lsp.intellij.setting;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.modelingvalue.nelumbo.lsp.intellij.Constants;

@State(name="org.modelingvalue.nelumbo.lsp.intellij.setting.NelumboSetting", storages={@Storage(Constants.ID + ".xml")})
public class PluginSetting implements PersistentStateComponent<PluginSetting.Setting> {

    public static class Setting {
        public Nelumbo nelumbo             = new Nelumbo();
        public boolean useJetBrainsRuntime = true;

        public static class Nelumbo {
            public Formatting formatting = new Formatting();
            public Classpath  classpath  = new Classpath();
            public boolean    debugging  = false;

            public static class Formatting {
                public PropsSpaceLine propsSpaceLine = PropsSpaceLine.HAS_ANNOTATION;

                public enum PropsSpaceLine {
                    ALWAYS, NEVER, HAS_ANNOTATION
                }
            }

            public static class Classpath {
                public boolean findConfiguration = true;
                public boolean findOtherProject  = true;
            }
        }
    }

    private Setting state = new Setting();

    @Override
    public @Nullable Setting getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull Setting setting) {
        this.state = setting;
    }

    public static PluginSetting getInstance() {
        return ApplicationManager.getApplication().getService(PluginSetting.class);
    }
}
