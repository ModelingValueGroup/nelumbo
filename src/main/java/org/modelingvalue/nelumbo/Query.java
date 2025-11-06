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

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParserResult;

public final class Query extends Node implements Evaluatable {
    @Serial
    private static final long serialVersionUID = -6751904607718047038L;

    private InferResult       inferResult;

    public Query(Functor functor, List<AstElement> elements, Object... args) {
        super(functor, elements, args(elements, args));
    }

    @SuppressWarnings("unchecked")
    private static Object[] args(List<AstElement> elements, Object[] args) {
        Predicate nodePred = (Predicate) args[0];
        Predicate predicate = nodePred.setVariables(Predicate.literals(nodePred.variables()));
        Optional<List<List<Object>>> expected = (Optional<List<List<Object>>>) args[1];
        if (expected.isEmpty()) {
            return new Object[]{predicate};
        }
        List<Object> facts = expected.get().get(0);
        List<Object> falsehoods = expected.get().get(1);
        Object[] array = new Object[5];
        array[0] = predicate;
        array[1] = bindings(facts.filter(List.class).asList());
        array[2] = !facts.contains("..");
        array[3] = bindings(falsehoods.filter(List.class).asList());
        array[4] = !falsehoods.contains("..");
        return array;
    }

    @SuppressWarnings("unchecked")
    private static Set<Map<Variable, Object>> bindings(List<?> listListList) {
        Set<Map<Variable, Object>> set = Set.of();
        for (List<List<Object>> listList : (List<List<List<Object>>>) listListList) {
            Map<Variable, Object> map = Map.of();
            for (List<Object> list : listList) {
                map = map.put(((Variable) list.get(0)).literal(), (Node) list.get(1));
            }
            set = set.add(map);
        }
        return set;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public List<Object> args() {
        List<Object> args = List.of(predicate());
        if (hasExpected()) {
            List<Object> trueList = List.of();
            for (Map<Variable, Object> binding : facts()) {
                List<List<Object>> list = List.of();
                for (Entry<Variable, Object> e : binding) {
                    list = list.add(List.of(e.getKey(), e.getValue()));
                }
                trueList = trueList.add(list);
            }
            if (!completeFacts()) {
                trueList = trueList.add("..");
            }
            List<Object> falseList = List.of();
            for (Map<Variable, Object> binding : falsehoods()) {
                List<List<Object>> list = List.of();
                for (Entry<Variable, Object> e : binding) {
                    list = list.add(List.of(e.getKey(), e.getValue()));
                }
                falseList = falseList.add(list);
            }
            if (!completeFalsehoods()) {
                falseList = falseList.add("..");
            }
            args = args.add(Optional.of(List.of(trueList, falseList)));
        } else {
            args = args.add(Optional.empty());
        }
        return args;
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
    public Set<Map<Variable, Object>> facts() {
        return (Set<Map<Variable, Object>>) get(1);
    }

    public boolean completeFacts() {
        return (java.lang.Boolean) get(2);
    }

    @SuppressWarnings("unchecked")
    public Set<Map<Variable, Object>> falsehoods() {
        return (Set<Map<Variable, Object>>) get(3);
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
        InferResult found;
        try {
            found = predicate.infer().predicate(predicate);
        } catch (InconsistencyException ie) {
            result.addException(new ParseException(ie.getMessage(), ie.rule()));
            return;
        }
        if (hasExpected()) {
            toString();
            Set<Map<Variable, Object>> trueBindings = facts();
            Set<Predicate> truePredicates = trueBindings.map(b -> predicate.setBinding(b)).asSet();
            boolean completeFacts = completeFacts();
            Set<Map<Variable, Object>> falseBindings = falsehoods();
            Set<Predicate> falsePredicates = falseBindings.map(b -> predicate.setBinding(b)).asSet();
            boolean completeFalsehoods = completeFalsehoods();
            InferResult expected = InferResult.of(predicate, truePredicates, completeFacts, falsePredicates, completeFalsehoods, Set.of());
            if (!found.equals(expected) && !found.toString().equals(expected.toString())) {
                List<AstElement> astElements = astElements();
                result.addException(new ParseException("Expected result " + expected + ", found " + found, //
                        astElements.sublist(2, astElements.size()).toArray(i -> new AstElement[i])));
            }
        }
        inferResult = found;
    }

    public InferResult inferResult() {
        return inferResult;
    }

}
