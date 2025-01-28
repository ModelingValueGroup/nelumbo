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

    Set<PredicateImpl> cycles();

    List<PredicateImpl> stackOverflow();

    InferResult EMPTY      = new InferResult() {
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

    InferResult INCOMPLETE = new InferResult() {
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

    static InferResult cycles(Set<PredicateImpl> cycles) {
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
        Set<PredicateImpl> facts = add(facts(), result.facts());
        Set<PredicateImpl> falsehoods = add(falsehoods(), result.falsehoods());
        Set<PredicateImpl> cycles = add(cycles(), result.cycles());
        return of(facts, falsehoods, cycles);
    }

    static Set<PredicateImpl> add(Set<PredicateImpl> a, Set<PredicateImpl> b) {
        return a == null ? b : b == null ? a : a.addAll(b);
    }

    default InferResult bind(PredicateImpl fromDecl, PredicateImpl to, PredicateImpl toDecl) {
        Set<PredicateImpl> facts = bind(facts(), fromDecl, to, toDecl);
        Set<PredicateImpl> falsehoods = bind(falsehoods(), fromDecl, to, toDecl);
        return of(facts, falsehoods, cycles());
    }

    static Set<PredicateImpl> bind(Set<PredicateImpl> set, PredicateImpl fromDecl, PredicateImpl to, PredicateImpl toDecl) {
        return set != null ? set.replaceAll(p -> toDecl.setBinding(to, fromDecl.getBinding(p, Map.of()))) : null;
    }

    default InferResult not() {
        return of(falsehoods(), facts(), cycles());
    }

    default boolean hasCycleWith(PredicateImpl predicate) {
        return cycles().contains(predicate);
    }

    default boolean hasStackOverflow() {
        return stackOverflow() != null;
    }

    default boolean isIncomplete() {
        return facts() == null && falsehoods() == null;
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
        return set != null ? set.toString().substring(3) : "?";
    }
}
