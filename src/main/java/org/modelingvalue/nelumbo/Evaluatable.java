package org.modelingvalue.nelumbo;

import org.modelingvalue.nelumbo.syntax.ParseException;

public interface Evaluatable {

    void evaluate(KnowledgeBase knowledgeBase) throws ParseException;

}
