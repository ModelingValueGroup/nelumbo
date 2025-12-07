package org.modelingvalue.nelumbo.lsp.intellij;

import com.intellij.lang.Language;

public class Nelumbo extends Language {
    public Nelumbo() {
        super(Constants.LANGUAGE_ID);
    }

    @Override
    public String getDisplayName() {
        return Constants.LANGUAGE_NAME;
    }
}
