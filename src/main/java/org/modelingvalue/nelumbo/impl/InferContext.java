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

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;

public interface InferContext {
    KnowledgeBaseImpl knowledgebase();

    List<PredicateImpl> stack();

    Map<PredicateImpl, InferResult> cycleResult();

    boolean reduce();

    boolean expand();

    boolean trace();

    static InferContext of(KnowledgeBaseImpl knowledgebase, List<PredicateImpl> stack, Map<PredicateImpl, InferResult> cyclic, boolean reduce, boolean expand, boolean trace) {
        return new InferContext() {
            @Override
            public KnowledgeBaseImpl knowledgebase() {
                return knowledgebase;
            }

            @Override
            public List<PredicateImpl> stack() {
                return stack;
            }

            @Override
            public Map<PredicateImpl, InferResult> cycleResult() {
                return cyclic;
            }

            @Override
            public boolean reduce() {
                return reduce;
            }

            @Override
            public boolean expand() {
                return expand;
            }

            @Override
            public boolean trace() {
                return trace;
            }
        };
    }

    default InferContext pushOnStack(PredicateImpl predicate) {
        return of(knowledgebase(), stack().append(predicate), cycleResult(), false, false, trace());
    }

    default InferContext putCycleResult(PredicateImpl predicate, InferResult cycleResult) {
        return of(knowledgebase(), stack(), cycleResult().put(predicate, cycleResult), false, false, trace());
    }

    default InferContext reduceExpand(boolean reduce, boolean expand) {
        return of(knowledgebase(), stack(), cycleResult(), reduce, expand, trace());
    }

    default InferContext trace(boolean trace) {
        return trace == trace() ? this : of(knowledgebase(), stack(), cycleResult(), reduce(), expand(), trace);
    }

    default String prefix() {
        return "NELUMBO: " + "    ".repeat(stack().size());
    }

    default InferResult getCycleResult(PredicateImpl predicate) {
        InferResult result = cycleResult().get(predicate);
        return result != null ? result.cast(predicate) : null;
    }

}
