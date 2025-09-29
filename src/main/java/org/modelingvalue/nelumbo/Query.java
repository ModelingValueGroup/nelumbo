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

package org.modelingvalue.nelumbo;

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.syntax.ParseException;

public final class Query extends Node implements Evaluatable {
    @Serial
    private static final long serialVersionUID = -6751904607718047038L;

    public Query(Functor functor, List<AstElement> elements, Object... args) {
        super(functor, elements, args(elements, args));
    }

    private static Object[] args(List<AstElement> elements, Object[] args) {
        Object[] array = new Object[5];
        array[0] = args[0];
        return array;
    }

    private Query(Object[] array) {
        super(array);
    }

    @Override
    protected Query struct(Object[] array) {
        return new Query(array);
    }

    @Override
    public Query set(int i, Object... a) {
        return (Query) super.set(i, a);
    }

    public Predicate predicate() {
        return (Predicate) get(0);
    }

    @Override
    public void evaluate(KnowledgeBase knowledgeBase) throws ParseException {
        Predicate predicate = predicate();
        predicate = predicate.setVariables(Predicate.literals(predicate.variables()));
        InferResult result = predicate.infer();

        //        Set<Predicate> facts = ((List<Predicate>) l.get(1)).asSet();
        //        boolean completeFacts = true;
        //        if (facts.contains(INCOMPLETE)) {
        //            completeFacts = false;
        //            facts = facts.remove(INCOMPLETE);
        //        }
        //        Set<Predicate> falsehoods = ((List<Predicate>) l.get(2)).asSet();
        //        boolean completeFalsehoods = true;
        //        if (falsehoods.contains(INCOMPLETE)) {
        //            completeFalsehoods = false;
        //            falsehoods = falsehoods.remove(INCOMPLETE);
        //        }
        //        InferResult expected = InferResult.of(facts, completeFacts, falsehoods, completeFalsehoods, Set.of());
        //        InferResult result = l.getVal(0, 1);
        //        if (!result.equals(expected) && !result.toString().equals(expected.toString())) {
        //            throw new ParseException("Expected result " + expected + ", found " + result, Token.concat(l, t));
        //        }
    }

}
