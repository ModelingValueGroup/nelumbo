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

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParserResult;

public final class Rule extends Node implements Evaluatable {
    @Serial
    private static final long serialVersionUID = -4602043866952049391L;

    public Rule(Functor functor, List<AstElement> elements, Predicate consequence, Predicate condition) {
        super(functor, elements, consequence, condition);
    }

    private Rule(Object[] args) {
        super(args);
    }

    @Override
    public List<Object> args() {
        return super.args().add(List.of());
    }

    @Override
    protected Rule struct(Object[] array) {
        return new Rule(array);
    }

    public final Predicate consequence() {
        return (Predicate) get(0);
    }

    public final Predicate condition() {
        return (Predicate) get(1);
    }

    protected final InferResult imply(Predicate proven, InferContext context) {
        Map<Variable, Object> binding = proven.getBinding(consequence(), Map.of(), true);
        if (binding == null) {
            return null;
        }
        binding = variables().putAll(binding);
        Predicate consequence = consequence().setBinding(binding);
        Predicate condition = condition().setBinding(binding);
        if (context.trace()) {
            System.out.println(context.prefix() + consequence + " <==> " + condition);
        }
        InferResult condResult = condition.resolve(context);
        InferResult proResult;
        if (condResult.hasStackOverflow()) {
            proResult = condResult;
        } else {
            Set<Predicate> proFacts = Set.of(), proFalsehoods = Set.of();
            boolean completeFacts = true, completeFalsehoods = true;
            for (Predicate condFact : condResult.facts()) {
                Predicate proFact = proven.castFrom(consequence.setBinding(condFact.getBinding()));
                proFacts = proFacts.add(proFact);
            }
            for (Predicate condFalsehood : condResult.falsehoods()) {
                Predicate proFalsehood = proven.castFrom(consequence.setBinding(condFalsehood.getBinding()));
                if (!proFacts.contains(proFalsehood)) {
                    proFalsehoods = proFalsehoods.add(proFalsehood);
                }
            }
            if (!proven.isFullyBound()) {
                boolean condFullyBound = condition.isFullyBound();
                if (condFullyBound ? proFacts.isEmpty() : !condResult.completeFacts()) {
                    completeFacts = false;
                }
                if (condFullyBound ? proFalsehoods.isEmpty() : !condResult.completeFalsehoods()) {
                    completeFalsehoods = false;
                }
            }
            proResult = InferResult.of(proFacts, completeFacts, proFalsehoods, completeFalsehoods, condResult.cycles());
        }
        if (context.trace()) {
            System.out.println(context.prefix() + proven + " " + proResult);
        }
        return proResult;
    }

    @Override
    public Rule set(int i, Object... a) {
        return (Rule) super.set(i, a);
    }

    @Override
    public void evaluate(KnowledgeBase knowledgeBase, ParserResult result) throws ParseException {
        knowledgeBase.addRule(this);
    }

}
