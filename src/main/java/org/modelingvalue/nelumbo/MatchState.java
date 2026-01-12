//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2026 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
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
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.util.Mergeable;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class MatchState<E> implements Mergeable<MatchState<E>> {

    @SuppressWarnings("rawtypes")
    public static final MatchState           EMPTY = new MatchState<>();

    private final Map<Object, MatchState<E>> transitions;
    private final Set<E>                     elements;

    private MatchState() {
        this.transitions = Map.of();
        this.elements = Set.of();
    }

    public MatchState(E element) {
        this.transitions = Map.of();
        this.elements = element != null ? Set.of(element) : Set.of();
    }

    public MatchState(Functor functor, MatchState<E> to) {
        this.transitions = Map.of(Entry.of(functor, to));
        this.elements = Set.of();
    }

    public MatchState(Type type, MatchState<E> to) {
        this.transitions = Map.of(Entry.of(type, to));
        this.elements = Set.of();
    }

    public MatchState(TokenType tokenType, MatchState<E> to) {
        this.transitions = Map.of(Entry.of(tokenType, to));
        this.elements = Set.of();
    }

    public MatchState(Class<?> clss, MatchState<E> to) {
        this.transitions = Map.of(Entry.of(clss, to));
        this.elements = Set.of();
    }

    private MatchState(Map<Object, MatchState<E>> transitions, Set<E> elements) {
        this.transitions = transitions;
        this.elements = elements;
    }

    public Map<Object, MatchState<E>> transitions() {
        return transitions;
    }

    public Set<E> elements() {
        return elements;
    }

    @Override
    public String toString() {
        return transitions().toKeys().asSet().toString().substring(3);
    }

    public MatchState<E> merge(MatchState<E> state) {
        if (state == null) {
            return this;
        }
        Map<Object, MatchState<E>> transitions = transitions().addAll(state.transitions(), MatchState::merge);
        for (Object key : transitions.toKeys()) {
            if (key instanceof Type subType) {
                for (Type superType : subType.allSupers()) {
                    if (!superType.equals(subType)) {
                        MatchState<E> superState = transitions.get(superType);
                        if (superState != null) {
                            MatchState<E> subState = transitions.get(subType);
                            MatchState<E> mergedState = subState.merge(superState);
                            transitions = transitions.put(subType, mergedState);
                        }
                    }
                }
            }
        }
        return new MatchState<>(transitions, elements().addAll(state.elements()));
    }

    public Set<E> match(Object obj) {
        MatchState<E> state = doMatch(obj);
        return state != null ? state.elements() : Set.of();
    }

    private MatchState<E> doMatch(Object obj) {
        MatchState<E> state;
        switch (obj) {
        case Type type -> state = matchType(type);
        case Variable var -> state = matchType(var.type());
        case Node node -> {
            Functor functor = node.functor();
            state = functor != null ? transitions().get(functor) : null;
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
        }
        case String text -> state = transitions().get(TokenType.of(text));
        default -> state = transitions().get(obj.getClass());
        }
        return state;
    }

    private MatchState<E> matchType(Type type) {
        MatchState<E> state;
        for (Type sup : type.allSupers()) {
            state = transitions().get(sup);
            if (state != null) {
                return state;
            }
        }
        state = transitions().get(Type.TYPE());
        return state;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public MatchState<E> merge(MatchState[] branches, int length) {
        MatchState<E> state = this;
        for (int i = 0; i < length; i++) {
            state = branches[i].merge(state);
        }
        return state;
    }

    @SuppressWarnings("unchecked")
    @Override
    public MatchState<E> getMerger() {
        return EMPTY;
    }

    @Override
    public Class<?> getMeetClass() {
        return MatchState.class;
    }

}
