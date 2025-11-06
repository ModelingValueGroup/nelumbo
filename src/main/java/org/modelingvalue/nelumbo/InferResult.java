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

import org.modelingvalue.collections.Collection;
import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;

public interface InferResult {

    Set<Predicate> facts();

    Set<Predicate> falsehoods();

    default Collection<Predicate> allFacts() {
        return facts();
    }

    default Collection<Predicate> allFalsehoods() {
        return falsehoods();
    }

    boolean completeFacts();

    boolean completeFalsehoods();

    default Predicate predicate() {
        return null;
    }

    default Set<Predicate> cycles() {
        return Set.of();
    }

    default List<Predicate> stackOverflow() {
        return null;
    }

    default boolean hasCycleWith(Predicate predicate) {
        return cycles().contains(predicate);
    }

    default boolean hasStackOverflow() {
        return stackOverflow() != null;
    }

    default boolean isTrueCC() {
        return allFalsehoods().isEmpty() && !allFacts().isEmpty() && completeFalsehoods() && completeFacts();
    }

    default boolean isFalseCC() {
        return allFacts().isEmpty() && !allFalsehoods().isEmpty() && completeFacts() && completeFalsehoods();
    }

    default boolean isComplete() {
        return completeFacts() || completeFalsehoods();
    }

    default boolean isEmpty() {
        return allFacts().isEmpty() && allFalsehoods().isEmpty() && cycles().isEmpty();
    }

    default Set<Map<Variable, Object>> trueBindings() {
        return allFacts().map(p -> p.getBinding(predicate()).removeAll(e -> e.getValue() instanceof Variable || e.getValue() instanceof Type)).asSet();
    }

    default Set<Map<Variable, Object>> falseBindings() {
        return allFalsehoods().map(p -> p.getBinding(predicate()).removeAll(e -> e.getValue() instanceof Variable || e.getValue() instanceof Type)).asSet();
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
            public Set<Predicate> cycles() {
                return cycles;
            }
        };
    }

    static InferResult of(Predicate predicate, Set<Predicate> facts, boolean completeFacts, Set<Predicate> falsehoods, boolean completeFalsehoods, Set<Predicate> cycles) {
        return new InferResultImpl() {

            @Override
            public Predicate predicate() {
                return predicate;
            }

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
            public Set<Predicate> cycles() {
                return cycles;
            }
        };
    }

    static InferResult of(Collection<Predicate> facts, boolean completeFacts, Collection<Predicate> falsehoods, boolean completeFalsehoods, Set<Predicate> cycles) {
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
            public Collection<Predicate> allFacts() {
                return facts;
            }

            @Override
            public Collection<Predicate> allFalsehoods() {
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
            public Set<Predicate> cycles() {
                return cycles;
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
            public Predicate predicate() {
                return unknown;
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

        };
    }

    static InferResult falsehoodsCI(Predicate falsehood) {
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
                return true;
            }

            @Override
            public boolean completeFalsehoods() {
                return false;
            }

            @Override
            public Predicate predicate() {
                return falsehood;
            }

        };
    }

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
            public Set<Predicate> cycles() {
                return predicate.singleton();
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
            public List<Predicate> stackOverflow() {
                return overflow;
            }
        };
    }

    default InferResult addAnd(InferResult other) {
        List<Predicate> facts = Collection.concat(allFacts(), other.allFacts()).asList();
        boolean completeFacts = completeFacts() || other.completeFacts();
        List<Predicate> falsehoods = Collection.concat(allFalsehoods(), other.allFalsehoods()).asList();
        boolean completeFalsehoods = completeFalsehoods() && other.completeFalsehoods();
        Set<Predicate> cycles = cycles().addAll(other.cycles());
        return of(facts, completeFacts, falsehoods, completeFalsehoods, cycles);
    }

    default InferResult addOr(InferResult other) {
        List<Predicate> facts = Collection.concat(allFacts(), other.allFacts()).asList();
        boolean completeFacts = completeFacts() && other.completeFacts();
        List<Predicate> falsehoods = Collection.concat(allFalsehoods(), other.allFalsehoods()).asList();
        boolean completeFalsehoods = completeFalsehoods() || other.completeFalsehoods();
        Set<Predicate> cycles = cycles().addAll(other.cycles());
        return of(facts, completeFacts, falsehoods, completeFalsehoods, cycles);
    }

    default InferResult flipComplete() {
        return of(allFacts(), completeFalsehoods(), allFalsehoods(), completeFacts(), cycles());
    }

    default InferResult biimply(InferResult ruleResult, Rule rule) {
        if (checkConsistency(ruleResult) || ruleResult.checkConsistency(this)) {
            throw new InconsistencyException(rule, ruleResult, this);
        }
        Set<Predicate> facts = facts().addAll(ruleResult.facts());
        Set<Predicate> falsehoods = falsehoods().addAll(ruleResult.falsehoods());
        boolean completeFacts = completeFacts() || ruleResult.completeFacts();
        boolean completeFalsehoods = completeFalsehoods() || ruleResult.completeFalsehoods();
        Set<Predicate> cycles = cycles().addAll(ruleResult.cycles());
        return of(facts, completeFacts, falsehoods, completeFalsehoods, cycles);
    }

    default boolean checkConsistency(InferResult other) {
        return (other.completeFacts() && !facts().allMatch(other.facts()::contains)) || //
                (other.completeFalsehoods() && !falsehoods().allMatch(other.falsehoods()::contains));
    }

    default InferResult cast(Predicate to) {
        return of(cast(facts(), to), completeFacts(), cast(falsehoods(), to), completeFalsehoods(), cycles());
    }

    default InferResult predicate(Predicate predicate) {
        return of(predicate, facts(), completeFacts(), falsehoods(), completeFalsehoods(), cycles());
    }

    static Set<Predicate> cast(Set<Predicate> set, Predicate to) {
        return set.replaceAll(p -> p.equals(to) ? to : to.castFrom(p));
    }

    abstract class InferResultImpl implements InferResult {
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
                return toString(trueBindings(), completeFacts()) + toString(falseBindings(), completeFalsehoods()) + cycleString;
            }
        }

        private String toString(Set<Map<Variable, Object>> binding, boolean complete) {
            List<String> stringList = binding.map(m -> {
                String map = m.toString();
                return "(" + map.substring(4, map.length() - 1) + ")";
            }).sorted().asList();
            String result = stringList.toString().substring(4);
            return complete ? result : result.substring(0, result.length() - 1) + (binding.isEmpty() ? "..]" : ",..]");
        }

        @Override
        public int hashCode() {
            int h = (completeFacts() ? 3 : 0) + (completeFalsehoods() ? 7 : 0);
            return h + allFacts().hashCode() ^ allFalsehoods().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj == null) {
                return false;
            } else if (!(obj instanceof InferResult other)) {
                return false;
            } else {
                return allFacts().equals(other.allFacts()) && completeFacts() == other.completeFacts() && //
                        allFalsehoods().equals(other.allFalsehoods()) && completeFalsehoods() == other.completeFalsehoods() && //
                        cycles().equals(other.cycles());
            }
        }
    }

}
