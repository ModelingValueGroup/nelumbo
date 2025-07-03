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

package org.modelingvalue.nelumbo;

import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;

public class Rule extends Node {
    private static final long   serialVersionUID = -4602043866952049391L;
    public static final Type    TYPE             = new Type(Rule.class, Type.ROOT);
    public static final Functor FUNCTOR          = new Functor(TYPE, "Rule", n -> n.toString(1) + " <== " + n.toString(2), 10, Relation.TYPE, Predicate.TYPE);

    public Rule(Relation consequence, Predicate condition) {
        super(FUNCTOR, consequence, condition);
        KnowledgeBase.CURRENT.get().addRule(this);
    }

    private Rule(Object[] args) {
        super(args);
    }

    @Override
    protected Rule struct(Object[] array) {
        return new Rule(array);
    }

    public final Relation consequence() {
        return (Relation) get(1);
    }

    public final Predicate condition() {
        return (Predicate) get(2);
    }

    protected final InferResult imply(Relation relation, InferContext context) {
        Map<Variable, Object> binding = relation.getBinding(consequence(), Map.of(), true);
        if (binding == null) {
            return null;
        }
        binding = variables().putAll(binding);
        Predicate condition = condition().setBinding(binding);
        Predicate consequence = consequence().setBinding(binding);
        if (context.trace()) {
            System.err.println(context.prefix() + consequence + " <== " + condition);
        }
        InferResult condResult = condition.resolve(context);
        InferResult relResult;
        if (condResult.hasStackOverflow()) {
            relResult = condResult;
        } else {
            Set<Predicate> relFacts = Set.of(), relFalsehoods = Set.of();
            boolean completeFacts = true, completeFalsehoods = true;
            for (Predicate condFact : condResult.facts()) {
                Predicate relFact = relation.castFrom(consequence.setBinding(condFact.getBinding()));
                relFacts = relFacts.add(relFact);
            }
            for (Predicate condFalsehood : condResult.falsehoods()) {
                Predicate relFalsehood = relation.castFrom(consequence.setBinding(condFalsehood.getBinding()));
                if (!relFacts.contains(relFalsehood)) {
                    relFalsehoods = relFalsehoods.add(relFalsehood);
                }
            }
            if (relation.isFullyBound()) {
                if (relFacts.isEmpty() && relFalsehoods.isEmpty()) {
                    relFalsehoods = relation.singleton();
                }
            } else {
                boolean condFullyBound = condition.isFullyBound();
                if (condFullyBound ? relFacts.isEmpty() : !condResult.completeFacts()) {
                    completeFacts = false;
                }
                if (condFullyBound ? relFalsehoods.isEmpty() : !condResult.completeFalsehoods()) {
                    completeFalsehoods = false;
                }
                if (!completeFacts && !relFalsehoods.isEmpty()) {
                    relFalsehoods = Set.of();
                }
            }
            relResult = InferResult.of(relFacts, completeFacts, relFalsehoods, completeFalsehoods, condResult.cycles());
        }
        if (context.trace()) {
            System.err.println(context.prefix() + relation + " " + relResult);
        }
        return relResult;
    }

    @Override
    public Rule set(int i, Object... a) {
        return (Rule) super.set(i, a);
    }

}
