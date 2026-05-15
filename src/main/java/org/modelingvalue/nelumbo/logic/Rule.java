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

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.ConstructionReason;
import org.modelingvalue.nelumbo.Evaluatable;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.NelumboFunctorField;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.collections.NList;
import org.modelingvalue.nelumbo.lang.Functor;
import org.modelingvalue.nelumbo.lang.FunctorOrType;
import org.modelingvalue.nelumbo.lang.Type;
import org.modelingvalue.nelumbo.lang.Variable;
import org.modelingvalue.nelumbo.syntax.ParseContext;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParseExceptionHandler;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;

public final class Rule extends Node implements Evaluatable {
    @Serial
    private static final long serialVersionUID = -4602043866952049391L;

    @NelumboFunctorField
    private static Functor FUNCTOR;

    @NelumboConstructor
    public Rule(FunctorOrType functorOrType, List<AstElement> elements, Node declaration, Object... args) {
        super(functorOrType, elements, declaration, args);
    }

    public Rule(List<AstElement> elements, Predicate consequence, Predicate condition) {
        super(FUNCTOR, elements, null, consequence, condition);
    }

    @Override
    public List<Object> args() {
        return super.args().add(List.of());
    }

    @Override
    protected Rule set(FunctorOrType functorOrType, List<AstElement> elements, Node declaration, Object[] array) {
        return new Rule(functorOrType, elements, declaration, array);
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

    @SuppressWarnings("unchecked")

    @Override
    public Node init(KnowledgeBase knowledgeBase, ParseContext ctx, ConstructionReason reason) throws ParseException {
        if (reason == ConstructionReason.transforming) {
            return this;
        }
        Predicate p = consequence();
        Functor consFunctor = p.functor();
        Functor litFunctor = knowledgeBase.literal(consFunctor);
        if (Type.FACT_TYPE.isAssignableFrom((litFunctor != null ? litFunctor : consFunctor).resultType())) {
            knowledgeBase.addException(
                    new ParseException("Rule consequence " + p + " must be a Predicate, not a FactType", p));
        }
        List<AstElement> elements = astElements();
        NList roots = new NList(elements.sublist(0, 2), Type.ROOT);
        Node l = p instanceof NIs ? (Node) p.get(0) : p;
        Predicate cons = (Predicate) p.replace(e -> e != p && e instanceof BooleanVariable v ? v.variable() : e)
                .resetDeclaration();
        Map<Variable, Object> consVars = cons.getBinding();
        Map<Variable, Object> nodeVars = l == cons ? consVars : l.getBinding();
        Functor nodeFunctor = l.functor();
        Functor literalFunctor = nodeFunctor != null ? knowledgeBase.literal(nodeFunctor) : null;
        int i = 0;
        for (List<Object> condIf : (List<List<Object>>) get(1)) {
            Predicate cond = (Predicate) condIf.get(0);
            Predicate when = (Predicate) ((Optional<Object>) condIf.get(1)).orElse(null);
            Map<Variable, Object> condVars = cond.getBinding();
            Map<Variable, Object> whenVars = when != null ? when.getBinding() : null;
            Map<Variable, Object> nonConsVars = (when != null ? condVars.addAll(whenVars) : condVars)
                    .removeAllKey(consVars);
            if (!nonConsVars.isEmpty()) {
                Map<Variable, Object> localVars = nonConsVars.removeAllKey(cond.allLocalVars());
                if (when != null) {
                    localVars = localVars.removeAllKey(when.allLocalVars());
                }
                if (!localVars.isEmpty()) {
                    String message = "Rule has local variables " + nonConsVars.map(e -> e.getKey().toString())
                            .reduce("", (a, b) -> a.isEmpty() ? b : a + "," + b) + " in condition";
                    knowledgeBase.addException(
                            when != null ? new ParseException(message, cond, when) : new ParseException(message, cond));
                }
            }
            if (literalFunctor != null) {
                Map<Variable, Object> litVars = Predicate.literals(nodeVars.putAll(nonConsVars));
                cons = cons.setVariables(litVars, ctx);
                cond = cond.setVariables(litVars, ctx);
                if (when != null) {
                    when = when.setVariables(litVars, ctx);
                }
            } else if (!nonConsVars.isEmpty()) {
                Map<Variable, Object> litVars = Predicate.literals(nonConsVars);
                cond = cond.setVariables(litVars, ctx);
                if (when != null) {
                    when = when.setVariables(litVars, ctx);
                }
            }
            Rule rule = new Rule(when != null ? List.of(cond, when) : List.of(cond), //
                    cons, when != null ? When.of(when, cond) : cond);
            roots = new NList(List.of(), roots, rule);
            for (i++; i < elements.size(); i++) {
                if (elements.get(i) instanceof Token t && t.text().equals(",")) {
                    roots = roots.setAstElements(roots.astElements().add(t));
                    break;
                }
            }
        }
        return roots;
    }

    public final InferResult biimply(Predicate predicate, InferContext context, InferResult result) {
        Predicate consequence = consequence();
        Map<Variable, Object> binding = predicate.getBinding(consequence);
        if (binding == null) {
            return result;
        }
        Predicate condition = condition();
        binding = getBinding().putAll(binding);
        consequence = consequence.setBinding(binding);
        condition = condition.setBinding(binding);
        if (context.trace() && !isSyntatic()) {
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
        InferResult ruleResult = InferResult.of(predicate, facts, completeFacts, falsehoods, completeFalsehoods, //
                condResult.cycles());
        if (context.trace() && !isSyntatic()) {
            System.out.println(context.prefix() + consequence + " " + ruleResult.predicate(consequence.setVariables()));
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
        return InferResult.of(predicate, facts, completeFacts, falsehoods, completeFalsehoods, //
                result.cycles().addAll(ruleResult.cycles()));
    }

    @Override
    public Rule set(int i, Object... a) {
        return (Rule) super.set(i, a);
    }

    @Override
    public void evaluate(KnowledgeBase knowledgeBase, ParseExceptionHandler handler) throws ParseException {
        knowledgeBase.addRule(this);
    }

    @Override
    public String toString(TokenType[] previous) {
        return consequence() + " <=> " + condition();
    }

}
