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

import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.Logic;
import org.modelingvalue.nelumbo.Logic.Functor;
import org.modelingvalue.nelumbo.Logic.Predicate;
import org.modelingvalue.nelumbo.Logic.Relation;
import org.modelingvalue.nelumbo.Logic.Rule;
import org.modelingvalue.nelumbo.Logic.RuleModifier;

public final class RuleImpl extends StructureImpl<Rule> {
    private static final long              serialVersionUID   = -4602043866952049391L;

    private static final FunctorImpl<Rule> RULE_FUNCTOR       = FunctorImpl.<Rule, Relation, Predicate> of(Logic::rule);
    private static final Functor<Rule>     RULE_FUNCTOR_PROXY = RULE_FUNCTOR.proxy();

    @SuppressWarnings("rawtypes")
    private Map<VariableImpl, Object>      variables;
    private final boolean                  trace;

    public RuleImpl(Relation pred, Predicate decl, RuleModifier[] modifiers) {
        super(RULE_FUNCTOR_PROXY, pred, decl);
        trace = has(RuleModifier.trace, modifiers);
    }

    private static boolean has(RuleModifier e, RuleModifier[] modifiers) {
        for (RuleModifier m : modifiers) {
            if (m == e) {
                return true;
            }
        }
        return false;
    }

    private RuleImpl(Object[] args, RuleImpl declaration) {
        super(args, declaration);
        trace = false;
    }

    @Override
    protected RuleImpl struct(Object[] array) {
        return new RuleImpl(array, declaration());
    }

    @Override
    public RuleImpl declaration() {
        return (RuleImpl) super.declaration();
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Map<VariableImpl, Object> variables() {
        if (variables == null) {
            variables = super.variables();
        }
        return variables;
    }

    @SuppressWarnings("rawtypes")
    public final PredicateImpl consequence() {
        return (PredicateImpl) get(1);
    }

    @SuppressWarnings("rawtypes")
    public final PredicateImpl condition() {
        return (PredicateImpl) get(2);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected InferResult infer(PredicateImpl predicate, InferContext context) {
        if (!TRACE_NELUMBO && trace()) {
            context = context.trace(true);
        }
        Map<VariableImpl, Object> binding = predicate.getBinding(consequence(), Map.of());
        if (binding == null) {
            return null;
        }
        binding = variables().putAll(binding);
        PredicateImpl consequence = consequence().setBinding(binding);
        PredicateImpl condition = condition().setBinding(binding);
        if (context.trace()) {
            System.err.println(context.prefix() + condition.toString(null) + "\u21D2" + consequence);
        }
        InferResult condResult = condition.infer(context);
        if (condResult == condition.incomplete()) {
            return predicate.incomplete();
        }
        if (condResult.hasStackOverflow()) {
            return condResult;
        }
        Set<PredicateImpl> consFacts = InferResult.bind(condResult.facts(), condition, consequence);
        Set<PredicateImpl> consFalsehoods = InferResult.bind(condResult.falsehoods(), condition, consequence);
        Set<PredicateImpl> predFacts = InferResult.cast(consFacts, predicate);
        Set<PredicateImpl> predFalsehoods = InferResult.cast(consFalsehoods, predicate);
        if (!consequence.equals(predicate)) {
            predFalsehoods = predFalsehoods.add(predicate);
        }
        InferResult predResult = InferResult.of(predFacts, predFalsehoods, condResult.cycles());
        if (context.trace()) {
            System.err.println(context.prefix() + condition.toString(null) + "\u2192" + predResult);
        }
        return predResult;
    }

    public int rulePrio() {
        return condition().nrOfVariables();
    }

    @Override
    public RuleImpl set(int i, Object... a) {
        return (RuleImpl) super.set(i, a);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<Rule> type() {
        return Rule.class;
    }

    @Override
    public String toString() {
        return PRETTY_NELUMBO ? condition().toString(null) + "\u21D2" + consequence() : super.toString();
    }

    public boolean trace() {
        return trace;
    }
}
