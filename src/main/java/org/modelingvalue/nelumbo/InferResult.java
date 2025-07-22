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

package org.modelingvalue.nelumbo;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Set;

public interface InferResult {

    Set<Predicate> facts();

    Set<Predicate> falsehoods();

    boolean completeFacts();

    boolean completeFalsehoods();

    Predicate unknown();

    Set<Predicate> cycles();

    List<Predicate> stackOverflow();

    default boolean hasCycleWith(Predicate predicate) {
        return cycles().contains(predicate);
    }

    default boolean hasStackOverflow() {
        return stackOverflow() != null;
    }

    default boolean isTrue() {
        return !facts().isEmpty();
    }

    default boolean isFalse() {
        return facts().isEmpty() && completeFacts();
    }

    default boolean isTrueCC() {
        return falsehoods().isEmpty() && !facts().isEmpty() && completeFalsehoods() && completeFacts();
    }

    default boolean isFalseCC() {
        return facts().isEmpty() && !falsehoods().isEmpty() && completeFacts() && completeFalsehoods();
    }

    default boolean isComplete() {
        return completeFacts() || completeFalsehoods();
    }

    default boolean isEmpty() {
        return facts().isEmpty() && falsehoods().isEmpty();
    }

    default boolean isEmptyII() {
        return facts().isEmpty() && falsehoods().isEmpty() && !completeFacts() && !completeFalsehoods();
    }

