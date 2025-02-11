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
import org.modelingvalue.collections.Set;

public interface InferResult {

    Set<PredicateImpl> facts();

    Set<PredicateImpl> falsehoods();

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
        public Set<PredicateImpl> cycles() {
            return Set.of();
        }

        @Override
        public List<PredicateImpl> stackOverflow() {
            return null;
        }

        @Override
        public String toString() {
            return asString();
        }
    };

    static InferResult of(Set<PredicateImpl> facts, Set<PredicateImpl> falsehoods, Set<PredicateImpl> cycles) {
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
            public Set<PredicateImpl> cycles() {
                return cycles;
            }

            @Override
            public List<PredicateImpl> stackOverflow() {
                return null;
            }

            @Override
            public String toString() {
                return asString();
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
            public Set<PredicateImpl> cycles() {
                return Set.of();
            }

            @Override
            public List<PredicateImpl> stackOverflow() {
                return null;
            }

            @Override
            public String toString() {
                return asString();
            }
        };
    }

    static InferResult incomplete(PredicateImpl predicate) {
        Set<PredicateImpl> set = Set.of(predicate);
        return new InferResult() {
            @Override
            public Set<PredicateImpl> facts() {
                return set;
            }

            @Override
            public Set<PredicateImpl> falsehoods() {
                return set;
            }

            @Override
            public Set<PredicateImpl> cycles() {
                return Set.of();
            }

            @Override
            public List<PredicateImpl> stackOverflow() {
                return null;
            }

            @Override
            public String toString() {
                return asString();
            }
        };
    }

    static InferResult cycle(PredicateImpl predicate) {
        Set<PredicateImpl> set = Set.of(predicate);
        return new InferResult() {
            @Override
            public Set<PredicateImpl> facts() {
                return set;
            }

            @Override
            public Set<PredicateImpl> falsehoods() {
                return set;
            }

            @Override
            public Set<PredicateImpl> cycles() {
                return set;
            }

            @Override
            public List<PredicateImpl> stackOverflow() {
                return null;
            }

            @Override
            public String toString() {
                return asString();
            }
        };
    }

    static InferResult overflow(List<PredicateImpl> overflow) {
        return new InferResult() {
            @Override
            public Set<PredicateImpl> facts() {
                return null;
            }

            @Override
            public Set<PredicateImpl> falsehoods() {
                return null;
            }

            @Override
            public Set<PredicateImpl> cycles() {
                return Set.of();
            }

            @Override
            public List<PredicateImpl> stackOverflow() {
                return overflow;
            }

            @Override
            public String toString() {
                return asString();
            }
        };
    }

    default InferResult add(InferResult result) {
        Set<PredicateImpl> facts = facts().addAll(result.facts());
        Set<PredicateImpl> falsehoods = falsehoods().addAll(result.falsehoods());
        Set<PredicateImpl> cycles = cycles().addAll(result.cycles());
        return of(facts, falsehoods, cycles);
    }

    default InferResult cast(PredicateImpl to) {
        return of(cast(facts(), to), cast(falsehoods(), to), cycles());
    }

    static Set<PredicateImpl> cast(Set<PredicateImpl> set, PredicateImpl to) {
        return set.replaceAll(p -> p.equals(to) ? to : p.setDeclaration(to));
    }

    default InferResult bind(PredicateImpl from, PredicateImpl to) {
        return of(bind(facts(), from, to), bind(falsehoods(), from, to), cycles());
    }

    static Set<PredicateImpl> bind(Set<PredicateImpl> set, PredicateImpl from, PredicateImpl to) {
        return set.replaceAll(p -> bind(p, from, to));
    }

    static PredicateImpl bind(PredicateImpl pred, PredicateImpl from, PredicateImpl to) {
        return pred.equals(from) ? to : to.setBinding(pred.getBinding(Map.of()));
    }

    default boolean hasCycleWith(PredicateImpl predicate) {
        return cycles().contains(predicate);
    }

    default boolean hasStackOverflow() {
        return stackOverflow() != null;
    }

    default String asString() {
        List<PredicateImpl> overflow = stackOverflow();
        if (overflow != null) {
            return overflow.toString().substring(4);
        } else {
            return toString(facts()) + toString(falsehoods()) + toString(cycles());
        }
    }

    default String toString(Set<PredicateImpl> set) {
        return set.toString().substring(3);
    }

    default boolean equals(InferResult other) {
        return facts().equals(other.facts()) && falsehoods().equals(other.falsehoods()) && cycles().equals(other.cycles());
    }

    default boolean hasOnly(PredicateImpl predicate) {
        return facts().allMatch(predicate::equals) && falsehoods().allMatch(predicate::equals);
    }

    default boolean isIncomplete() {
        return !facts().retainAll(falsehoods()).isEmpty();
    }
}
