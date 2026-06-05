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

package org.modelingvalue.nelumbo.logic;

import java.io.Serial;
import java.util.Optional;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.util.Pair;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.ConstructionReason;
import org.modelingvalue.nelumbo.Evaluatable;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.NodeInfo;
import org.modelingvalue.nelumbo.lang.Variable;
import org.modelingvalue.nelumbo.syntax.ParseContext;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParseExceptionHandler;

public final class Query extends Node implements Evaluatable {
    @Serial
    private static final long serialVersionUID = -6751904607718047038L;

    private InferResult inferResult;

    @NelumboConstructor
    public Query(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    @Override
    public Node init(KnowledgeBase knowledgeBase, ParseContext ctx, ConstructionReason reason) throws ParseException {
        if (reason == ConstructionReason.parsing) {
            Predicate nodePred = predicate();
            Predicate predicate = nodePred.setVariables(Predicate.literals(nodePred.getBinding()), ctx);
            List<Node> expected = getVal(1);
            if (expected == null) {
                Object[] array = new Object[1];
                array[0] = predicate;
                return setArgs(array);
            } else {
                List<Object> facts = expected.get(0).args();
                List<Object> falsehoods = expected.get(1).args();
                Object[] array = new Object[5];
                array[0] = predicate;
                array[1] = bindings(facts.filter(List.class).asList());
                array[2] = !facts.contains("..");
                array[3] = bindings(falsehoods.filter(List.class).asList());
                array[4] = !falsehoods.contains("..");
                return setArgs(array);
            }
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    private static Set<Map<Variable, Object>> bindings(List<?> listListList) {
        Set<Map<Variable, Object>> set = Set.of();
        for (List<List<Object>> listList : (List<List<List<Object>>>) listListList) {
            Map<Variable, Object> map = Map.of();
            for (List<Object> list : listList) {
                map = map.put(((Variable) list.get(0)).literal(), list.get(1));
            }
            set = set.add(map);
        }
        return set;
    }

    @Override
    public List<Object> args() {
        List<Object> args = List.of(get(0));
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

    @Override
    public Query setBinding(Map<Variable, Object> vars) {
        vars = vars.replaceAll(e -> e.getKey().type().isLiteral() ? e : Entry.of(e.getKey().literal(), e.getValue()));
        return (Query) super.setBinding(vars);
    }

    @Override
    protected Query set(NodeInfo nodeInfo, Object[] args) {
        return new Query(nodeInfo, args);
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
        return (Boolean) get(2);
    }

    @SuppressWarnings("unchecked")
    public Set<Map<Variable, Object>> falsehoods() {
        return (Set<Map<Variable, Object>>) get(3);
    }

    public boolean completeFalsehoods() {
        return (Boolean) get(4);
    }

    public boolean hasExpected() {
        return length() > 1;
    }

    @Override
    public void evaluate(KnowledgeBase knowledgeBase, ParseExceptionHandler handler) throws ParseException {
        Predicate predicate = predicate();
        InferResult found;
        try {
            found = predicate.infer().predicate(predicate);
        } catch (InconsistencyException ie) {
            handler.addException(new ParseException(ie.getMessage(), predicate));
            return;
        }
        inferResult = found;
        if (hasExpected()) {
            Predicate pred = predicate.setTypes();
            Set<Map<Variable, Object>> trueBindings = facts();
            Set<Predicate> truePredicates = trueBindings.map(pred::setBinding).asSet();
            boolean completeFacts = completeFacts();
            Set<Map<Variable, Object>> falseBindings = falsehoods();
            Set<Predicate> falsePredicates = falseBindings.map(pred::setBinding).asSet();
            boolean completeFalsehoods = completeFalsehoods();
            InferResult expected = InferResult.of(predicate, truePredicates, completeFacts, falsePredicates,
                    completeFalsehoods, Set.of());
            if (!found.equals(expected)) {
                Set<Pair<Object, Object>> diff = found.diff(expected);
                List<AstElement> astElements = astElements();
                handler.addException(new ParseException("Expected result " + expected + ", found " + found, //
                        astElements.sublist(2, astElements.size()).toArray(AstElement[]::new)));
            }
        }
    }

    public InferResult inferResult() {
        return inferResult;
    }

}
