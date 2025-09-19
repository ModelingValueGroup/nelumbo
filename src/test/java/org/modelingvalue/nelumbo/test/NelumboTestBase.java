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

package org.modelingvalue.nelumbo.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.InferResult;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.Predicate;

@SuppressWarnings("unused")
public class NelumboTestBase {
    static void setProp(String name, String def) {
        String env = System.getenv(name);
        System.setProperty(name, env != null ? env : System.getProperty(name, def));
    }
    // Utilities

    public KnowledgeBase run(Runnable test) {
        return KnowledgeBase.run(test);
    }

    public KnowledgeBase run(Runnable test, KnowledgeBase init) {
        return KnowledgeBase.run(test, init);
    }

    public static void isTrue(Predicate pred) {
        InferResult result = getResult(pred);
        assertTrue(result.isTrue());
    }

    public static void isFalse(Predicate pred) {
        InferResult result = getResult(pred);
        assertTrue(result.isFalse());
    }

    public static InferResult getResult(Predicate pred) {
        return pred.infer();
    }

    public static void haveEqualResult(Predicate query1, Predicate query2) {
        assertEquals(getResult(query1), getResult(query2));
    }

    public static void hasResult(Predicate query, Set<Predicate> facts, boolean completeFacts, Set<Predicate> falsehoods, boolean completeFalsehoods) {
        InferResult expectedResult = InferResult.of(facts, completeFacts, falsehoods, completeFalsehoods, Set.of());
        InferResult queryResult = getResult(query);
        assertEquals(expectedResult, queryResult);
    }

}
