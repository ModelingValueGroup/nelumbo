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

public class TypeMatcher {

    public static final TypeMatcher EMPTY = new TypeMatcher(Map.of(), null);

    private final Map<Type, TypeMatcher> transitions;
    private final Type                   type;

    private Map<Type, TypeMatcher> typeArgs = null;

    public TypeMatcher(Map<Type, TypeMatcher> transitions, Type type) {
        this.transitions = transitions;
        this.type = type;
    }

    public Type type() {
        return type;
    }

    private Map<Type, TypeMatcher> typeArgs() {
        if (typeArgs == null) {
            Map<Type, TypeMatcher> map = Map.of();
            for (Entry<Type, TypeMatcher> e : transitions) {
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

    public Set<Type> match(Type type, MutableMap<Variable, Type> typeArgs) {
        Set<Type> result = Set.of();
        for (Type sup : type.allSupersList()) {
            TypeMatcher state = transitions.get(sup);
            if (state != null) {
                result = sup.hasArgument() ? result.addAll(state.match(type.argument(), typeArgs))
                        : result.add(state.type);
                break;
            }
        }
        if (!type.hasArgument() && (result.isEmpty() || type.variable() == null)) {
            outer: for (Entry<Type, TypeMatcher> e : typeArgs()) {
                if (e.getKey().isMany()) {
                    for (Type m : e.getKey().many()) {
                        if (m.variable() == null && !m.isAssignableFrom(type)) {
                            continue outer;
                        }
                    }
                }
                type = type.nonVariable();
                Variable var = e.getKey().variable();
                Type found = typeArgs.get(var);
                if (found == null) {
                    typeArgs.put(var, type);
                    result = result.add(e.getValue().type);
                } else {
                    found = type.common(found);
                    if (found != null) {
                        typeArgs.put(var, found);
                        result = result.add(e.getValue().type);
                    } else {
                        typeArgs.put(var, Type.$NONE);
                    }
                }
            }
        }
        return result;
    }

    public TypeMatcher merge(TypeMatcher merged) {
        return new TypeMatcher(transitions.addAll(merged.transitions, TypeMatcher::merge),
                elementMerge(type, merged.type));
    }

    private static <T> T elementMerge(T t1, T t2) {
        if (t1 != null && t2 != null && !t1.equals(t2)) {
            throw new NotMergeableException("Non deterministic pattern merge " + t1 + " <> " + t2);
        }
        return t1 == null ? t2 : t1;
    }

    @Override
    public String toString() {
        return transitions.toKeys().asSet().toString().substring(3);
    }

}
