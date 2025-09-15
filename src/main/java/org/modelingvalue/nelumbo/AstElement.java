package org.modelingvalue.nelumbo;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.syntax.Token;

public interface AstElement {

    Token firstToken();

    Token lastToken();

    static Token firstToken(List<AstElement> elements) {
        for (AstElement element : elements) {
            Token first = element.firstToken();
            if (first != null) {
                return first;
            }
        }
        return null;
    }

    static Token lastToken(List<AstElement> elements) {
        for (AstElement element : elements) {
            Token first = element.firstToken();
            if (first != null) {
                return first;
            }
        }
        return null;
    }

}
