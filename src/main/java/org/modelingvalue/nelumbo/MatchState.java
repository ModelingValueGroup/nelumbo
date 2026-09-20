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
import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.mutable.MutableMap;
import org.modelingvalue.nelumbo.lang.Functor;
import org.modelingvalue.nelumbo.lang.Type;
import org.modelingvalue.nelumbo.lang.Variable;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class MatchState<E extends Node> extends AbstractState<MatchState<E>> {

    @SuppressWarnings("rawtypes")
    public static final MatchState EMPTY = new MatchState<>();

    private final Map<Object, MatchState<E>> transitions;
    private final Set<E>                     elements;

    private MatchState() {
        super(TypeMatcherState.EMPTY);
        this.transitions = Map.of();
        this.elements = Set.of();
    }

    public MatchState(E element) {
        super(TypeMatcherState.EMPTY);
        this.transitions = Map.of();
        this.elements = element != null ? Set.of(element) : Set.of();
    }

    public MatchState(Functor functor, MatchState<E> to) {
        super(TypeMatcherState.EMPTY);
        this.transitions = Map.of(Entry.of(functor.original(), to));
        this.elements = Set.of();
    }

    public MatchState(Type type, MatchState<E> to) {
        super(type.typeMatcher());
        this.transitions = Map.of(Entry.of(type, to));
        this.elements = Set.of();
    }

    public MatchState(TokenType tokenType, MatchState<E> to) {
        super(TypeMatcherState.EMPTY);
        this.transitions = Map.of(Entry.of(tokenType, to));
        this.elements = Set.of();
    }

    public MatchState(Class<?> clss, MatchState<E> to) {
        super(TypeMatcherState.EMPTY);
        this.transitions = Map.of(Entry.of(clss, to));
        this.elements = Set.of();
    }

    private MatchState(TypeMatcherState typeMatcher, Map<Object, MatchState<E>> transitions, Set<E> elements) {
        super(typeMatcher);
        this.transitions = transitions;
        this.elements = elements;
    }

    public Map<Object, MatchState<E>> transitions() {
        return transitions;
    }

    @Override
    protected Map<Object, MatchState<E>> typeTransitions() {
        return transitions;
    }

    public Set<E> elements() {
        return elements;
    }

    @Override
    public String toString() {
        return transitions().toKeys().asSet().toString().substring(3);
    }

    @Override
    public MatchState<E> merge(MatchState<E> merged) {
        if (merged == null) {
            return this;
        }
        TypeMatcherState typeMatcher = typeMatcher().merge(merged.typeMatcher());
        Map<Object, MatchState<E>> transitions = transitions().addAll(merged.transitions(), MatchState::merge);
        return new MatchState<>(typeMatcher, inherit(transitions), elements().addAll(merged.elements()));
    }

    @SuppressWarnings("unchecked")
    public Set<E> match(Object obj, KnowledgeBase knowledgeBase) {
        MutableMap<Variable, Type> typeArgs = MutableMap.of(Map.of());
        List<MatchState<E>> list = doMatch(obj, typeArgs);
        Set<E> elements = list.isEmpty() ? Set.of() : list.first().elements();
        Map<Variable, Type> tas = typeArgs.get();
        return tas.isEmpty() ? elements
                : elements.replaceAll(e -> knowledgeBase.actualize(e, tas, p -> (E) p.a().setTypeArgs(p.b())));
    }

    private List<MatchState<E>> doMatch(Object obj, MutableMap<Variable, Type> typeArgs) {
        List<MatchState<E>> states = List.of();
        switch (obj) {
        case Type type    -> {
            MatchState<E> state = matchType(type, typeArgs);
            if (state != null) {
                states = states.add(state);
            }
            break;
        }
        case Variable var -> {
            MatchState<E> state = matchType(var.type().toVariable(), typeArgs);
            if (state != null) {
                states = states.add(state);
            }
            break;
        }
        case Node node    -> {
            Functor functor = node.functor();
            MatchState<E> state = functor != null ? transitions().get(functor.original()) : null;
            if (state != null) {
                List<MatchState<E>> inners = List.of(state);
                for (Object arg : node.args()) {
                    for (MatchState<E> inner : inners) {
                        inners = inner.doMatch(arg, typeArgs);
                        if (!inners.isEmpty()) {
                            break;
                        }
                    }
                }
                if (!inners.isEmpty()) {
                    states = states.addAll(inners);
                }
            }
            state = matchType(node.type(), typeArgs);
            if (state != null) {
                states = states.add(state);
            }
            break;
        }
        case String text  -> {
            MatchState<E> state = transitions().get(TokenType.of(text));
            if (state != null) {
                states = states.add(state);
            }
            break;
        }
        default           -> {
            MatchState<E> state = transitions().get(obj.getClass());
            if (state != null) {
                states = states.add(state);
            }
            break;
        }
        }
        return states;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
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
