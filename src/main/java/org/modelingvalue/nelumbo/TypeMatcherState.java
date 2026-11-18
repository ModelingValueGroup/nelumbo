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
//     Wim Bast, Tom Brus, Victor Lap                                                                                  ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.nelumbo;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.List;
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

    public TypeMatcherState(Map<Type, TypeMatcherState> transitions, Type type) {
        this.transitions = transitions;
        this.type = type;
    }

    public Type type() {
        return type;
    }

    // With coerce, a type variable in an argument of the type also matches an expected type. The variable is not bound:
    // that is done by matchAndCoerce, when the node is constructed.
    public final Set<Type> match(Type type, boolean inverse, boolean coerce, MutableMap<Variable, Type> typeArgs) {
        MutableMap<Variable, Type> coercions = coerce ? MutableMap.of(Map.of()) : null;
        return doMatch(type, inverse, null, coercions, typeArgs).replaceAll(TypeMatcherState::type);
    }

    // As match with coerce. When there is only one match, the type variables in the arguments of the type are bound in
    // the typeArgs to the expected types they matched.
    public final Set<Type> matchAndCoerce(Type type, boolean inverse, MutableMap<Variable, Type> typeArgs) {
        MutableMap<Variable, Type> coercions = MutableMap.of(Map.of());
        Set<TypeMatcherState> result = doMatch(type, inverse, null, coercions, typeArgs);
        if (result.size() == 1) {
            for (Entry<Variable, Type> c : coercions.get()) {
                if (!put(typeArgs, c.getKey(), c.getValue())) {
                    return Set.of();
                }
            }
        }
        return result.replaceAll(TypeMatcherState::type);
    }

    private Set<TypeMatcherState> doMatch(Type type, boolean inverse, MutableMap<Variable, Type> coercions,
            MutableMap<Variable, Type> argCoercions, MutableMap<Variable, Type> typeArgs) {
        Set<TypeMatcherState> result = Set.of();
        Entry<Type, TypeMatcherState> found = null;
        Type assigned = null;
        List<Entry<Type, TypeMatcherState>> coerced = List.of();
        outer: for (Entry<Type, TypeMatcherState> e : transitions) {
            Variable var = e.getKey().variable();
            if (var != null) {
                if (e.getKey().isMany()) {
                    for (Type m : e.getKey().many()) {
                        if (m.variable() == null && !m.isAssignableFrom(type)) {
                            if (var.name().contains("$")) {
                                typeArgs.put(var, Type.$NONE);
                            }
                            continue outer;
                        }
                    }
                }
                if (put(typeArgs, var, type.nonVariable())) {
                    result = result.add(e.getValue());
                }
            } else {
                Type a = e.getKey().getAssigned(type);
                if (a == null && inverse) {
                    // the type may be a super type of the expected type: its arguments become those of the expected type
                    Type numbered = e.getKey().numberArgs();
                    Map<Variable, Type> bound = numbered.coerceTo(type, Map.of());
                    if (bound != null) {
                        a = e.getKey();
                        if (!bound.isEmpty()) {
                            List<Type> args = a.arguments();
                            a = a.setArguments(numbered.arguments().replaceAll(n -> {
                                Type b = n.variable() != null ? bound.get(n.variable()) : null;
                                return b != null ? b : args.get(numbered.arguments().firstIndexOf(n));
                            }));
                        }
                    }
                }
                if (a == null && coercions != null && type.variable() != null) {
                    coerced = coerced.addAll(whole(e));
                } else if (a != null && (found == null || found.getKey().isAssignableFrom(e.getKey()))) {
                    found = e;
                    assigned = a;
                }
            }
        }
        if (found != null) {
            if (found.getKey().hasArguments()) {
                Set<TypeMatcherState> pre, post = Set.of(found.getValue());
                for (Type arg : assigned.arguments()) {
                    pre = post;
                    post = Set.of();
                    for (TypeMatcherState s : pre) {
                        post = post.addAll(s.doMatch(arg, inverse, argCoercions, argCoercions, typeArgs));
                    }
                }
                result = result.addAll(post);
            } else {
                result = result.add(found.getValue());
            }
        } else if (!coerced.isEmpty()) {
            if (coerced.size() == 1) {
                coercions.put(type.variable(), coerced.first().getKey());
            }
            result = result.addAll(coerced.map(Entry::getValue));
        }
        return result;
    }

    // The complete types (arguments included) that start with the given transition, each with the state after it.
    private static List<Entry<Type, TypeMatcherState>> whole(Entry<Type, TypeMatcherState> transition) {
        List<Entry<Type, TypeMatcherState>> wholes = List.of(transition);
        if (transition.getKey().hasArguments()) {
            for (int i = 0; i < transition.getKey().arguments().size(); i++) {
                int ai = i;
                wholes = wholes.flatMap(w -> w.getValue().transitions.flatMap(TypeMatcherState::whole).map(a -> Entry
                        .of(w.getKey().setArguments(w.getKey().arguments().replace(ai, a.getKey())), a.getValue())))
                        .asList();
            }
        }
        return wholes;
    }

    // Combines the type that is matched for a type variable with the type that was matched for it before.
    private static boolean put(MutableMap<Variable, Type> typeArgs, Variable var, Type type) {
        Type match = typeArgs.get(var);
        if (match == null) {
            typeArgs.put(var, type);
            return true;
        } else if (Type.$NONE.equals(match)) {
            return false;
        }
        Map<Variable, Type> pre = typeArgs.get();
        Map<Variable, Type> coerced = type.coerceTo(match, pre);
        Map<Variable, Type> post = coerced != null ? coerced : match.coerceTo(type, pre);
        if (post != null && post != pre) {
            typeArgs.set(m -> post.replaceAll(e -> Entry.of(e.getKey(), e.getValue().setTypeArgs(post))));
            match = typeArgs.get(var);
        }
        if (post != null && !type.typeVariables().isEmpty()) {
            type = type.setTypeArgs(post);
        }
        Type common = type.common(match);
        typeArgs.put(var, common != null ? common : Type.$NONE);
        return common != null;
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

    @Override
    public Map<Object, TypeMatcherState> allTransitions() {
        return Map.<Object, TypeMatcherState>of().putAll(transitions);
    }

}
