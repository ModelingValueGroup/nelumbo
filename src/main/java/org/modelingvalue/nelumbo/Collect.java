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

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.patterns.Functor;

public final class Collect extends Predicate {
    @Serial
    private static final long serialVersionUID = -3084545514049410749L;

    private Variable          resultVar;
    private Variable          iteratorVar;
    private Variable          contextVar;
    private Node              identityCons;
    private int[]             identityIdx;
    private Predicate         identityPred;
    private Predicate         emptyCollector;

    public Collect(Functor functor, List<AstElement> elements, Object[] args) {
        super(functor, elements, args[0], args[1]);
    }

    private Collect(Object[] args, Collect declaration) {
        super(args, declaration);
    }

    private void initDeclaration() {
        if (emptyCollector == null) {
            Map<Variable, Object> condVars = condition().variables();
            Map<Variable, Object> collVars = collector().variables();
            Map<Variable, Object> globalVars = root().variables();
            // result
            Map<Variable, Object> resultVars = collVars.retainAllKey(globalVars).removeAllKey(condVars);
            if (resultVars.size() != 1) {
                throw new IllegalArgumentException("Collect shoud have exactly one (result) variable in the collector (that is not used in the condition), " + resultVars.size() + " found in " + this);
            }
            resultVar = resultVars.get(0).getKey();
            // iterator
            Map<Variable, Object> iteratorVars = collVars.retainAllKey(condVars).removeAllKey(globalVars);
            if (iteratorVars.size() != 1) {
                throw new IllegalArgumentException("Collect shoud have exactly one shared (iterator) variable in the condition and the collector, " + iteratorVars.size() + " found in " + this);
            }
            iteratorVar = iteratorVars.get(0).getKey();
            // context
            Map<Variable, Object> contextVars = condVars.retainAllKey(globalVars).removeAllKey(collVars);
            if (contextVars.size() != 1) {
                throw new IllegalArgumentException("Collect shoud have exactly one (context) variable in the condition, " + contextVars.size() + " found in " + this);
            }
            contextVar = contextVars.get(0).getKey();
            // identity
            Map<Terminal, int[]> collCons = collector().terminals();
            if (collCons.size() != 1) {
                throw new IllegalArgumentException("Collect shoud have exactly one (identity) constant in the collector, " + collCons.size() + " found in " + collector());
            }
            identityCons = collCons.get(0).getKey();
            identityIdx = collCons.get(0).getValue();
            identityPred = collector().set(iteratorVar, identityCons).set(resultVar, identityCons);
            InferResult result = identityPred.infer();
            if (!result.equals(identityPred.factCC())) {
                throw new IllegalArgumentException("The (identity) constant in the collector of is not an identity, hence " + identityPred + " is not true");
            }
            emptyCollector = collector().set(resultVar, resultVar.type());
        }
    }

    private void init() {
        if (emptyCollector == null) {
            Collect decl = declaration();
            decl.initDeclaration();
            resultVar = decl.resultVar;
            iteratorVar = decl.iteratorVar;
            contextVar = decl.contextVar;
            identityCons = decl.identityCons;
            identityIdx = decl.identityIdx;
            identityPred = decl.identityPred;
            emptyCollector = decl.emptyCollector;
        }
    }

    @Override
    protected Collect struct(Object[] array, Predicate declaration) {
        return new Collect(array, (Collect) declaration);
    }

    @Override
    public Collect declaration() {
        return (Collect) super.declaration();
    }

    public Predicate condition() {
        return (Predicate) get(0);
    }

    public Predicate collector() {
        return (Predicate) get(1);
    }

    @Override
    protected InferResult infer(InferContext context) {
        init();
        if (context.reduce() || get(contextVar) instanceof Type) {
            return unknown();
        }
        InferResult condResult = condition().resolve(context);
        if (condResult.hasStackOverflow()) {
            return condResult;
        } else {
            return collect(condResult, context);
        }
    }

    private InferResult collect(InferResult condResult, InferContext context) {
        Predicate condColl;
        Set<Predicate> prev, next = Set.of();
        Set<Predicate> cycles = condResult.cycles();
        boolean complete = condResult.completeFacts();
        if (complete) {
            next = identityPred.singleton();
            for (Predicate condFact : condResult.facts()) {
                prev = next;
                next = Set.of();
                condColl = emptyCollector.set(iteratorVar, condFact.get(iteratorVar));
                for (Predicate prevFact : prev) {
                    Predicate coll = condColl.set(identityIdx, prevFact.get(resultVar));
                    InferResult inferResult = coll.resolve(context);
                    if (inferResult.hasStackOverflow()) {
                        return inferResult;
                    }
                    next = next.addAll(inferResult.facts());
                    cycles = cycles.addAll(inferResult.cycles());
                    complete &= inferResult.completeFacts();
                }
            }
        }
        Object resultVal = collector().get(resultVar);
        if (!(resultVal instanceof Type)) {
            if (next.anyMatch(f -> f.get(resultVar).equals(resultVal))) {
                return InferResult.of(singleton(), true, Set.of(), true, cycles);
            } else if (!complete) {
                return InferResult.of(Set.of(), false, Set.of(), false, cycles);
            } else {
                return InferResult.of(Set.of(), true, singleton(), true, cycles);
            }
        }
        return InferResult.of(next.replaceAll(f -> set(resultVar, f.get(resultVar))), complete, Set.of(), false, cycles);
    }

    @Override
    public Collect set(int i, Object... a) {
        return (Collect) super.set(i, a);
    }

}
