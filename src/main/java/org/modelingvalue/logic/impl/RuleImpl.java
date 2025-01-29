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

import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.logic.Logic;
import org.modelingvalue.logic.Logic.Functor;
import org.modelingvalue.logic.Logic.Predicate;
import org.modelingvalue.logic.Logic.Relation;
import org.modelingvalue.logic.Logic.Rule;

public final class RuleImpl extends StructureImpl<Rule> {
    private static final long              serialVersionUID   = -4602043866952049391L;

    private static final boolean           TRACE_LOGIC        = Boolean.getBoolean("TRACE_LOGIC");
    private static final FunctorImpl<Rule> RULE_FUNCTOR       = FunctorImpl.<Rule, Relation, Predicate> of(Logic::rule);
    private static final Functor<Rule>     RULE_FUNCTOR_PROXY = RULE_FUNCTOR.proxy();

    @SuppressWarnings("rawtypes")
    private Map<VariableImpl, Object>      variables;

    public RuleImpl(Relation pred, Predicate decl) {
        super(RULE_FUNCTOR_PROXY, pred, decl);
    }

    private RuleImpl(Object[] args) {
        super(args);
    }

    @Override
    protected RuleImpl struct(Object[] array) {
        return new RuleImpl(array);
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
    protected InferResult infer(PredicateImpl consequence, InferContext context) {
        PredicateImpl conseqDecl = consequence();
        Map<VariableImpl, Object> binding = conseqDecl.getBinding(consequence, Map.of());
        if (binding == null) {
            return null;
        }
        PredicateImpl condDecl = condition();
        PredicateImpl condition = condDecl.setBinding(condDecl, variables().putAll(binding));
        InferResult condResult = condition.infer(condDecl, context);
        if (condResult.hasStackOverflow()) {
            return condResult;
        }
        Set<PredicateImpl> fullFacts = InferResult.bind(condResult.facts().filter(PredicateImpl::isFullyBound).asSet(), condDecl, consequence, conseqDecl);
        Set<PredicateImpl> fullFalsehoods = InferResult.bind(condResult.falsehoods().filter(PredicateImpl::isFullyBound).asSet(), condDecl, consequence, conseqDecl);
        Set<PredicateImpl> incFacts = InferResult.bind(condResult.facts().exclude(PredicateImpl::isFullyBound).asSet(), condDecl, consequence, conseqDecl).removeAll(fullFalsehoods);
        Set<PredicateImpl> incFalsehoods = InferResult.bind(condResult.falsehoods().exclude(PredicateImpl::isFullyBound).asSet(), condDecl, consequence, conseqDecl).removeAll(fullFacts);
        InferResult conseqResult = InferResult.of(fullFacts.addAll(incFacts), fullFalsehoods.addAll(incFalsehoods), condResult.cycles());
        if (TRACE_LOGIC) {
            System.err.println("LOGIC " + "  ".repeat(context.stack().size()) + //
                    condDecl.setBinding(condDecl, binding) + " -> " + //
                    conseqResult.facts().toString().substring(3));
        }
        return conseqResult;
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
}
