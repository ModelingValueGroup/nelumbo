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

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.QualifiedSet;
import org.modelingvalue.nelumbo.patterns.Functor;

public class MatchState {

    private static final QualifiedSet<Functor, Rule> EMPTY_RULES = QualifiedSet.of(Rule::consequenceFunctor);
    public static final MatchState                   EMPTY       = new MatchState();

    private final Map<Object, MatchState>            transitions;
    private final QualifiedSet<Functor, Rule>        rules;

    private MatchState() {
        this.transitions = Map.of();
        this.rules = EMPTY_RULES;
    }

    public MatchState(Rule rule) {
        this.transitions = Map.of();
        this.rules = rule != null ? EMPTY_RULES.add(rule) : EMPTY_RULES;
    }

    public MatchState(Functor functor, MatchState to) {
        this.transitions = Map.of(Entry.of(functor, to));
        this.rules = EMPTY_RULES;
    }

    public MatchState(Type type, MatchState to) {
        this.transitions = Map.of(Entry.of(type, to));
        this.rules = EMPTY_RULES;
    }

    public MatchState(Class<?> clss, MatchState to) {
        this.transitions = Map.of(Entry.of(clss, to));
        this.rules = EMPTY_RULES;
    }

    private MatchState(Map<Object, MatchState> transitions, QualifiedSet<Functor, Rule> rules) {
        this.transitions = transitions;
        this.rules = rules;
    }

    public Map<Object, MatchState> transitions() {
        return transitions;
    }

    public QualifiedSet<Functor, Rule> rules() {
        return rules;
    }

    @Override
    public String toString() {
        return transitions().toKeys().asSet().toString().substring(3);
    }

    public MatchState merge(MatchState state) {
        if (state == null) {
            return this;
        }
        Map<Object, MatchState> transitions = transitions().addAll(state.transitions(), (a, b) -> a.merge(b));
        for (Object key : transitions.toKeys()) {
            if (key instanceof Type subType) {
                for (Type superType : subType.allSupers()) {
                    if (!superType.equals(subType)) {
                        MatchState superState = transitions.get(superType);
                        if (superState != null) {
                            MatchState subState = transitions.get(subType);
                            MatchState mergedState = subState.merge(superState);
                            transitions = transitions.put(subType, mergedState);
                        }
                    }
                }
            }
        }
        return new MatchState(transitions, state.rules().putAll(rules()));
    }

    public QualifiedSet<Functor, Rule> match(Object obj) {
        MatchState state = doMatch(obj);
        return state != null ? state.rules() : EMPTY_RULES;
    }

    private MatchState doMatch(Object obj) {
        MatchState state;
        if (obj instanceof Type type) {
            state = matchType(type);
        } else if (obj instanceof Variable var) {
            state = matchType(var.type());
        } else if (obj instanceof Node node) {
            state = transitions().get(node.functor());
            if (state != null) {
                for (Object arg : node.args()) {
                    state = state.doMatch(arg);
                    if (state == null) {
                        break;
                    }
                }
            }
            if (state == null) {
                state = matchType(node.type());
            }
        } else {
            state = transitions().get(obj.getClass());
        }
        return state;
    }

    private MatchState matchType(Type type) {
        MatchState state = null;
        for (Type sup : type.allSupers()) {
            state = transitions().get(sup);
            if (state != null) {
                return state;
            }
        }
        return state;
    }

}
