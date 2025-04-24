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

public abstract class PredicateImpl<P extends Predicate> extends StructureImpl<P> {

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

    @SuppressWarnings("unchecked")
    public P proxyWithVariables() {
        return (P) setVariables().proxy();
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

    @SuppressWarnings("rawtypes")
    protected PredicateImpl set(VariableImpl var, Object val) {
        return (PredicateImpl) super.set(declaration, var, val);
    }

    @SuppressWarnings("rawtypes")
    protected final Object get(VariableImpl var) {
        return super.get(declaration, var);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected final PredicateImpl replace(Object from, Object to) {
        return (PredicateImpl) super.replace(from, to);
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
        InferContext context = KnowledgeBaseImpl.CURRENT.get().context();
        PredicateImpl<P> predicate = setBinding(variables());
        if (context.trace()) {
            System.err.println(context.prefix() + "  " + predicate.toString(null));
        }
        InferResult result = predicate.resolve(context);
        if (context.trace()) {
            System.err.println(context.prefix() + "  " + predicate.toString(null) + "\u2192" + result);
        }
        return result;
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
    protected InferResult resolve(InferContext context) {
        Map<Map<VariableImpl, Object>, PredicateImpl> now, next = Map.of(Entry.of(getBinding(), this));
        Set<PredicateImpl<?>> facts = Set.of(), falsehoods = Set.of();
        Set<RelationImpl> cycles = Set.of();
        InferContext reduce = context.reduce(true);
        do {
            now = next;
            next = Map.of();
            for (Entry<Map<VariableImpl, Object>, PredicateImpl> entry : now) {
                if (context.trace()) {
                    System.err.println(context.prefix() + "  " + entry.getValue().toString(null));
                }
                InferResult result = entry.getValue().infer(reduce);
                if (result.hasStackOverflow()) {
                    return result;
                } else if (result.facts().isEmpty()) {
                    falsehoods = falsehoods.add(setBinding(entry.getKey()));
                } else if (result.falsehoods().isEmpty()) {
                    facts = facts.add(setBinding(entry.getKey()));
                } else {
                    PredicateImpl predicate = result.facts().get(0);
                    result = predicate.infer(context);
                    if (result.hasStackOverflow()) {
                        return result;
                    } else if (result.hasOnlyUnknowns()) {
                        facts = facts.add(this);
                        falsehoods = falsehoods.add(this);
                        cycles = cycles.addAll(result.cycles());
                    } else {
                        for (PredicateImpl pred : result.facts()) {
                            if (pred.isFullyBound()) {
                                Map<VariableImpl, Object> binding = entry.getKey().putAll(pred.getBinding());
                                next = next.put(binding, predicate.setBinding(binding));
                            } else {
                                facts = facts.add(this);
                            }
                        }
                        for (PredicateImpl pred : result.falsehoods()) {
                            if (pred.isFullyBound()) {
                                Map<VariableImpl, Object> binding = entry.getKey().putAll(pred.getBinding());
                                next = next.put(binding, predicate.setBinding(binding));
                            } else {
                                falsehoods = falsehoods.add(this);
                            }
                        }
                        cycles = cycles.addAll(result.cycles());
                    }
                }
            }
        } while (!next.isEmpty());
        return InferResult.of(facts, falsehoods, cycles);
    }

}
