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

    Set<PredicateImpl<?>> facts();

    Set<PredicateImpl<?>> falsehoods();

    boolean completeFacts();

    boolean completeFalsehoods();

    PredicateImpl<?> unknown();

    Set<RelationImpl> cycles();

    List<RelationImpl> stackOverflow();

    static InferResult of(Set<PredicateImpl<?>> facts, boolean completeFacts, Set<PredicateImpl<?>> falsehoods, boolean completeFalsehoods, Set<RelationImpl> cycles) {
        return new InferResult() {
            @Override
            public Set<PredicateImpl<?>> facts() {
                return facts;
            }

            @Override
            public Set<PredicateImpl<?>> falsehoods() {
                return falsehoods;
            }

            @Override
            public boolean completeFacts() {
                return completeFacts;
            }

            @Override
            public boolean completeFalsehoods() {
                return completeFalsehoods;
            }

            @Override
            public PredicateImpl<?> unknown() {
                return null;
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

    static InferResult factsIncompleteFalsehoods(Set<PredicateImpl<?>> facts) {
        return new InferResult() {
            @Override
            public Set<PredicateImpl<?>> facts() {
                return facts;
            }

            @Override
            public Set<PredicateImpl<?>> falsehoods() {
                return Set.of();
            }

            @Override
            public boolean completeFacts() {
                return true;
            }

            @Override
            public boolean completeFalsehoods() {
                return false;
            }

            @Override
            public PredicateImpl<?> unknown() {
                return null;
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

    static InferResult unknown(PredicateImpl<?> unknown) {
        return new InferResult() {
            @Override
            public Set<PredicateImpl<?>> facts() {
                return Set.of();
            }

            @Override
            public Set<PredicateImpl<?>> falsehoods() {
                return Set.of();
            }

            @Override
            public boolean completeFacts() {
                return false;
            }

            @Override
            public boolean completeFalsehoods() {
                return false;
            }

            @Override
            public PredicateImpl<?> unknown() {
                return unknown;
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

    static InferResult completeFacts(Set<PredicateImpl<?>> facts) {
        return new InferResult() {
            @Override
            public Set<PredicateImpl<?>> facts() {
                return facts;
            }

            @Override
            public Set<PredicateImpl<?>> falsehoods() {
                return Set.of();
            }

            @Override
            public boolean completeFacts() {
                return true;
            }

            @Override
            public boolean completeFalsehoods() {
                return true;
            }

            @Override
            public PredicateImpl<?> unknown() {
                return null;
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

    static InferResult completeFalsehoods(Set<PredicateImpl<?>> falsehoods) {
        return new InferResult() {
            @Override
            public Set<PredicateImpl<?>> facts() {
                return Set.of();
            }

            @Override
            public Set<PredicateImpl<?>> falsehoods() {
                return falsehoods;
            }

            @Override
            public boolean completeFacts() {
                return true;
            }

            @Override
            public boolean completeFalsehoods() {
                return true;
            }

            @Override
            public PredicateImpl<?> unknown() {
                return null;
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
        Set cycles = Set.of(relation);
        return new InferResult() {
            @Override
            public Set<PredicateImpl<?>> facts() {
                return Set.of();
            }

            @Override
            public Set<PredicateImpl<?>> falsehoods() {
                return Set.of();
            }

            @Override
            public boolean completeFacts() {
                return false;
            }

            @Override
            public boolean completeFalsehoods() {
                return false;
            }

            @Override
            public PredicateImpl<?> unknown() {
                return relation;
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

    static InferResult overflow(List<RelationImpl> overflow) {
        return new InferResult() {
            @Override
            public Set<PredicateImpl<?>> facts() {
                return null;
            }

            @Override
            public Set<PredicateImpl<?>> falsehoods() {
                return null;
            }

            @Override
            public boolean completeFacts() {
                return false;
            }

            @Override
            public boolean completeFalsehoods() {
                return false;
            }

            @Override
            public PredicateImpl<?> unknown() {
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

    default InferResult addCycles(Set<RelationImpl> cycles) {
        return cycles.isEmpty() ? this : of(facts(), completeFacts(), falsehoods(), completeFalsehoods(), cycles().addAll(cycles));
    }

    default InferResult incompleteFacts() {
        return !completeFacts() ? this : of(facts(), false, falsehoods(), completeFalsehoods(), cycles());
    }

    default InferResult incompleteFalsehood(PredicateImpl<?> falsehood) {
        return !completeFalsehoods() ? this : of(facts(), completeFacts(), falsehoods(), false, cycles());
    }

    default InferResult add(InferResult other) {
        Set<PredicateImpl<?>> facts = facts().addAll(other.facts());
        boolean completeFacts = completeFacts() && other.completeFacts();
        Set<PredicateImpl<?>> falsehoods = falsehoods().addAll(other.falsehoods());
        boolean completeFalsehoods = completeFalsehoods() && other.completeFalsehoods();
        Set<RelationImpl> cycles = cycles().addAll(other.cycles());
        return of(facts, completeFacts, falsehoods, completeFalsehoods, cycles);
    }

    default InferResult not() {
        return of(falsehoods(), completeFalsehoods(), facts(), completeFacts(), cycles());
    }

    default InferResult and(InferResult other) {
        Set<PredicateImpl<?>> facts = facts().retainAll(other.facts());
        boolean completeFacts = completeFacts() || other.completeFacts();
        Set<PredicateImpl<?>> falsehoods = falsehoods().addAll(other.falsehoods());
        boolean completeFalsehoods = completeFalsehoods() && other.completeFalsehoods();
        Set<RelationImpl> cycles = cycles().addAll(other.cycles());
        return of(facts, completeFacts, falsehoods, completeFalsehoods, cycles);
    }

    default InferResult or(InferResult other) {
        Set<PredicateImpl<?>> facts = facts().addAll(other.facts());
        boolean completeFacts = completeFacts() && other.completeFacts();
        Set<PredicateImpl<?>> falsehoods = falsehoods().retainAll(other.falsehoods());
        boolean completeFalsehoods = completeFalsehoods() || other.completeFalsehoods();
        Set<RelationImpl> cycles = cycles().addAll(other.cycles());
        return of(facts, completeFacts, falsehoods, completeFalsehoods, cycles);
    }

    default InferResult complete() {
        return completeFacts() && completeFalsehoods() ? this : of(facts(), true, falsehoods(), true, cycles());
    }

    default InferResult bind(PredicateImpl<?> from, PredicateImpl<?> to) {
        return of(bind(facts(), from, to), completeFacts(), bind(falsehoods(), from, to), completeFalsehoods(), cycles());
    }

    static Set<PredicateImpl<?>> bind(Set<PredicateImpl<?>> set, PredicateImpl<?> from, PredicateImpl<?> to) {
        return set.replaceAll(p -> bind(p, from, to));
    }

    static PredicateImpl<?> bind(PredicateImpl<?> pred, PredicateImpl<?> from, PredicateImpl<?> to) {
        return pred.equals(from) ? to : to.setBinding(pred.getBinding());
    }

    default InferResult cast(PredicateImpl<?> to) {
        return of(cast(facts(), to), completeFacts(), cast(falsehoods(), to), completeFalsehoods(), cycles());
    }

    static Set<PredicateImpl<?>> cast(Set<PredicateImpl<?>> set, PredicateImpl<?> to) {
        return set.replaceAll(p -> p.equals(to) ? to : to.castFrom(p));
    }

    default boolean hasCycleWith(PredicateImpl<?> predicate) {
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
            return toString(facts(), completeFacts()) + toString(falsehoods(), completeFalsehoods()) + toString(cycles(), true);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    default String toString(Set set, boolean complete) {
        String result = set.toString().substring(3);
        return complete ? result : result.substring(0, result.length() - 1) + (set.isEmpty() ? "" : ",") + "\u2026]";
    }

    default boolean equals(InferResult other) {
        if (other == null) {
            return false;
        }
        return facts().equals(other.facts()) && completeFacts() == other.completeFacts() && //
                falsehoods().equals(other.falsehoods()) && completeFalsehoods() == other.completeFalsehoods() && //
                cycles().equals(other.cycles());
    }

    default boolean isTrue() {
        return falsehoods().isEmpty() && completeFalsehoods();
    }

    default boolean isFalse() {
        return facts().isEmpty() && completeFacts();
    }

    default boolean isIncomplete() {
        return !completeFacts() && !completeFalsehoods();
    }

    default boolean isEmpty() {
        return facts().isEmpty() && falsehoods().isEmpty();
    }

}
