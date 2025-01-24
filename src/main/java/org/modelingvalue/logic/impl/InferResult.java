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

    Set<PredicateImpl> incomplete();

    Set<PredicateImpl> falseIncomplete();

    Set<PredicateImpl> cycles();

    List<PredicateImpl> stackOverflow();

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
        public Set<PredicateImpl> incomplete() {
            return Set.of();
        }

        @Override
        public Set<PredicateImpl> falseIncomplete() {
            return Set.of();
        }

        @Override
        public Set<PredicateImpl> cycles() {
            return Set.of();
        }

        @Override
        public List<PredicateImpl> stackOverflow() {
            return null;
        }
    };

    static InferResult of(Set<PredicateImpl> facts, Set<PredicateImpl> falsehoods, Set<PredicateImpl> incomplete, Set<PredicateImpl> falseIncomplete, Set<PredicateImpl> cycles) {
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
            public Set<PredicateImpl> incomplete() {
                return incomplete;
            }

            @Override
            public Set<PredicateImpl> falseIncomplete() {
                return falseIncomplete;
            }

            @Override
            public Set<PredicateImpl> cycles() {
                return cycles;
            }

            @Override
            public List<PredicateImpl> stackOverflow() {
                return null;
            }
        };
    }

    static InferResult trueFalse(Set<PredicateImpl> facts, Set<PredicateImpl> falsehoods) {
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
            public Set<PredicateImpl> incomplete() {
                return Set.of();
            }

            @Override
            public Set<PredicateImpl> falseIncomplete() {
                return Set.of();
            }

            @Override
            public Set<PredicateImpl> cycles() {
                return Set.of();
            }

            @Override
            public List<PredicateImpl> stackOverflow() {
                return null;
            }
        };
    }

    static InferResult falseIncomplete(Set<PredicateImpl> falsehoods, PredicateImpl falseIncomplete) {
        Set<PredicateImpl> falseIncompletes = Set.of(falseIncomplete);
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
            public Set<PredicateImpl> incomplete() {
                return Set.of();
            }

            @Override
            public Set<PredicateImpl> falseIncomplete() {
                return falseIncompletes;
            }

            @Override
            public Set<PredicateImpl> cycles() {
                return Set.of();
            }

            @Override
            public List<PredicateImpl> stackOverflow() {
                return null;
            }
        };
    }

    static InferResult incomplete(PredicateImpl incomplete) {
        Set<PredicateImpl> incompletes = Set.of(incomplete);
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
            public Set<PredicateImpl> incomplete() {
                return incompletes;
            }

            @Override
            public Set<PredicateImpl> falseIncomplete() {
                return incompletes;
            }

            @Override
            public Set<PredicateImpl> cycles() {
                return Set.of();
            }

            @Override
            public List<PredicateImpl> stackOverflow() {
                return null;
            }
        };
    }

    static InferResult cycle(PredicateImpl cycle) {
        Set<PredicateImpl> cycles = Set.of(cycle);
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
            public Set<PredicateImpl> incomplete() {
                return Set.of();
            }

            @Override
            public Set<PredicateImpl> falseIncomplete() {
                return Set.of();
            }

            @Override
            public Set<PredicateImpl> cycles() {
                return cycles;
            }

            @Override
            public List<PredicateImpl> stackOverflow() {
                return null;
            }
        };
    }

    static InferResult overflow(List<PredicateImpl> overflow) {
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
            public Set<PredicateImpl> incomplete() {
                return Set.of();
            }

            @Override
            public Set<PredicateImpl> falseIncomplete() {
                return Set.of();
            }

            @Override
            public Set<PredicateImpl> cycles() {
                return Set.of();
            }

            @Override
            public List<PredicateImpl> stackOverflow() {
                return overflow;
            }
        };
    }

    default InferResult add(InferResult result) {
        return of(facts().addAll(result.facts()), //
                falsehoods().addAll(result.falsehoods()), //
                incomplete().addAll(result.incomplete()), //
                falseIncomplete().addAll(result.falseIncomplete()), //
                cycles().addAll(result.cycles()));
    }

    default InferResult bind(PredicateImpl fromDecl, PredicateImpl to, PredicateImpl toDecl) {
        boolean complete = to.isFullyBound();
        Set<PredicateImpl> incomplete = complete ? //
                bind(incomplete().retainAll(falseIncomplete()), fromDecl, to, toDecl) : //
                bind(incomplete(), fromDecl, to, toDecl);
        Set<PredicateImpl> falseIncomplete = complete ? //
                incomplete : //
                bind(falseIncomplete(), fromDecl, to, toDecl);
        return of(bind(facts(), fromDecl, to, toDecl), //
                bind(falsehoods(), fromDecl, to, toDecl), //
                incomplete, falseIncomplete, cycles());
    }

    static Set<PredicateImpl> bind(Set<PredicateImpl> set, PredicateImpl fromDecl, PredicateImpl to, PredicateImpl toDecl) {
        return set.replaceAll(p -> toDecl.setBinding(to, fromDecl.getBinding(p, Map.of())));
    }

    default InferResult not() {
        return of(falsehoods(), facts(), falseIncomplete(), incomplete(), cycles());
    }

    default boolean hasCycleWith(PredicateImpl predicate) {
        return cycles().contains(predicate);
    }

    default boolean hasStackOverflow() {
        return stackOverflow() != null;
    }
}
