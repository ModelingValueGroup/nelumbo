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
import org.modelingvalue.collections.Set;

public interface InferResult {

    Set<PredicateImpl> facts();

    Set<PredicateImpl> falsehoods();

    Set<List<PredicateImpl>> incomplete();

    Set<List<PredicateImpl>> falseIncomplete();

    InferResult EMPTY = new InferResult() {
        @Override
        public Set<PredicateImpl> facts() {
            return Set.of();
        }

        @Override
        public Set<PredicateImpl> falsehoods() {
            return Set.of();
        }

        @Override
        public Set<List<PredicateImpl>> incomplete() {
            return Set.of();
        }

        @Override
        public Set<List<PredicateImpl>> falseIncomplete() {
            return Set.of();
        }
    };

    static InferResult of(Set<PredicateImpl> facts, Set<PredicateImpl> falsehoods) {
        return new InferResult() {
            @Override
            public Set<PredicateImpl> facts() {
                return facts;
            }

            @Override
            public Set<PredicateImpl> falsehoods() {
                return falsehoods;
            }

            @Override
            public Set<List<PredicateImpl>> incomplete() {
                return Set.of();
            }

            @Override
            public Set<List<PredicateImpl>> falseIncomplete() {
                return Set.of();
            }
        };
    }

    static InferResult of(Set<PredicateImpl> falsehoods, List<PredicateImpl> falseIncomplete) {
        Set<List<PredicateImpl>> falseIncompletes = Set.of(falseIncomplete);
        return new InferResult() {
            @Override
            public Set<PredicateImpl> facts() {
                return Set.of();
            }

            @Override
            public Set<PredicateImpl> falsehoods() {
                return falsehoods;
            }

            @Override
            public Set<List<PredicateImpl>> incomplete() {
                return Set.of();
            }

            @Override
            public Set<List<PredicateImpl>> falseIncomplete() {
                return falseIncompletes;
            }
        };
    }

    static InferResult of(Set<PredicateImpl> facts, Set<PredicateImpl> falsehoods, Set<List<PredicateImpl>> incomplete, Set<List<PredicateImpl>> falseIncomplete) {
        return new InferResult() {
            @Override
            public Set<PredicateImpl> facts() {
                return facts;
            }

            @Override
            public Set<PredicateImpl> falsehoods() {
                return falsehoods;
            }

            @Override
            public Set<List<PredicateImpl>> incomplete() {
                return incomplete;
            }

            @Override
            public Set<List<PredicateImpl>> falseIncomplete() {
                return falseIncomplete;
            }
        };
    }

    static InferResult of(List<PredicateImpl> incomplete) {
        Set<List<PredicateImpl>> incompletes = Set.of(incomplete);
        return new InferResult() {
            @Override
            public Set<PredicateImpl> facts() {
                return Set.of();
            }

            @Override
            public Set<PredicateImpl> falsehoods() {
                return Set.of();
            }

            @Override
            public Set<List<PredicateImpl>> incomplete() {
                return incompletes;
            }

            @Override
            public Set<List<PredicateImpl>> falseIncomplete() {
                return incompletes;
            }
        };
    }

    default InferResult add(InferResult result) {
        return of(facts().addAll(result.facts()), falsehoods().addAll(result.falsehoods()), //
                incomplete().addAll(result.incomplete()), falseIncomplete().addAll(result.falseIncomplete()));
    }

    default InferResult bind(PredicateImpl fromDecl, PredicateImpl to, PredicateImpl toDecl) {
        boolean complete = to.isFullyBound();
        Set<List<PredicateImpl>> incomplete = complete ? incomplete().retainAll(falseIncomplete()) : incomplete();
        Set<List<PredicateImpl>> falseIncomplete = complete ? incomplete : falseIncomplete();
        return of(facts().replaceAll(p -> toDecl.setBinding(to, fromDecl.getBinding(p, Map.of()))), //
                falsehoods().replaceAll(p -> toDecl.setBinding(to, fromDecl.getBinding(p, Map.of()))), //
                incomplete, falseIncomplete);
    }

    default InferResult not() {
        return of(falsehoods(), facts(), falseIncomplete(), incomplete());
    }

    default boolean hasCycleWith(PredicateImpl predicate) {
        return incomplete().anyMatch(l -> l.last().equals(predicate));
    }

    default List<PredicateImpl> stackOverflow() {
        return incomplete().findAny(l -> l.size() >= PredicateImpl.MAX_LOGIC_DEPTH).orElse(null);
    }

    default boolean hasStackOverflow() {
        return incomplete().anyMatch(l -> l.size() >= PredicateImpl.MAX_LOGIC_DEPTH);
    }
}
