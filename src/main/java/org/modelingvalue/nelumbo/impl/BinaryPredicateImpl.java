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

package org.modelingvalue.nelumbo.impl;

import java.util.concurrent.ThreadLocalRandom;

import org.modelingvalue.nelumbo.Logic.BinaryPredicate;

public abstract class BinaryPredicateImpl<T extends BinaryPredicate<T>> extends PredicateImpl<T> {
    private static final long    serialVersionUID = -928776822979604743L;

    protected static final int[] ZERO_ONE         = new int[]{0, 1};
    protected static final int[] ONE_ZERO         = new int[]{1, 0};

    protected BinaryPredicateImpl(FunctorImpl<T> functor, PredicateImpl<?> predicate1, PredicateImpl<?> predicate2) {
        super(functor, predicate1, predicate2);
    }

    protected BinaryPredicateImpl(Object[] args, BinaryPredicateImpl<T> declaration) {
        super(args, declaration);
    }

    @Override
    public BinaryPredicateImpl<T> declaration() {
        return (BinaryPredicateImpl<T>) super.declaration();
    }

    @SuppressWarnings("rawtypes")
    public final PredicateImpl<?> predicate1() {
        return (PredicateImpl) get(1);
    }

    @SuppressWarnings("rawtypes")
    public final PredicateImpl<?> predicate2() {
        return (PredicateImpl) get(2);
    }

    @SuppressWarnings("rawtypes")
    private PredicateImpl<?> predicate(int i) {
        return (PredicateImpl) get(i + 1);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    protected final InferResult infer(InferContext context) {
        PredicateImpl[] predicate = new PredicateImpl[2];
        InferResult[] predResult = new InferResult[2];
        for (int i : order()) {
            predicate[i] = predicate(i);
            predResult[i] = predicate[i].infer(context);
            if (predResult[i].hasStackOverflow()) {
                return predResult[i];
            } else if (context.reduce()) {
                if (isTrue(predResult[i])) {
                    return BooleanImpl.TRUE_CONCLUSION;
                } else if (isFalse(predResult[i])) {
                    return BooleanImpl.FALSE_CONCLUSION;
                }
            }
        }
        if (context.reduce()) {
            if (isTrue(predResult)) {
                return BooleanImpl.TRUE_CONCLUSION;
            } else if (isFalse(predResult)) {
                return BooleanImpl.FALSE_CONCLUSION;
            } else if (isLeft(predResult)) {
                return predResult[0];
            } else if (isRight(predResult)) {
                return predResult[1];
            } else {
                return set(1, predResult[0].unknown(), predResult[1].unknown()).unknown();
            }
        } else {
            if (!predResult[0].isEmpty() && predResult[1].isEmpty()) {
                return predResult[0];
            } else if (predResult[0].isEmpty() && !predResult[1].isEmpty()) {
                return predResult[1];
            } else {
                return predResult[0].add(predResult[1]);
            }
        }
    }

    protected abstract boolean isTrue(InferResult predResult);

    protected abstract boolean isFalse(InferResult predResult);

    protected abstract boolean isTrue(InferResult[] predResult);

    protected abstract boolean isFalse(InferResult[] predResult);

    protected abstract boolean isLeft(InferResult[] predResult);

    protected abstract boolean isRight(InferResult[] predResult);

    protected int[] order() {
        if (REVERSE_NELUMBO) {
            return ONE_ZERO;
        } else if (RANDOM_NELUMBO) {
            return ThreadLocalRandom.current().nextBoolean() ? ONE_ZERO : ZERO_ONE;
        } else {
            return ZERO_ONE;
        }
    }

}
