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

public abstract class PredicateImpl extends StructureImpl<Predicate> {

    private static final long        serialVersionUID = -1605559565948158856L;

    private final Set<PredicateImpl> singleton        = Set.of(this);
    private final PredicateImpl      declaration;

    @SuppressWarnings("unchecked")
    protected PredicateImpl(Functor<? extends Predicate> functor, Object... args) {
        super((Functor<Predicate>) functor, args);
        this.declaration = this;
    }

    protected PredicateImpl(FunctorImpl<Predicate> functor, Object... args) {
        super(functor, args);
        this.declaration = this;
    }

    protected PredicateImpl(Object[] args, PredicateImpl declaration) {
        super(args);
        this.declaration = declaration;
    }

    public PredicateImpl declaration() {
        return declaration;
    }

    protected PredicateImpl from(PredicateImpl from) {
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
    protected abstract PredicateImpl struct(Object[] array);

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Class getType(int i) {
        Object v = get(i);
        return v instanceof Class ? (Class) v : v instanceof StructureImpl ? ((StructureImpl) v).type() : null;
    }

    public InferResult infer() {
        InferContext context = KnowledgeBaseImpl.CURRENT.get().context();
        if (context.trace()) {
            System.err.println(context.prefix() + toString(null));
        }
        PredicateImpl conditon = setBinding(variables());
        InferResult result = conditon.resolve(conditon, context);
        if (context.trace()) {
            System.err.println(context.prefix() + toString(null) + "\u2192" + result);
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
    public PredicateImpl set(int i, Object... a) {
        return (PredicateImpl) super.set(i, a);
    }

    public PredicateImpl copy(int from, int to) {
        return (PredicateImpl) super.set(to, get(from));
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

    public final Set<PredicateImpl> singleton() {
        return singleton;
    }

    @SuppressWarnings("rawtypes")
    protected InferResult resolve(PredicateImpl consequence, InferContext context) {
        Set<PredicateImpl> now, next = singleton(), bindings, facts = Set.of(), falsehoods = Set.of();
        Set<RelationImpl> cycles = Set.of();
        InferContext reduce = context.reduceExpand(true, false), expand = context.reduceExpand(false, true);
        PredicateImpl reduced;
        InferResult predResult, reducedResult, bindResult;
        boolean consFacts = false, consFalsehoods = false;
        do {
            now = next;
            next = Set.of();
            for (PredicateImpl predicate : now) {
                predResult = predicate.infer(reduce);
                if (predResult.hasStackOverflow()) {
                    return predResult;
                } else if (predResult.facts().isEmpty()) {
                    falsehoods = falsehoods.add(InferResult.bind(predicate, this, consequence));
                } else if (predResult.falsehoods().isEmpty()) {
                    facts = facts.add(InferResult.bind(predicate, this, consequence));
                } else {
                    reduced = predResult.facts().get(0);
                    reducedResult = reduced.infer(expand);
                    if (reducedResult.hasStackOverflow()) {
                        return reducedResult;
                    } else {
                        bindResult = reducedResult.bind(null, predicate);
                        bindings = bindResult.facts().addAll(bindResult.falsehoods()).removeAll(now);
                        next = next.addAll(bindings);
                        cycles = cycles.addAll(bindResult.cycles());
                        if (!bindResult.facts().allMatch(bindings::contains)) {
                            consFacts = true;
                        }
                        if (!bindResult.falsehoods().allMatch(bindings::contains)) {
                            consFalsehoods = true;
                        }
                    }
                }
            }
        } while (!next.isEmpty());
        if (consFacts && falsehoods.contains(consequence)) {
            consFacts = false;
        }
        if (consFalsehoods && facts.contains(consequence)) {
            consFalsehoods = false;
        }
        if (consFacts) {
            facts = facts.add(consequence);
        }
        if (consFalsehoods) {
            falsehoods = falsehoods.add(consequence);
        }
        InferResult consResult = InferResult.of(facts, falsehoods, cycles);
        if (context.trace()) {
            System.err.println(context.prefix() + toString(null) + "\u2192" + consResult);
        }
        return consResult;
    }
}
