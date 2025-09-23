package org.modelingvalue.nelumbo.syntax;

import java.io.Serial;

public class PatternMergeException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -3898909321769326515L;

    public PatternMergeException(String message) {
        super(message);
    }

}
