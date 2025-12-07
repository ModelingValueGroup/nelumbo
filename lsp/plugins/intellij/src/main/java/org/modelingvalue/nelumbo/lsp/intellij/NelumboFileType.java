package org.modelingvalue.nelumbo.lsp.intellij;

import javax.swing.Icon;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;

public final class NelumboFileType extends LanguageFileType {
    public static final NelumboFileType INSTANCE = new NelumboFileType();

    private NelumboFileType() {
        super(Constants.NELUMBO);
    }

    @NotNull
    @Override
    public String getName() {
        return Constants.LANGUAGE_NAME;
    }

    @NotNull
    @Override
    public String getDescription() {
        return Constants.LANGUAGE_NAME;
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return Constants.EXTENSION;
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return Constants.ICON;
    }
}
