//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2026 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
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
import static org.junit.jupiter.api.Assertions.fail;

import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.InferResult;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.U;
import org.modelingvalue.nelumbo.logic.Predicate;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Tokenizer;

@SuppressWarnings("unused")
public class NelumboTestBase {

    static void setProp(String name, String def) {
        String env = System.getenv(name);
        System.setProperty(name, env != null ? env : System.getProperty(name, def));
    }
    // Utilities

    public KnowledgeBase run(Runnable test) {
        return KnowledgeBase.BASE.run(test);
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

    public void testResource(String resource) {
        run(() -> {
            try {
                U.printResults(Parser.parse(NelumboTestBase.class, resource));
            } catch (ParseException e) {
                System.err.println(e.getMessage());
                fail(e);
            }
        });
    }

    public void testString(String text, String name) {
        run(() -> {
            try {
                new Parser(new Tokenizer(text, name).tokenize()).parseEvaluate();
            } catch (ParseException e) {
                System.err.println(e.getMessage());
                fail(e);
            }
        });
    }

}
