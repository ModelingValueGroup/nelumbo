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
            public OptionSubject1 optionSubject1 = new OptionSubject1();
            public OptionSubject2 optionSubject2 = new OptionSubject2();

            public static class OptionSubject1 {
                public FutureOptionAEnum futureOptionA = FutureOptionAEnum.FUTURE_OPTION_3;

                public enum FutureOptionAEnum {
                    FUTURE_OPTION_1, FUTURE_OPTION_2, FUTURE_OPTION_3
                }
            }

            public static class OptionSubject2 {
                public boolean optA = true;
                public boolean optB = true;
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
