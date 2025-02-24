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
import org.modelingvalue.collections.Set;

public interface InferResult {

    Set<PredicateImpl> facts();

    Set<PredicateImpl> falsehoods();

    Set<RelationImpl> cycles();

    List<RelationImpl> stackOverflow();

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
        public Set<RelationImpl> cycles() {
            return Set.of();
        }

        @Override
        public List<RelationImpl> stackOverflow() {
            return null;
        }

        @Override
        public String toString() {
            return asString();
        }
    };

    static InferResult of(Set<PredicateImpl> facts, Set<PredicateImpl> falsehoods, Set<RelationImpl> cycles) {
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
            public Set<RelationImpl> cycles() {
                return cycles;
            }

            @Override
            public List<RelationImpl> stackOverflow() {
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
            public Set<RelationImpl> cycles() {
                return Set.of();
            }

            @Override
            public List<RelationImpl> stackOverflow() {
                return null;
            }

            @Override
            public String toString() {
                return asString();
            }
        };
    }

    static InferResult unknowns(Set<PredicateImpl> unknowns) {
        return new InferResult() {
            @Override
            public Set<PredicateImpl> facts() {
                return unknowns;
            }

            @Override
            public Set<PredicateImpl> falsehoods() {
                return unknowns;
            }

            @Override
            public Set<RelationImpl> cycles() {
                return Set.of();
            }

            @Override
            public List<RelationImpl> stackOverflow() {
                return null;
            }

            @Override
            public String toString() {
                return asString();
            }
        };
    }

    static InferResult facts(Set<PredicateImpl> facts) {
        return new InferResult() {
            @Override
            public Set<PredicateImpl> facts() {
                return facts;
            }

            @Override
            public Set<PredicateImpl> falsehoods() {
                return Set.of();
            }

            @Override
            public Set<RelationImpl> cycles() {
                return Set.of();
            }

            @Override
            public List<RelationImpl> stackOverflow() {
                return null;
            }

            @Override
            public String toString() {
                return asString();
            }
        };
    }

    static InferResult falsehoods(Set<PredicateImpl> falsehoods) {
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
            public Set<RelationImpl> cycles() {
                return Set.of();
            }

            @Override
            public List<RelationImpl> stackOverflow() {
                return null;
            }

            @Override
            public String toString() {
                return asString();
            }
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static InferResult cycle(RelationImpl relation) {
        Set set = Set.of(relation);
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
            public Set<RelationImpl> cycles() {
                return set;
            }

            @Override
            public List<RelationImpl> stackOverflow() {
                return null;
            }

            @Override
            public String toString() {
                return asString();
            }
        };
    }

    static InferResult overflow(List<RelationImpl> overflow) {
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
            public Set<RelationImpl> cycles() {
                return Set.of();
            }

            @Override
            public List<RelationImpl> stackOverflow() {
                return overflow;
            }

            @Override
            public String toString() {
                return asString();
            }
        };
    }

    default InferResult add(InferResult other) {
        Set<PredicateImpl> facts = facts().addAll(other.facts());
        Set<PredicateImpl> falsehoods = falsehoods().addAll(other.falsehoods());
        Set<RelationImpl> cycles = cycles().addAll(other.cycles());
        return of(facts, falsehoods, cycles);
    }

    default InferResult not() {
        return of(falsehoods(), facts(), cycles());
    }

    default InferResult and(InferResult other) {
        Set<PredicateImpl> facts = facts().retainAll(other.facts());
        Set<PredicateImpl> falsehoods = falsehoods().addAll(other.falsehoods());
        Set<RelationImpl> cycles = cycles().addAll(other.cycles());
        return of(facts, falsehoods, cycles);
    }

    default InferResult or(InferResult other) {
        Set<PredicateImpl> facts = facts().addAll(other.facts());
        Set<PredicateImpl> falsehoods = falsehoods().retainAll(other.falsehoods());
        Set<RelationImpl> cycles = cycles().addAll(other.cycles());
        return of(facts, falsehoods, cycles);
    }

    default InferResult cast(PredicateImpl to) {
        return of(cast(facts(), to), cast(falsehoods(), to), cycles());
    }

    static Set<PredicateImpl> cast(Set<PredicateImpl> set, PredicateImpl to) {
        return set.replaceAll(p -> p.equals(to) ? to : to.from(p));
    }

    default InferResult bind(PredicateImpl from, PredicateImpl to) {
        return of(bind(facts(), from, to), bind(falsehoods(), from, to), cycles());
    }

    static Set<PredicateImpl> bind(Set<PredicateImpl> set, PredicateImpl from, PredicateImpl to) {
        return set.replaceAll(p -> bind(p, from, to));
    }

    static PredicateImpl bind(PredicateImpl pred, PredicateImpl from, PredicateImpl to) {
        return pred.equals(from) ? to : to.setBinding(pred.getBinding());
    }

    default boolean hasCycleWith(PredicateImpl predicate) {
        return cycles().contains(predicate);
    }

    default boolean hasStackOverflow() {
        return stackOverflow() != null;
    }

    default String asString() {
        List<RelationImpl> overflow = stackOverflow();
        if (overflow != null) {
            return overflow.toString().substring(4);
        } else {
            return toString(facts()) + toString(falsehoods()) + toString(cycles());
        }
    }

    default String toString(Set<? extends PredicateImpl> set) {
        return set.toString().substring(3);
    }

    default boolean equals(InferResult other) {
        return facts().equals(other.facts()) && falsehoods().equals(other.falsehoods()) && cycles().equals(other.cycles());
    }

    default boolean hasOnly(PredicateImpl predicate) {
        return facts().allMatch(predicate::equals) && falsehoods().allMatch(predicate::equals);
    }

    default boolean hasUnknown() {
        return !facts().retainAll(falsehoods()).isEmpty();
    }

    default boolean isUnknown() {
        return facts().size() == 1 && facts().equals(falsehoods()) && cycles().isEmpty();
    }

    default boolean hasBindings(PredicateImpl predicate) {
        return !predicate.isFullyBound() && !facts().removeAll(falsehoods()).isEmpty();
    }
}