    static InferResult of(Set<Predicate> facts, boolean completeFacts, Set<Predicate> falsehoods, boolean completeFalsehoods, Set<Predicate> cycles) {
        return new InferResultImpl() {
            @Override
            public Set<Predicate> facts() {
                return facts;
            }

            @Override
            public Set<Predicate> falsehoods() {
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
            public Predicate unknown() {
                return null;
            }

            @Override
            public Set<Predicate> cycles() {
                return cycles;
            }

            @Override
            public List<Predicate> stackOverflow() {
                return null;
            }
        };
    }

    static InferResult unknown(Predicate unknown) {
        return new InferResultImpl() {
            @Override
            public Set<Predicate> facts() {
                return Set.of();
            }

            @Override
            public Set<Predicate> falsehoods() {
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
            public Predicate unknown() {
                return unknown;
            }

            @Override
            public Set<Predicate> cycles() {
                return Set.of();
            }

            @Override
            public List<Predicate> stackOverflow() {
                return null;
            }
        };
    }

    static InferResult factsCI(Set<Predicate> facts) {
        return new InferResultImpl() {
            @Override
            public Set<Predicate> facts() {
                return facts;
            }

            @Override
            public Set<Predicate> falsehoods() {
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
            public Predicate unknown() {
                return null;
            }

            @Override
            public Set<Predicate> cycles() {
                return Set.of();
            }

            @Override
            public List<Predicate> stackOverflow() {
                return null;
            }
        };
    }

    static InferResult factsIC(Set<Predicate> facts) {
        return new InferResultImpl() {
            @Override
            public Set<Predicate> facts() {
                return facts;
            }

            @Override
            public Set<Predicate> falsehoods() {
                return Set.of();
            }

            @Override
            public boolean completeFacts() {
                return false;
            }

            @Override
            public boolean completeFalsehoods() {
                return true;
            }

            @Override
            public Predicate unknown() {
                return null;
            }

            @Override
            public Set<Predicate> cycles() {
                return Set.of();
            }

            @Override
            public List<Predicate> stackOverflow() {
                return null;
            }
        };
    }

    static InferResult factsCC(Set<Predicate> facts) {
        return new InferResultImpl() {
            @Override
            public Set<Predicate> facts() {
                return facts;
            }

            @Override
            public Set<Predicate> falsehoods() {
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
            public Predicate unknown() {
                return null;
            }

            @Override
            public Set<Predicate> cycles() {
                return Set.of();
            }

            @Override
            public List<Predicate> stackOverflow() {
                return null;
            }
        };
    }

    static InferResult falsehoodsCC(Set<Predicate> falsehoods) {
        return new InferResultImpl() {
            @Override
            public Set<Predicate> facts() {
                return Set.of();
            }

            @Override
            public Set<Predicate> falsehoods() {
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
            public Predicate unknown() {
                return null;
            }

            @Override
            public Set<Predicate> cycles() {
                return Set.of();
            }

            @Override
            public List<Predicate> stackOverflow() {
                return null;
            }
        };
    }

    static InferResult falsehoodsIC(Set<Predicate> falsehoods) {
        return new InferResultImpl() {
            @Override
            public Set<Predicate> facts() {
                return Set.of();
            }

            @Override
            public Set<Predicate> falsehoods() {
                return falsehoods;
            }

            @Override
            public boolean completeFacts() {
                return false;
            }

            @Override
            public boolean completeFalsehoods() {
                return true;
            }

            @Override
            public Predicate unknown() {
                return null;
            }

            @Override
            public Set<Predicate> cycles() {
                return Set.of();
            }

            @Override
            public List<Predicate> stackOverflow() {
                return null;
            }
        };
    }

    static InferResult falsehoodsCI(Set<Predicate> falsehoods) {
        return new InferResultImpl() {
            @Override
            public Set<Predicate> facts() {
                return Set.of();
            }

            @Override
            public Set<Predicate> falsehoods() {
                return falsehoods;
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
            public Predicate unknown() {
                return null;
            }

            @Override
            public Set<Predicate> cycles() {
                return Set.of();
            }

            @Override
            public List<Predicate> stackOverflow() {
                return null;
            }
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static InferResult cycle(Set<Predicate> facts, Set<Predicate> falsehoods, Predicate predicate) {
        return new InferResultImpl() {
            @Override
            public Set<Predicate> facts() {
                return facts;
            }

            @Override
            public Set<Predicate> falsehoods() {
                return falsehoods;
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
            public Predicate unknown() {
                return null;
            }

            @Override
            public Set<Predicate> cycles() {
                return (Set) predicate.singleton();
            }

            @Override
            public List<Predicate> stackOverflow() {
                return null;
            }
        };
    }

    static InferResult overflow(List<Predicate> overflow) {
        return new InferResultImpl() {
            @Override
            public Set<Predicate> facts() {
                return null;
            }

            @Override
            public Set<Predicate> falsehoods() {
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
            public Predicate unknown() {
                return null;
            }

            @Override
            public Set<Predicate> cycles() {
                return Set.of();
            }

            @Override
            public List<Predicate> stackOverflow() {
                return overflow;
            }
        };
    }

    default InferResult addAnd(InferResult other) {
        Set<Predicate> facts = facts().addAll(other.facts());
        boolean completeFacts = completeFacts() || other.completeFacts();
        Set<Predicate> falsehoods = falsehoods().addAll(other.falsehoods());
        boolean completeFalsehoods = completeFalsehoods() && other.completeFalsehoods();
        Set<Predicate> cycles = cycles().addAll(other.cycles());
        return of(facts, completeFacts, falsehoods, completeFalsehoods, cycles);
    }

    default InferResult addOr(InferResult other) {
        Set<Predicate> facts = facts().addAll(other.facts());
        boolean completeFacts = completeFacts() && other.completeFacts();
        Set<Predicate> falsehoods = falsehoods().addAll(other.falsehoods());
        boolean completeFalsehoods = completeFalsehoods() || other.completeFalsehoods();
        Set<Predicate> cycles = cycles().addAll(other.cycles());
        return of(facts, completeFacts, falsehoods, completeFalsehoods, cycles);
    }

    default InferResult flipComplete() {
        return of(facts(), completeFalsehoods(), falsehoods(), completeFacts(), cycles());
    }

    default InferResult or(InferResult other) {
        Set<Predicate> facts = facts().addAll(other.facts());
        Set<Predicate> falsehoods = falsehoods().addAll(other.falsehoods());
        if (facts.anyMatch(Predicate::isFullyBound) || falsehoods.anyMatch(Predicate::isFullyBound)) {
            facts = facts.retainAll(Predicate::isFullyBound);
            falsehoods = falsehoods.retainAll(Predicate::isFullyBound);
        }
        falsehoods = falsehoods.removeAll(facts);
        boolean completeFacts = completeFacts() && other.completeFacts();
        boolean completeFalsehoods = completeFalsehoods() && other.completeFalsehoods();
        Set<Predicate> cycles = cycles().addAll(other.cycles());
        return of(facts, completeFacts, falsehoods, completeFalsehoods, cycles);
    }

    default InferResult complete() {
        return completeFacts() && completeFalsehoods() ? this : of(facts(), true, falsehoods(), true, cycles());
    }

    default InferResult bind(Predicate from, Predicate to) {
        return of(bind(facts(), from, to), completeFacts(), bind(falsehoods(), from, to), completeFalsehoods(), cycles());
    }

    static Set<Predicate> bind(Set<Predicate> set, Predicate from, Predicate to) {
        return set.replaceAll(p -> bind(p, from, to));
    }

    static Predicate bind(Predicate pred, Predicate from, Predicate to) {
        return pred.equals(from) ? to : to.setBinding(pred.getBinding());
    }

    default InferResult cast(Predicate to) {
        return of(cast(facts(), to), completeFacts(), cast(falsehoods(), to), completeFalsehoods(), cycles());
    }

    static Set<Predicate> cast(Set<Predicate> set, Predicate to) {
        return set.replaceAll(p -> p.equals(to) ? to : to.castFrom(p));
    }

    static abstract class InferResultImpl implements InferResult {
        @Override
        public String toString() {
            List<Predicate> overflow = stackOverflow();
            if (overflow != null) {
                return overflow.toString().substring(4);
            } else {
                String cycleString = "";
                if (!cycles().isEmpty()) {
                    cycleString = cycles().toString().substring(3);
                    cycleString = "{" + cycleString.substring(1, cycleString.length() - 1) + "}";
                }
                return toString(facts(), completeFacts()) + toString(falsehoods(), completeFalsehoods()) + cycleString;
            }
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private String toString(Set<Predicate> predicates, boolean complete) {
            List<String> stringList = predicates.map(Object::toString).sorted().asList();
            String result = stringList.toString().substring(4);
            return complete ? result : result.substring(0, result.length() - 1) + (predicates.isEmpty() ? "..]" : ",..]");
        }

        @Override
        public int hashCode() {
            int h = (completeFacts() ? 3 : 0) + (completeFalsehoods() ? 7 : 0);
            return h + facts().hashCode() ^ falsehoods().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj == null) {
                return false;
            } else if (!(obj instanceof InferResult)) {
                return false;
            } else {
                InferResult other = (InferResult) obj;
                return facts().equals(other.facts()) && completeFacts() == other.completeFacts() && //
                        falsehoods().equals(other.falsehoods()) && completeFalsehoods() == other.completeFalsehoods() && //
                        cycles().equals(other.cycles());
            }
        }
    }

}
