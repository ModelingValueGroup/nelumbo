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

package org.modelingvalue.logic.impl;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.logic.Logic;
import org.modelingvalue.logic.Logic.Functor;
import org.modelingvalue.logic.Logic.Predicate;

public final class CollectImpl extends PredicateImpl {
    private static final long                   serialVersionUID      = -2799691054715131197L;

    private static final FunctorImpl<Predicate> COLLECT_FUNCTOR       = FunctorImpl.<Predicate, Predicate, Predicate> of(Logic::collect);
    private static final Functor<Predicate>     COLLECT_FUNCTOR_PROXY = COLLECT_FUNCTOR.proxy();

    private int                                 resultIndex           = -1;

    public CollectImpl(Predicate pred, Predicate accum) {
        super(COLLECT_FUNCTOR_PROXY, pred, accum);
    }

    private CollectImpl(Object[] args) {
        super(args);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected CollectImpl struct(Object[] array) {
        return new CollectImpl(array);
    }

    @SuppressWarnings("rawtypes")
    public final PredicateImpl collector() {
        return (PredicateImpl) get(1);
    }

    @SuppressWarnings("rawtypes")
    public final PredicateImpl accumulator() {
        return (PredicateImpl) get(2);
    }

    @SuppressWarnings("rawtypes")
    private Map<VariableImpl, Object> localVariables;

    @SuppressWarnings("rawtypes")
    protected Map<VariableImpl, Object> localVariables() {
        if (localVariables == null) {
            Map<VariableImpl, Object> collVars = collector().variables();
            Map<VariableImpl, Object> accumVars = accumulator().variables();
            localVariables = collVars.retainAll(accumVars::contains);
        }
        return localVariables;
    }

    @SuppressWarnings("rawtypes")
    private Map<VariableImpl, Object> variables;

    @SuppressWarnings("rawtypes")
    @Override
    public Map<VariableImpl, Object> variables() {
        if (variables == null) {
            Map<VariableImpl, Object> collVars = collector().variables();
            Map<VariableImpl, Object> accumVars = accumulator().variables();
            variables = collVars.removeAll(accumVars::contains).addAll(accumVars.removeAll(collVars::contains));
        }
        return variables;
    }

    private int identityIndex = -1;

    @SuppressWarnings("rawtypes")
    private int identityIndex() {
        if (identityIndex < 0) {
            PredicateImpl accum = accumulator();
            for (int i = 1; i < accum.length(); i++) {
                Object v = accum.get(i);
                if (!(v instanceof VariableImpl) && v instanceof StructureImpl) {
                    Class<?> rt = ((VariableImpl) accum.get(resultIndex())).type();
                    Class<?> at = ((StructureImpl) v).type();
                    if (rt.isAssignableFrom(at)) {
                        identityIndex = i;
                        break;
                    }
                }
            }
        }
        return identityIndex;
    }

    @SuppressWarnings("rawtypes")
    private int resultIndex() {
        if (resultIndex < 0) {
            PredicateImpl accum = accumulator();
            for (int i = 1; i < accum.length(); i++) {
                Object v = accum.get(i);
                if (v instanceof VariableImpl && !localVariables().containsKey((VariableImpl) v)) {
                    resultIndex = i;
                    break;
                }
            }
        }
        return resultIndex;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public InferResult infer(PredicateImpl declaration, InferContext context) {
        Map<VariableImpl, Object> localVars = ((CollectImpl) declaration).localVariables();
        int identityIndex = ((CollectImpl) declaration).identityIndex();
        int resultIndex = ((CollectImpl) declaration).resultIndex();
        PredicateImpl goalColl = ((CollectImpl) declaration).collector();
        PredicateImpl goalAccum = ((CollectImpl) declaration).accumulator();
        PredicateImpl accum = accumulator();
        StructureImpl identity = accum.getVal(identityIndex);
        InferResult result = goalColl.setBinding(collector(), localVars).infer(goalColl, context);
        if (result.hasStackOverflow()) {
            return result;
        }
        Set<List<PredicateImpl>> incomplete = result.incomplete();
        Set<List<PredicateImpl>> falseIncomplete = result.falseIncomplete();
        Set<StructureImpl> facts = Set.of(identity);
        for (PredicateImpl element : result.facts()) {
            Map<VariableImpl, Object> binding = goalColl.getBinding(element, Map.of());
            Set<StructureImpl> res = Set.of();
            for (StructureImpl r : facts) {
                PredicateImpl s = goalAccum.setBinding(accum, binding).set(identityIndex, r);
                result = s.infer(goalAccum, context);
                if (result.hasStackOverflow()) {
                    return result;
                }
                for (PredicateImpl am : result.facts()) {
                    res = res.add(am.getVal(resultIndex));
                }
                incomplete = incomplete.addAll(result.incomplete());
                falseIncomplete = falseIncomplete.addAll(result.falseIncomplete());
            }
            facts = res;
        }
        return InferResult.of(facts.replaceAll(r -> set(2, accum.set(resultIndex, r))), Set.of(), incomplete, falseIncomplete);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Map<VariableImpl, Object> getBinding(StructureImpl<Predicate> pred, Map<VariableImpl, Object> vars) {
        Map<VariableImpl, Object> localVars = localVariables();
        return super.getBinding(pred, vars).removeAll(e -> localVars.containsKey(e.getKey()));
    }

    @Override
    public CollectImpl set(int i, Object... a) {
        return (CollectImpl) super.set(i, a);
    }
}
