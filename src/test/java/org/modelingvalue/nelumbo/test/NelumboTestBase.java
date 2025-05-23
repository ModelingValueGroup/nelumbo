//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//  (C) Copyright 2018-2025 Modeling Value Group B.V. (http://modelingvalue.org)                                         ~
//                                                                                                                       ~
//  Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in       ~
//  compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0   ~
//  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on  ~
//  an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the   ~
//  specific language governing permissions and limitations under the License.                                           ~
//                                                                                                                       ~
//  Maintainers:                                                                                                         ~
//      Wim Bast, Tom Brus                                                                                               ~
//                                                                                                                       ~
//  Contributors:                                                                                                        ~
//      Ronald Krijgsheld ✝, Arjan Kok, Carel Bast                                                                       ~
// --------------------------------------------------------------------------------------------------------------------- ~
//  In Memory of Ronald Krijgsheld, 1972 - 2023                                                                          ~
//      Ronald was suddenly and unexpectedly taken from us. He was not only our long-term colleague and team member      ~
//      but also our friend. "He will live on in many of the lines of code you see below."                               ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.nelumbo.test;

import static org.junit.jupiter.api.Assertions.*;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.Logic;
import org.modelingvalue.nelumbo.Logic.Predicate;
import org.modelingvalue.nelumbo.Logic.Relation;
import org.modelingvalue.nelumbo.Logic.Rule;
import org.modelingvalue.nelumbo.Logic.Variable;
import org.modelingvalue.nelumbo.Result;

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
        assertFalse(!Logic.isFalse(query));
    }

    public static void haveEqualResult(Predicate query1, Predicate query2) {
        assertEquals(Logic.getResult(query1), Logic.getResult(query2));
    }

    public static void hasResult(Predicate query, Set<Predicate> facts, boolean completeFacts, Set<Predicate> falsehoods, boolean completeFalsehoods) {
        Result expectedResult = new Result(facts, completeFacts, falsehoods, completeFalsehoods);
        Result queryResult = Logic.getResult(query);
        assertEquals(expectedResult, queryResult);
    }

    @SafeVarargs
    public static void hasBindings(Predicate query, Map<Variable, Object>... bindings) {
        assertEquals(Set.of(bindings), Logic.getBindings(query));
    }

    public static void print(KnowledgeBase db) {
        for (Entry<Relation, Result> e : db.facts()) {
            System.err.println(e.getKey() + " " + e.getValue());
        }
        for (Entry<Relation, org.modelingvalue.collections.List<Rule>> e : db.rules()) {
            System.err.println(e.getKey() + " " + e.getValue().toString().substring(4));
        }
    }

}
