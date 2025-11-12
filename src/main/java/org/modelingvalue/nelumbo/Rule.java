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

import static org.modelingvalue.nelumbo.KnowledgeBase.TRACE_SYNTATIC;

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParserResult;
import org.modelingvalue.nelumbo.syntax.TokenType;

public final class Rule extends Node implements Evaluatable {
    @Serial
    private static final long serialVersionUID = -4602043866952049391L;

    public Rule(Functor functor, List<AstElement> elements, Predicate consequence, Predicate condition) {
        super(functor, elements, consequence, condition);
    }

    private Rule(Object[] args, Rule declaration) {
        super(args, declaration);
    }

    @Override
    public List<Object> args() {
        return super.args().add(List.of());
    }

    @Override
    protected Rule struct(Object[] array, Node declaration) {
        return new Rule(array, (Rule) declaration);
    }

    public final Functor consequenceFunctor() {
        return consequence().functor();
    }

    public final Predicate consequence() {
        return (Predicate) get(0);
    }

    public final Predicate condition() {
        return (Predicate) get(1);
    }

    protected final InferResult biimply(Predicate predicate, InferContext context, InferResult result) {
        Predicate consequence = consequence();
        Map<Variable, Object> binding = predicate.getBinding(consequence);
        if (binding == null) {
            return result;
        }
        Predicate condition = condition();
        binding = getBinding().putAll(binding);
        consequence = consequence.setBinding(binding);
        condition = condition.setBinding(binding);
        if (context.trace() && (TRACE_SYNTATIC || !isSyntatic())) {
            System.out.println(context.prefix() + consequence + " <=> " + condition);
        }
        InferResult condResult = condition.resolve(context);
        if (condResult.hasStackOverflow()) {
            return condResult;
        }
        Set<Predicate> facts = Set.of(), falsehoods = Set.of();
        boolean completeFacts = true, completeFalsehoods = true;
        for (Predicate condFact : condResult.facts()) {
            Predicate fact = predicate.castFrom(consequence.setBinding(condFact.getBinding()));
            if (fact.isFullyBound()) {
                facts = facts.add(fact);
            } else {
                completeFacts = false;
            }
        }
        for (Predicate condFalsehood : condResult.falsehoods()) {
            Predicate falsehood = predicate.castFrom(consequence.setBinding(condFalsehood.getBinding()));
            if (falsehood.isFullyBound()) {
                falsehoods = falsehoods.add(falsehood);
            } else {
                completeFalsehoods = false;
            }
        }
        if ((facts.isEmpty() && falsehoods.isEmpty()) || !predicate.isFullyBound()) {
            if (!condResult.completeFacts()) {
                completeFacts = false;
            }
            if (!condResult.completeFalsehoods()) {
                completeFalsehoods = false;
            }
        }
        InferResult ruleResult = InferResult.of(facts, completeFacts, falsehoods, completeFalsehoods, //
                condResult.cycles());
        if (context.trace() && (TRACE_SYNTATIC || !isSyntatic())) {
            System.out.println(context.prefix() + predicate + " " + ruleResult);
        }
        for (Predicate fact : result.facts()) {
            if (falsehoods.contains(fact) || (completeFacts && //
                    biimply(fact, context, fact.unknown()).isFalseCC())) {
                throw new InconsistencyException(ruleResult, result);
            }
            facts = facts.add(fact);
        }
        for (Predicate falsehood : result.falsehoods()) {
            if (facts.contains(falsehood) || (completeFalsehoods && //
                    biimply(falsehood, context, falsehood.unknown()).isTrueCC())) {
                throw new InconsistencyException(ruleResult, result);
            }
            falsehoods = falsehoods.add(falsehood);
        }
        completeFacts |= result.completeFacts();
        completeFalsehoods |= result.completeFalsehoods();
        return InferResult.of(facts, completeFacts, falsehoods, completeFalsehoods, //
                result.cycles().addAll(ruleResult.cycles()));
    }

    @Override
    public Rule set(int i, Object... a) {
        return (Rule) super.set(i, a);
    }

    @Override
    public void evaluate(KnowledgeBase knowledgeBase, ParserResult result) throws ParseException {
        knowledgeBase.addRule(this);
    }

    public boolean isSyntatic() {
        return TRACE_SYNTATIC || astElements().isEmpty() || firstToken().fileName().equals("logic.nl");
    }

    @Override
    public String toString(TokenType[] previous) {
        return consequence() + " <=> " + condition();
    }

}
