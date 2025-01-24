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

import java.util.function.UnaryOperator;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Set;
import org.modelingvalue.logic.Logic.Predicate;

public abstract class AndOrImpl extends PredicateImpl {
    private static final long                 serialVersionUID = -928776822979604743L;

    private static final int[]                ONE_ARRAY        = new int[]{1};
    private static final int[]                TWO_ARRAY        = new int[]{2};
    private static final UnaryOperator<int[]> ADD_ONE          = a -> {
                                                                   int[] r = new int[a.length + 1];
                                                                   System.arraycopy(a, 0, r, 1, a.length);
                                                                   r[0] = 1;
                                                                   return r;
                                                               };
    private static final UnaryOperator<int[]> ADD_TWO          = a -> {
                                                                   int[] r = new int[a.length + 1];
                                                                   System.arraycopy(a, 0, r, 1, a.length);
                                                                   r[0] = 2;
                                                                   return r;
                                                               };

    private List<int[]>                       idxList;

    protected AndOrImpl(FunctorImpl<Predicate> functor, PredicateImpl predicate1, PredicateImpl predicate2) {
        super(functor, predicate1, predicate2);
    }

    protected AndOrImpl(Object[] args) {
        super(args);
    }

    @SuppressWarnings("rawtypes")
    private List<int[]> idxList() {
        if (idxList == null) {
            List<int[]> l = List.of();
            PredicateImpl predicate1 = predicate1();
            if (equalClass(predicate1)) {
                l = l.prependList(((AndOrImpl) predicate1).idxList().replaceAll(ADD_ONE));
            } else {
                l = l.append(ONE_ARRAY);
            }
            PredicateImpl predicate2 = predicate2();
            if (equalClass(predicate2)) {
                l = l.appendList(((AndOrImpl) predicate2).idxList().replaceAll(ADD_TWO));
            } else {
                l = l.append(TWO_ARRAY);
            }
            idxList = l;
        }
        return idxList;
    }

    @SuppressWarnings("rawtypes")
    public final PredicateImpl predicate1() {
        return (PredicateImpl) get(1);
    }

    @SuppressWarnings("rawtypes")
    public final PredicateImpl predicate2() {
        return (PredicateImpl) get(2);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public final InferResult infer(PredicateImpl declaration, InferContext context) {
        idxList = ((AndOrImpl) declaration).idxList();
        Set<PredicateImpl> facts = Set.of();
        InferResult result = InferResult.EMPTY, tmpResult, andResult;
        Set<AndOrImpl> nextAnds = Set.of(this), prevAnds;
        do {
            prevAnds = nextAnds;
            nextAnds = Set.of();
            outer:
            for (AndOrImpl and : prevAnds) {
                List<int[]> idxl = and.idxList;
                if (idxl.isEmpty()) {
                    facts = facts.add(and);
                } else {
                    tmpResult = InferResult.EMPTY;
                    for (int ii = 0; ii < idxl.size(); ii++) {
                        int[] i = idxl.get(ii);
                        PredicateImpl declPred = declaration.getVal(i);
                        PredicateImpl pred = and.getVal(i);
                        InferResult predResult = flip(pred.infer(declPred, context));
                        if (predResult.hasStackOverflow()) {
                            return predResult;
                        }
                        andResult = predResult.bind(declPred, and, declaration);
                        if (andResult.incomplete().isEmpty()) {
                            List<int[]> iil = idxl.removeIndex(ii);
                            andResult.facts().forEach(f -> ((AndOrImpl) f).idxList = iil);
                            nextAnds = nextAnds.addAll((Set) andResult.facts());
                            result = result.add(andResult);
                            continue outer;
                        } else {
                            tmpResult = tmpResult.add(andResult);
                        }
                    }
                    result = result.add(tmpResult);
                }
            }
        } while (!nextAnds.isEmpty());
        return flip(InferResult.of(facts, result.falsehoods(), result.incomplete(), result.falseIncomplete(), result.cycles()));
    }

    protected abstract boolean equalClass(PredicateImpl predicate);

    protected abstract InferResult flip(InferResult result);
}
