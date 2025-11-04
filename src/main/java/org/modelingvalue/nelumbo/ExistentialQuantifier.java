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
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.patterns.Functor;

public final class ExistentialQuantifier extends Quantifier {
    @Serial
    private static final long serialVersionUID = 7237451657373739426L;

    private static Functor    FUNCTOR;

    static {
        KnowledgeBase.registerFunctorSetter(ExistentialQuantifier.class, f -> FUNCTOR = f);
    }

    public ExistentialQuantifier(Functor functor, List<AstElement> elements, Object[] args) {
        super(functor, elements, args);
    }

    protected ExistentialQuantifier(List<AstElement> elements, List<Variable> localVars, Predicate predicate) {
        super(FUNCTOR, elements, localVars, predicate);
    }

    private ExistentialQuantifier(Object[] args, ExistentialQuantifier declaration) {
        super(args, declaration);
    }

    @Override
    protected ExistentialQuantifier struct(Object[] array, Predicate declaration) {
        return new ExistentialQuantifier(array, (ExistentialQuantifier) declaration);
    }

    @Override
    protected InferResult resolve(InferContext context, InferResult predResult) {
        List<Variable> localVars = localVars();
        Set<Predicate> facts = Set.of(), falsehoods = Set.of();
        for (Predicate predFact : predResult.facts()) {
            Predicate fact = setBinding(predFact.getBinding().removeAllKey(localVars));
            facts = facts.add(fact);
        }
        for (Predicate predFalsehood : predResult.falsehoods()) {
            Predicate falsehood = setBinding(predFalsehood.getBinding().removeAllKey(localVars));
            if (!facts.contains(falsehood)) {
                falsehoods = falsehoods.add(falsehood);
            }
        }
        boolean completeFacts = predResult.completeFacts();
        boolean completeFalsehoods = predResult.completeFalsehoods();
        if (completeFacts && facts.isEmpty() && completeFalsehoods && falsehoods.isEmpty() && isFullyBound()) {
            falsehoods = falsehoods.add(this);
        }
        return InferResult.of(facts, completeFacts, falsehoods, completeFalsehoods, predResult.cycles());
    }

}
