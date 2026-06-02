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

package org.modelingvalue.nelumbo.collections;

import java.io.Serial;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.ConstructionReason;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.NelumboFunctorField;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.NodeInfo;
import org.modelingvalue.nelumbo.lang.Functor;
import org.modelingvalue.nelumbo.lang.Type;
import org.modelingvalue.nelumbo.lang.Variable;
import org.modelingvalue.nelumbo.logic.InferContext;
import org.modelingvalue.nelumbo.logic.InferResult;
import org.modelingvalue.nelumbo.logic.Predicate;
import org.modelingvalue.nelumbo.logic.Quantifier;
import org.modelingvalue.nelumbo.syntax.ParseContext;
import org.modelingvalue.nelumbo.syntax.ParseException;

public final class BuildSet extends Quantifier {
    @Serial
    private static final long serialVersionUID = -4380336025471527061L;

    @NelumboFunctorField
    private static Functor FUNCTOR;

    @NelumboConstructor
    public BuildSet(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Variable> localVars() {
        return (List<Variable>) get(0);
    }

    public NSet set() {
        return (NSet) get(2);
    }

    @Override
    protected InferResult resolve(InferContext context, InferResult predResult) {
        Variable localVar = localVars().first();
        Type type = localVar.type().nonVariable();
        Map<Variable, Object> clearLocal = Map.of(Entry.of(localVar, localVar));
        boolean completeFacts = predResult.completeFacts(), completeFalsehoods = predResult.completeFalsehoods();
        Set<Predicate> facts = Set.of(), falsehoods = Set.of();
        Map<Predicate, Set<Object>> trueMap = Map.of();
        for (Predicate predFact : predResult.facts()) {
            Object val = predFact.getBinding().get(localVar);
            Predicate fact = predFact.setBinding(clearLocal);
            Set<Object> set = trueMap.get(fact);
            trueMap = trueMap.put(fact, set != null ? set.add(val) : Set.of(val));
        }
        for (Entry<Predicate, Set<Object>> e : trueMap) {
            facts = facts.add(setBinding(e.getKey().getBinding()).set(2, new NSet(type, e.getValue())));
        }
        for (Predicate predFalsehood : predResult.falsehoods()) {
            Object val = predFalsehood.getBinding().get(localVar);
            Predicate falshood = predFalsehood.setBinding(clearLocal);
            falsehoods = falsehoods.add(setBinding(falshood.getBinding()).set(2, new NSet(type, Set.of(val))));
        }
        return InferResult.of(this, facts, completeFacts, falsehoods, completeFalsehoods, predResult.cycles());
    }

    @Override
    public Node init(KnowledgeBase knowledgeBase, ParseContext ctx, ConstructionReason reason) throws ParseException {
        return set(0, List.of(get(0))).resetDeclaration();
    }

}
