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

    protected UniversalQuantifier(List<AstElement> elements, Predicate predicate) {
        super(FUNCTOR, elements, new Object[]{predicate});
    }

    private UniversalQuantifier(Object[] args, UniversalQuantifier declaration) {
        super(args, declaration);
    }

    @Override
    protected UniversalQuantifier struct(Object[] array, Predicate declaration) {
        return new UniversalQuantifier(array, (UniversalQuantifier) declaration);
    }

    @Override
    protected InferResult resolve(InferContext context, InferResult predResult) {
        Set<Predicate> facts = Set.of(), falsehoods = Set.of();
        for (Predicate predFalsehood : predResult.falsehoods()) {
            Predicate falsehood = setBinding(predFalsehood.getBinding().retainAllKey(context.globalVars()));
            falsehoods = falsehoods.add(falsehood);
        }
        for (Predicate predFact : predResult.facts()) {
            Predicate fact = setBinding(predFact.getBinding().retainAllKey(context.globalVars()));
            if (!falsehoods.contains(fact)) {
                facts = facts.add(fact);
            }
        }
        boolean completeFacts = predResult.completeFacts();
        boolean completeFalsehoods = predResult.completeFalsehoods();
        if (completeFacts && facts.isEmpty() && completeFalsehoods && falsehoods.isEmpty() && isFullyBound()) {
            facts = facts.add(this);
        }
        return InferResult.of(facts, completeFacts, falsehoods, completeFalsehoods, predResult.cycles());
    }

}
