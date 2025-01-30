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

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;

public interface InferContext {
    KnowledgeBaseImpl knowledgebase();

    List<PredicateImpl> stack();

    Map<PredicateImpl, InferResult> cycleConclusion();

    boolean deep();

    boolean shallow();

    static InferContext of(KnowledgeBaseImpl knowledgebase, List<PredicateImpl> stack, Map<PredicateImpl, InferResult> cyclic, boolean deep, boolean shallow) {
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
            public Map<PredicateImpl, InferResult> cycleConclusion() {
                return cyclic;
            }

            @Override
            public boolean deep() {
                return deep;
            }

            @Override
            public boolean shallow() {
                return shallow;
            }
        };
    }

    default InferContext pushOnStack(PredicateImpl predicate) {
        return of(knowledgebase(), stack().append(predicate), cycleConclusion(), false, false);
    }

    default InferContext putCycleResult(PredicateImpl predicate, InferResult result) {
        return of(knowledgebase(), stack(), cycleConclusion().put(predicate, result), false, false);
    }

    default InferContext deepShallow(boolean deep, boolean shallow) {
        return of(knowledgebase(), stack(), cycleConclusion(), deep, shallow);
    }

    default String prefix() {
        return "NELUMBO " + "  ".repeat(stack().size());
    }

}
