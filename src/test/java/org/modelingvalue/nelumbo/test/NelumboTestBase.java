package org.modelingvalue.nelumbo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.modelingvalue.nelumbo.Logic.getBindings;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.util.Pair;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.Logic;
import org.modelingvalue.nelumbo.Logic.Predicate;
import org.modelingvalue.nelumbo.Logic.Relation;
import org.modelingvalue.nelumbo.Logic.Rule;
import org.modelingvalue.nelumbo.Logic.Variable;

public class NelumboTestBase {
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
        for (Entry<Relation, Pair<Set<Relation>, Set<Relation>>> e : db.facts()) {
            System.err.println(e.getKey() + " " + //
                    e.getValue().a().toString().substring(3) + e.getValue().b().toString().substring(3));
        }
        for (Entry<Relation, org.modelingvalue.collections.List<Rule>> e : db.rules()) {
            System.err.println(e.getKey() + " " + //
                    e.getValue().toString().substring(4));
        }
    }

}
