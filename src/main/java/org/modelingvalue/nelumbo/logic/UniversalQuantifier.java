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

package org.modelingvalue.nelumbo.logic;

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.InferContext;
import org.modelingvalue.nelumbo.InferResult;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Variable;
import org.modelingvalue.nelumbo.patterns.Functor;

public final class UniversalQuantifier extends Quantifier {
    @Serial
    private static final long serialVersionUID = 8390514736994316431L;

    private static Functor    FUNCTOR;

    static {
        KnowledgeBase.registerFunctorSetter(UniversalQuantifier.class, f -> FUNCTOR = f);
    }

    public UniversalQuantifier(Functor functor, List<AstElement> elements, Object[] args) {
        super(functor, elements, args);
    }

    protected UniversalQuantifier(List<AstElement> elements, List<Variable> localVars, Predicate predicate) {
        super(FUNCTOR, elements, localVars, predicate);
    }

    private UniversalQuantifier(Object[] args, UniversalQuantifier declaration) {
        super(args, declaration);
    }

    @Override
    protected UniversalQuantifier struct(Object[] array, Node declaration) {
        return new UniversalQuantifier(array, (UniversalQuantifier) declaration);
    }

    @Override
    protected InferResult resolve(InferContext context, InferResult predResult) {
        List<Variable> localVars = localVars();
        Set<Predicate> facts = Set.of(), falsehoods = Set.of();
        boolean completeFacts = true, completeFalsehoods = true;
        for (Predicate predFalsehood : predResult.falsehoods()) {
            Predicate falsehood = setBinding(predFalsehood.getBinding().removeAllKey(localVars));
            if (falsehood.isFullyBound()) {
                falsehoods = falsehoods.add(falsehood);
            } else {
                completeFalsehoods = false;
            }
        }
        for (Predicate predFact : predResult.facts()) {
            Predicate fact = setBinding(predFact.getBinding().removeAllKey(localVars));
            if (!falsehoods.contains(fact)) {
                if (fact.isFullyBound()) {
                    facts = facts.add(fact);
                } else {
                    completeFacts = false;
                }
            }
        }
        if (!isFullyBound()) {
            if (!predResult.completeFacts()) {
                completeFacts = false;
            }
            if (!predResult.completeFalsehoods()) {
                completeFalsehoods = false;
            }
        } else if (falsehoods.isEmpty() && facts.isEmpty()) {
            facts = facts.add(this);
        }
        return InferResult.of(facts, completeFacts, falsehoods, completeFalsehoods, predResult.cycles());
    }

}
