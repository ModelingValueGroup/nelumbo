package org.modelingvalue.logic.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.modelingvalue.logic.Logic.getBindings;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.logic.KnowledgeBase;
import org.modelingvalue.logic.Logic;
import org.modelingvalue.logic.Logic.Predicate;
import org.modelingvalue.logic.Logic.Relation;
import org.modelingvalue.logic.Logic.Rule;
import org.modelingvalue.logic.Logic.Variable;

public class LogicTestBase {
    // Utilities

    public KnowledgeBase run(Runnable test) {
        return Logic.run(test);
    }

    public KnowledgeBase run(Runnable test, KnowledgeBase init) {
        return Logic.run(test, init);
    }

    public static void isTrue(Predicate query) {
        assertTrue(Logic.isTrue(query));
    }

    public static void isFalse(Predicate query) {
        assertTrue(Logic.isFalse(query));
    }

    @SafeVarargs
    public static void hasBindings(Predicate query, Map<Variable, Object>... bindings) {
        assertEquals(Set.of(bindings), getBindings(query));
    }

    public static void print(KnowledgeBase db) {
        for (Entry<Relation, org.modelingvalue.collections.List<Rule>> e : db.rules()) {
            System.err.println(e.getKey() + " " + e.getValue());
        }
        for (Entry<Relation, Set<Relation>> e : db.facts()) {
            System.err.println(e.getKey() + " " + e.getValue());
        }
    }

}
