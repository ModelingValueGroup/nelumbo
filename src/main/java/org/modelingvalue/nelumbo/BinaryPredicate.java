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
import java.util.concurrent.ThreadLocalRandom;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.patterns.Functor;

public abstract class BinaryPredicate extends CompoundPredicate {
    @Serial
    private static final long serialVersionUID = -928776822979604743L;

    protected BinaryPredicate(Functor functor, List<AstElement> elements, Object predicate1, Object predicate2) {
        super(functor, elements, predicate1, predicate2);
    }

    protected BinaryPredicate(Type type, List<AstElement> elements, Object predicate1, Object predicate2) {
        super(type, elements, predicate1, predicate2);
    }

    protected BinaryPredicate(Object[] args, BinaryPredicate declaration) {
        super(args, declaration);
    }

    @Override
    public BinaryPredicate declaration() {
        return (BinaryPredicate) super.declaration();
    }

    public final Predicate predicate1() {
        return predicate(0);
    }

    public final Predicate predicate2() {
        return predicate(1);
    }

    private Predicate predicate(int i) {
        Predicate p = getVal(i);
        return p != null ? p : Boolean.UNKNOWN;
    }

    @Override
    protected final InferResult infer(InferContext context) {
        Predicate[] predicate = new Predicate[2];
        InferResult[] predResult = new InferResult[2];
        predicate[0] = predicate(0);
        predicate[1] = predicate(1);
        for (int i = 0; i < 2; i++) {
            predResult[i] = predicate[i].infer(context);
            if (predResult[i].hasStackOverflow()) {
                return predResult[i];
            } else if (context.reduce()) {
                if (isTrue(predResult[i], i)) {
                    return Boolean.TRUE.result();
                } else if (isFalse(predResult[i], i)) {
                    return Boolean.FALSE.result();
                } else if (isUnknown(predResult[i], i)) {
                    return Boolean.UNKNOWN.result();
                }
            }
        }
        if (context.reduce()) {
            if (isTrue(predResult)) {
                return Boolean.TRUE.result();
            } else if (isFalse(predResult)) {
                return Boolean.FALSE.result();
            } else if (isLeft(predResult)) {
                return predResult[0];
            } else if (isRight(predResult)) {
                return predResult[1];
            } else {
                return set(0, predResult[0].predicate(), predResult[1].predicate()).unknown();
            }
        } else {
            return resolvedOnly(predResult);
        }
    }

    protected InferResult resolvedOnly(InferResult[] predResult) {
        if (!predResult[0].unresolvable() && !predResult[1].unresolvable()) {
            return predResult[0].add(predResult[1]);
        } else if (!predResult[0].unresolvable()) {
            return predResult[0];
        } else if (!predResult[1].unresolvable()) {
            return predResult[1];
        } else {
            return InferResult.UNRESOLVABLE;
        }
    }

    protected abstract boolean isTrue(InferResult predResult, int i);

    protected abstract boolean isFalse(InferResult predResult, int i);

    protected abstract boolean isUnknown(InferResult predResult, int i);

    protected abstract boolean isTrue(InferResult[] predResult);

    protected abstract boolean isFalse(InferResult[] predResult);

    protected abstract boolean isLeft(InferResult[] predResult);

    protected abstract boolean isRight(InferResult[] predResult);

    protected boolean order(Predicate[] predicate) {
        if (predicate[0] instanceof Boolean && !(predicate[1] instanceof Boolean)) {
            return false;
        } else if (predicate[1] instanceof Boolean && !(predicate[0] instanceof Boolean)) {
            return flip(predicate);
        } else if (REVERSE_NELUMBO) {
            return flip(predicate);
        } else if (RANDOM_NELUMBO && ThreadLocalRandom.current().nextBoolean()) {
            return flip(predicate);
        } else {
            return false;
        }
    }

    private static boolean flip(Predicate[] predicate) {
        Predicate zero = predicate[0];
        predicate[0] = predicate[1];
        predicate[1] = zero;
        return true;
    }

}
