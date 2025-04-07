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

package org.modelingvalue.nelumbo.impl;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.Logic.Functor;
import org.modelingvalue.nelumbo.Logic.Predicate;

public abstract class PredicateImpl<P extends Predicate> extends StructureImpl<P> implements ResultCollector {

    private static final long           serialVersionUID = -1605559565948158856L;

    private final Set<PredicateImpl<?>> singleton        = Set.of(this);
    private final PredicateImpl<P>      declaration;

    @SuppressWarnings("unchecked")
    protected PredicateImpl(Functor<P> functor, Object... args) {
        super(functor, args);
        this.declaration = this;
    }

    protected PredicateImpl(FunctorImpl<P> functor, Object... args) {
        super(functor, args);
        this.declaration = this;
    }

    protected PredicateImpl(Object[] args, PredicateImpl<P> declaration) {
        super(args);
        this.declaration = declaration == null ? this : declaration;
    }

    public PredicateImpl<P> declaration() {
        return declaration;
    }

    protected PredicateImpl<P> castFrom(PredicateImpl<?> from) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return PRETTY_NELUMBO ? setVariables().superToString() : super.toString();
    }

    private String superToString() {
        return super.toString();
    }

    @SuppressWarnings("rawtypes")
    private PredicateImpl setVariables() {
        Map<VariableImpl, Object> vars = getBinding(declaration, Map.of());
        vars = vars != null ? vars.replaceAll(e -> e.getValue() instanceof Class ? Entry.of(e.getKey(), e.getKey()) : e) : null;
        return vars != null ? setBinding(vars) : this;
    }

    @SuppressWarnings("rawtypes")
    public Map<VariableImpl, Object> getBinding() {
        return super.getBinding(declaration, Map.of());
    }

    @SuppressWarnings("rawtypes")
    protected final PredicateImpl setBinding(Map<VariableImpl, Object> vars) {
        return (PredicateImpl) super.setBinding(declaration, vars);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected final PredicateImpl<P> struct(Object[] array) {
        return struct(array, declaration);
    }

    protected abstract PredicateImpl<P> struct(Object[] array, PredicateImpl<P> declaration);

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Class getType(int i) {
        Object v = get(i);
        return v instanceof Class ? (Class) v : v instanceof StructureImpl ? ((StructureImpl) v).type() : null;
    }

    @SuppressWarnings("unchecked")
    public InferResult infer() {
        PredicateImpl<P> predicate = setBinding(variables());
        return predicate.resolve(predicate, KnowledgeBaseImpl.CURRENT.get().context(), this);
    }

    protected InferResult expand(InferContext context) {
        throw new UnsupportedOperationException();
    }

    protected abstract InferResult infer(InferContext context);

    @SuppressWarnings("rawtypes")
    public boolean contains(PredicateImpl cond) {
        return equals(cond);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public PredicateImpl<P> set(int i, Object... a) {
        return ((PredicateImpl) super.set(i, a));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public PredicateImpl<P> copy(int from, int to) {
        return (PredicateImpl) super.set(to, get(from));
    }

    @SuppressWarnings("unchecked")
    protected final PredicateImpl<P> set(int i, PredicateImpl<?>... a) {
        Object[] predArray = toArray();
        Object[] declArray = declaration.toArray();
        for (int x = 0; x < a.length; x++) {
            predArray[i + x] = a[x];
            declArray[i + x] = a[x].declaration;
        }
        return struct(predArray, declaration.struct(declArray, null));
    }

    public final InferResult unknown() {
        return InferResult.unknowns(singleton);
    }

    public final InferResult fact() {
        return InferResult.facts(singleton);
    }

    public final InferResult falsehood() {
        return InferResult.falsehoods(singleton);
    }

    public final Set<PredicateImpl<?>> singleton() {
        return singleton;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected final InferResult resolve(PredicateImpl<?> consequence, InferContext context, ResultCollector collector) {
        Map<Map<VariableImpl, Object>, PredicateImpl> now, next = Map.of(Entry.of(getBinding(), this));
        Map<VariableImpl, Object> map;
        InferContext reduce = context.reduceExpand(true, false), expand = context.reduceExpand(false, true);
        PredicateImpl reduced, bindCons;
        InferResult predResult, reducedResult, result = InferResult.EMPTY;
        do {
            now = next;
            next = Map.of();
            for (Entry<Map<VariableImpl, Object>, PredicateImpl> entry : now) {
                if (context.trace()) {
                    System.err.println(context.prefix() + "  " + entry.getValue().toString(null));
                }
                predResult = entry.getValue().infer(reduce);
                if (predResult.hasStackOverflow()) {
                    return predResult;
                } else if (predResult.facts().isEmpty()) {
                    bindCons = consequence.setBinding(entry.getKey());
                    result = collector.addFalsehood(result, bindCons, consequence);
                } else if (predResult.falsehoods().isEmpty()) {
                    bindCons = consequence.setBinding(entry.getKey());
                    result = collector.addFact(result, bindCons, consequence);
                } else {
                    reduced = predResult.facts().get(0);
                    reducedResult = reduced.infer(expand);
                    if (reducedResult.hasStackOverflow()) {
                        return reducedResult;
                    } else {
                        for (PredicateImpl fact : reducedResult.facts()) {
                            if (fact.isFullyBound()) {
                                map = entry.getKey().putAll(fact.getBinding());
                                next = next.put(map, reduced.setBinding(map));
                            } else {
                                result = collector.addIncompleteFact(result, consequence);
                            }
                        }
                        for (PredicateImpl falsehood : reducedResult.falsehoods()) {
                            if (falsehood.isFullyBound()) {
                                map = entry.getKey().putAll(falsehood.getBinding());
                                next = next.put(map, reduced.setBinding(map));
                            } else {
                                result = collector.addIncompleteFalsehood(result, consequence);
                            }
                        }
                        result = result.addCycles(reducedResult.cycles());
                    }
                }
            }
        } while (!next.isEmpty());
        if (context.trace()) {
            System.err.println(context.prefix() + "  " + toString(null) + "\u2192" + result);
        }
        return result;
    }

}
