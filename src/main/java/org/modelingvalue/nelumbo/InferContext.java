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

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;

public interface InferContext {
    KnowledgeBase knowledgebase();

    List<Predicate> stack();

    Map<Predicate, InferResult> cycleResult();

    boolean reduce();

    boolean trace();

    static InferContext of(KnowledgeBase knowledgebase, List<Predicate> stack, Map<Predicate, InferResult> cyclic, boolean reduce, boolean trace) {
        return new InferContext() {
            @Override
            public KnowledgeBase knowledgebase() {
                return knowledgebase;
            }

            @Override
            public List<Predicate> stack() {
                return stack;
            }

            @Override
            public Map<Predicate, InferResult> cycleResult() {
                return cyclic;
            }

            @Override
            public boolean reduce() {
                return reduce;
            }

            @Override
            public boolean trace() {
                return trace;
            }
        };
    }

    default InferContext pushOnStack(Predicate predicate) {
        return of(knowledgebase(), stack().append(predicate), cycleResult(), false, trace());
    }

    default InferContext putCycleResult(Predicate predicate, InferResult cycleResult) {
        return of(knowledgebase(), stack(), cycleResult().put(predicate, cycleResult), false, trace());
    }

    default InferContext reduce(boolean reduce) {
        return of(knowledgebase(), stack(), cycleResult(), reduce, trace());
    }

    @SuppressWarnings("unused")
    default InferContext trace(boolean trace) {
        return trace == trace() ? this : of(knowledgebase(), stack(), cycleResult(), reduce(), trace);
    }

    default String prefix() {
        return "NELUMBO: " + "  ".repeat(stack().size());
    }

    default InferResult getCycleResult(Predicate predicate) {
        InferResult result = cycleResult().get(predicate);
        return result != null ? result.cast(predicate) : null;
    }

}
