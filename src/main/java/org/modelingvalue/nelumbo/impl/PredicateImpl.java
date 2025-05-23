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
import org.modelingvalue.collections.util.Pair;
import org.modelingvalue.nelumbo.Logic.Functor;
import org.modelingvalue.nelumbo.Logic.Predicate;

public abstract class PredicateImpl<P extends Predicate> extends StructureImpl<P> {

    private static final long           serialVersionUID = -1605559565948158856L;

    private final Set<PredicateImpl<?>> singleton        = Set.of(this);
    private final PredicateImpl<P>      declaration;

    private StructureImpl<?>            parent;
    private int                         parentIdx;

    @SuppressWarnings("unchecked")
    protected PredicateImpl(Functor<P> functor, Object... args) {
        super(functor, args);
        this.declaration = this;
    }

    protected PredicateImpl(FunctorImpl<P> functor, Object... args) {
        super(functor, args);
        this.declaration = this;
    }

    protected void init(StructureImpl<?> parent, int idx) {
        assert (this.parent == null && this.parentIdx == 0);
        this.parent = parent;
        this.parentIdx = idx;
    }

    private Pair<StructureImpl<?>, int[]> rootIdx() {
        if (parent != null) {
            Pair<StructureImpl<?>, int[]> root = parent instanceof PredicateImpl ? ((PredicateImpl<?>) parent).rootIdx() : null;
            if (root != null) {
                int[] idx = new int[root.b().length + 1];
                System.arraycopy(root.b(), 0, idx, 1, root.b().length);
                idx[0] = parentIdx;
                return Pair.of(root.a(), idx);
            } else {
                return Pair.of(parent, new int[]{parentIdx});
            }
        } else {
            return null;
        }
    }

    protected StructureImpl<?> root() {
        Pair<StructureImpl<?>, int[]> ri = rootIdx();
        return ri != null ? ri.a().set(ri.b(), BooleanImpl.TRUE) : null;
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

    protected PredicateImpl<P> clearDeclaration() {
        return struct(toArray(), (PredicateImpl<P>) null);
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
        Map<VariableImpl, Object> vars = getBinding(declaration, Map.of(), false);
        vars = vars.replaceAll(e -> e.getValue() instanceof Class ? Entry.of(e.getKey(), e.getKey()) : e);
        return setBinding(vars);
    }

    @SuppressWarnings("rawtypes")
    public Map<VariableImpl, Object> getBinding() {
        return super.getBinding(declaration, Map.of(), false);
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
        return (PredicateImpl) super.set(i, a);
    }

    protected PredicateImpl<?> replace(PredicateImpl<?> from, PredicateImpl<?> to) {
        return (PredicateImpl<?>) super.replace(from, to);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public PredicateImpl<P> set(int[] idx, Object val) {
        return (PredicateImpl) super.set(idx, val);
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
        return InferResult.unknown(this);
    }

    public final InferResult factCC() {
        return InferResult.factsCC(singleton);
    }

    public final InferResult falsehoodCC() {
        return InferResult.falsehoodsCC(singleton);
    }

    public final InferResult factCI() {
        return InferResult.factsCI(singleton);
    }

    public final InferResult falsehoodIC() {
        return InferResult.falsehoodsCC(singleton);
    }

    public final Set<PredicateImpl<?>> singleton() {
        return singleton;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected InferResult resolve(InferContext context) {
        Map<Map<VariableImpl, Object>, PredicateImpl> now, next = Map.of(Entry.of(getBinding(), this));
        Set<PredicateImpl<?>> facts = Set.of(), falsehoods = Set.of();
        boolean completeFacts = true, completeFalsehoods = true;
        Set<RelationImpl> cycles = Set.of();
        InferContext reduce = context.reduce(true);
        do {
            now = next;
            next = Map.of();
            for (Entry<Map<VariableImpl, Object>, PredicateImpl> entry : now) {
                InferResult result = entry.getValue().infer(reduce);
                if (result.hasStackOverflow()) {
                    return result;
                } else if (result.isFalseCC()) {
                    falsehoods = falsehoods.add(setBinding(entry.getKey()));
                } else if (result.isTrueCC()) {
                    facts = facts.add(setBinding(entry.getKey()));
                } else {
                    PredicateImpl predicate = result.unknown();
                    if (context.trace()) {
                        System.err.println(context.prefix() + "  " + predicate.toString(null));
                    }
                    result = predicate.infer(context);
                    if (result.hasStackOverflow()) {
                        return result;
                    } else if (result.isEmpty()) {
                        completeFacts = false;
                        completeFalsehoods = false;
                        cycles = cycles.addAll(result.cycles());
                    } else {
                        for (PredicateImpl pred : result.facts()) {
                            Map<VariableImpl, Object> binding = entry.getKey().putAll(pred.getBinding());
                            next = next.put(binding, predicate.setBinding(binding).replace(pred, BooleanImpl.TRUE));
                        }
                        for (PredicateImpl pred : result.falsehoods()) {
                            Map<VariableImpl, Object> binding = entry.getKey().putAll(pred.getBinding());
                            next = next.put(binding, predicate.setBinding(binding).replace(pred, BooleanImpl.FALSE));
                        }
                        completeFacts &= result.completeFacts();
                        completeFalsehoods &= result.completeFalsehoods();
                        cycles = cycles.addAll(result.cycles());
                    }
                }
            }
        } while (!next.isEmpty());
        return InferResult.of(facts, completeFacts, falsehoods, completeFalsehoods, cycles);
    }

}
