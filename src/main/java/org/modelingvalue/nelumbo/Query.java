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
import java.util.Optional;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParserResult;

public final class Query extends Node implements Evaluatable {
    @Serial
    private static final long serialVersionUID = -6751904607718047038L;

    public Query(Functor functor, List<AstElement> elements, Object... args) {
        super(functor, elements, args(elements, args));
    }

    @SuppressWarnings("unchecked")
    private static Object[] args(List<AstElement> elements, Object[] args) {
        Optional<List<Optional<List<Object>>>> expected = (Optional<List<Optional<List<Object>>>>) args[1];
        if (expected.isEmpty()) {
            return new Object[]{args[0]};
        }
        List<Object> flatFacts = flatten(expected.get().get(0));
        List<Object> flatFalsehoods = flatten(expected.get().get(1));
        Object[] array = new Object[5];
        array[0] = args[0];
        array[1] = flatFacts.filter(Predicate.class).asList();
        array[2] = !flatFacts.contains("..");
        array[3] = flatFalsehoods.filter(Predicate.class).asList();
        array[4] = !flatFalsehoods.contains("..");
        return array;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> flatten(Optional<List<Object>> expected) {
        return expected.orElse(List.of()).replaceAllAll(f -> f instanceof List list ? list : List.of(f));
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

    @SuppressWarnings("unchecked")
    public List<Predicate> facts() {
        return (List<Predicate>) get(1);
    }

    public boolean completeFacts() {
        return (java.lang.Boolean) get(2);
    }

    @SuppressWarnings("unchecked")
    public List<Predicate> falsehoods() {
        return (List<Predicate>) get(3);
    }

    public boolean completeFalsehoods() {
        return (java.lang.Boolean) get(4);
    }

    public boolean hasExpected() {
        return length() > 1;
    }

    @Override
    public void evaluate(KnowledgeBase knowledgeBase, ParserResult result) throws ParseException {
        Predicate predicate = predicate();
        predicate = predicate.setVariables(Predicate.literals(predicate.variables()));
        InferResult infer = predicate.infer();
        if (hasExpected()) {
            Set<Predicate> facts = facts().asSet();
            boolean completeFacts = completeFacts();
            Set<Predicate> falsehoods = falsehoods().asSet();
            boolean completeFalsehoods = completeFalsehoods();
            InferResult expected = InferResult.of(facts, completeFacts, falsehoods, completeFalsehoods, Set.of());
            if (!infer.equals(expected) && !infer.toString().equals(expected.toString())) {
                List<AstElement> astElements = astElements();
                result.addException(new ParseException("Expected result " + expected + ", found " + result, //
                        astElements.sublist(2, astElements.size()).toArray(i -> new AstElement[i])));
            }
        }
    }

}
