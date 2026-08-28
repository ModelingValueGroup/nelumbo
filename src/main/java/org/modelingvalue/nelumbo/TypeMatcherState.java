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
import org.modelingvalue.collections.mutable.MutableMap;
import org.modelingvalue.collections.util.NotMergeableException;
import org.modelingvalue.nelumbo.lang.Type;
import org.modelingvalue.nelumbo.lang.Variable;

public class TypeMatcherState implements IState<TypeMatcherState> {

    public static final TypeMatcherState EMPTY = new TypeMatcherState(Map.of(), null);

    private final Map<Type, TypeMatcherState> transitions;
    private final Type                        type;

    private Map<Type, TypeMatcherState> typeArgs = null;

    public TypeMatcherState(Map<Type, TypeMatcherState> transitions, Type type) {
        this.transitions = transitions;
        this.type = type;
    }

    public Type type() {
        return type;
    }

    private Map<Type, TypeMatcherState> typeArgs() {
        if (typeArgs == null) {
            Map<Type, TypeMatcherState> map = Map.of();
            for (Entry<Type, TypeMatcherState> e : transitions) {
                if (e.getKey() instanceof Type t) {
                    if (t.variable() != null) {
                        map = map.put(t, e.getValue());
                    }
                }
            }
            typeArgs = map;
        }
        return typeArgs;
    }

    public final Set<Type> match(Type type, MutableMap<Variable, Type> typeArgs) {
        return doMatch(type, typeArgs).replaceAll(TypeMatcherState::type);
    }

    private Set<TypeMatcherState> doMatch(Type type, MutableMap<Variable, Type> typeArgs) {
        Set<TypeMatcherState> result = Set.of();
        for (Entry<Type, Type> entry : type.allSupersList()) {
            Type sup = entry.getKey();
            TypeMatcherState state = transitions.get(sup);
            if (state != null) {
                if (sup.hasArguments()) {
                    Set<TypeMatcherState> pre, post = Set.of(state);
                    for (Type arg : entry.getValue().arguments()) {
                        pre = post;
                        post = Set.of();
                        for (TypeMatcherState s : pre) {
                            post = post.addAll(s.doMatch(arg, typeArgs));
                        }
                    }
                    result = result.addAll(post);
                } else {
                    result = result.add(state);
                }
                break;
            }
        }
        if (result.isEmpty()) {
            outer: for (Entry<Type, TypeMatcherState> e : typeArgs()) {
                if (e.getKey().isMany()) {
                    for (Type m : e.getKey().many()) {
                        if (m.variable() == null && !m.isAssignableFrom(type)) {
                            continue outer;
                        }
                    }
                }
                Type nvt = type.nonVariable();
                Variable var = e.getKey().variable();
                Type found = typeArgs.get(var);
                if (found == null) {
                    typeArgs.put(var, nvt);
                    result = result.add(e.getValue());
                } else {
                    found = nvt.common(found);
                    if (found != null) {
                        typeArgs.put(var, found);
                        result = result.add(e.getValue());
                    } else {
                        typeArgs.put(var, Type.$NONE);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public TypeMatcherState merge(TypeMatcherState merged) {
        return new TypeMatcherState(inherit(transitions.addAll(merged.transitions, TypeMatcherState::merge)),
                elementMerge(type, merged.type));
    }

    private static Type elementMerge(Type t1, Type t2) {
        if (t1 != null && t2 != null && !t1.equals(t2)) {
            if (t1.isAssignableFrom(t2)) {
                return t2;
            }
            if (t2.isAssignableFrom(t1)) {
                return t1;
            }
            throw new NotMergeableException("Non deterministic pattern merge " + t1 + " <> " + t2);
        }
        return t1 == null ? t2 : t1;
    }

    @Override
    public String toString() {
        return transitions.toKeys().asSet().toString().substring(3);
    }

    @Override
    public TypeMatcherState merge(TypeMatcherState[] branches, int length) {
        TypeMatcherState state = this;
        for (int i = 0; i < length; i++) {
            state = branches[i].merge(state);
        }
        return state;
    }

    @Override
    public TypeMatcherState getMerger() {
        return EMPTY;
    }

    @Override
    public Class<?> getMeetClass() {
        return TypeMatcherState.class;
    }

}
