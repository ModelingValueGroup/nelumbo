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

package org.modelingvalue.nelumbo.syntax;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.mutable.MutableMap;
import org.modelingvalue.collections.util.NotMergeableException;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.lang.Functor;
import org.modelingvalue.nelumbo.lang.Type;
import org.modelingvalue.nelumbo.lang.Variable;

public class ParseContext {

    public static ParseContext of(String group, int precedence, MutableMap<String, ParseState> preStates,
            MutableMap<String, ParseState> postStates, MutableMap<String, Set<Variable>> hiddenVariables) {
        return new ParseContext(null, null, preStates, postStates, hiddenVariables, null, group, precedence) {
            @Override
            public String toString() {
                return "(UNIVERSE)";
            }
        };
    }

    public static ParseContext of(String group, int precedence, ParseContext outer) {
        MutableMap<String, ParseState> preStates = MutableMap.of(outer.preStates.get());
        MutableMap<String, ParseState> postStates = MutableMap.of(outer.postStates.get());
        MutableMap<String, Set<Variable>> hiddenVariables = MutableMap.of(outer.hiddenVariables.get());
        return new ParseContext(null, null, preStates, postStates, hiddenVariables, outer, group, precedence) {
            @Override
            public String toString() {
                return "(" + precedence + " " + group + " " + outer + ")";
            }
        };
    }

    public static ParseContext of(ParseState state, Token token, ParseContext outer) {
        MutableMap<String, ParseState> preStates = MutableMap.of(outer.preStates.get());
        MutableMap<String, ParseState> postStates = MutableMap.of(outer.postStates.get());
        MutableMap<String, Set<Variable>> hiddenVariables = MutableMap.of(outer.hiddenVariables.get());
        Integer precedence = state.innerPrecedence();
        return new ParseContext(token, state, preStates, postStates, hiddenVariables, outer, state.group(),
                precedence == null ? Integer.MIN_VALUE : precedence) {
            @Override
            public String toString() {
                return "(" + precedence + " " + state.group() + " " + outer + ")";
            }
        };
    }

    private final Token                                        token;
    private final ParseState                                   state;
    private final ParseContext                                 outer;
    private final String                                       group;
    private final int                                          precedence;
    private final MutableMap<String, ParseState>               preStates;
    private final MutableMap<String, ParseState>               postStates;
    private final MutableMap<String, Set<Variable>>            hiddenVariables;
    private final MutableMap<Type, Map<String, ParseState>>    localPreStates;
    private final MutableMap<Type, Map<String, ParseState>>    localPostStates;
    private final MutableMap<Type, Map<String, Set<Variable>>> localHiddenVariables;

    private ParseContext(Token token, ParseState state, MutableMap<String, ParseState> preStates,
            MutableMap<String, ParseState> postStates, MutableMap<String, Set<Variable>> hiddenVariables,
            ParseContext outer, String group, int precedence) {
        this.localPreStates = MutableMap.of(Map.of());
        this.localPostStates = MutableMap.of(Map.of());
        this.localHiddenVariables = MutableMap.of(Map.of());
        this.token = token;
        this.state = state;
        this.preStates = preStates;
        this.postStates = postStates;
        this.hiddenVariables = hiddenVariables;
        this.outer = outer;
        this.group = group;
        this.precedence = precedence;
    }

    public ParseState state() {
        return state;
    }

    public Token token() {
        return token;
    }

    public int precedence() {
        return precedence;
    }

    public String group() {
        return group;
    }

    public ParseContext outer() {
        return outer;
    }

    public void register(KnowledgeBase knowledgeBase, String group, Type scope, Functor functor) throws ParseException {
        try {
            ParseState preStart = functor.preStart();
            ParseState postStart = functor.postStart();
            if (preStart != null) {
                preStates.set(p -> merge(group, preStart, p));
                localPreStates.set(p -> merge(scope, group, preStart, p));
            }
            if (postStart != null) {
                postStates.set(p -> merge(group, postStart, p));
                localPostStates.set(p -> merge(scope, group, postStart, p));
            }
            Variable var = functor.variable();
            if (var != null && var.hidden()) {
                hiddenVariables.set(p -> p.add(group, Set.of(var)));
                localHiddenVariables.set(p -> merge(scope, group, var, p));
            }
        } catch (NotMergeableException pme) {
            knowledgeBase.addException(new ParseException(pme.getMessage(), functor));
        }
        knowledgeBase.register(functor);
    }

    private static Map<String, ParseState> merge(String group, ParseState state, Map<String, ParseState> m) {
        ParseState ts = m.get(group);
        return m.put(group, ts != null ? state.merge(ts) : state);
    }

    private static Map<Type, Map<String, ParseState>> merge(Type scope, String group, ParseState state,
            Map<Type, Map<String, ParseState>> m) {
        Map<String, ParseState> ts = m.get(scope);
        ts = ts != null ? ts.put(group, state.merge(ts.get(group))) : Map.of(Entry.of(group, state));
        return m.put(scope, ts);
    }

    private static Map<Type, Map<String, Set<Variable>>> merge(Type scope, String group, Variable var,
            Map<Type, Map<String, Set<Variable>>> m) {
        Map<String, Set<Variable>> ts = m.get(scope);
        ts = ts != null ? ts.add(group, Set.of(var)) : Map.of(Entry.of(group, Set.of(var)));
        return m.put(scope, ts);
    }

    public boolean preParse(String group, Token token, Node left, PatternResult result) throws ParseException {
        if (left != null) {
            ParseState state = postStates(group);
            if (state != null) {
                Type type = left instanceof Variable ? left.type().toVariable() : left.type();
                result.clear();
                ParseState found = state.matchType(type, false, true, result.typeArgs());
                if (found != null) {
                    result.left(left);
                    if (found.parse(token, result, Map.of(), true)) {
                        return true;
                    }
                }
            }
            return false;
        }
        ParseState state = preStates(group);
        if (state != null) {
            result.clear();
            if (state.parse(token, result, Map.of(), true)) {
                return true;
            }
        }
        return false;
    }

    public ParseState preStates(String group) {
        return preStates.get(group);
    }

    public ParseState postStates(String group) {
        return postStates.get(group);
    }

    public Set<Variable> hiddenVariables(String group) {
        return hiddenVariables.get(group);
    }

    public boolean hasDefined(Variable var) {
        String name = var.name();
        return (type(name) != null || variable(name) != null) && outer.type(name) == null
                && outer.variable(name) == null;
    }

    public Variable variable(String name) {
        for (Entry<String, ParseState> e : preStates.get()) {
            ParseState found = e.getValue().tokenTexts().get(name);
            if (found != null) {
                Functor functor = found.functor(Map.of());
                if (functor != null) {
                    return functor.constructedVariable();
                }
            }
        }
        return null;
    }

    public Type type(String name) {
        for (Entry<String, ParseState> e : preStates.get()) {
            ParseState found = e.getValue().tokenTexts().get(name);
            if (found != null) {
                Functor functor = found.functor(Map.of());
                if (functor != null) {
                    return functor.constructedType();
                }
            }
        }
        return null;
    }

    public void finish(Type scope) {
        if (localPreStates.get().anyMatch(e -> e.getKey().isAssignableFrom(scope))) {
            preStates.set(p -> outer.preStates.get());
            for (Entry<Type, Map<String, ParseState>> e : localPreStates.get()) {
                if (e.getKey().isAssignableFrom(scope)) {
                    localPreStates.remove(e.getKey());
                } else {
                    preStates.set(p -> p.addAll(e.getValue()));
                }
            }
        }
        if (localPostStates.get().anyMatch(e -> e.getKey().isAssignableFrom(scope))) {
            postStates.set(p -> outer.postStates.get());
            for (Entry<Type, Map<String, ParseState>> e : localPostStates.get()) {
                if (e.getKey().isAssignableFrom(scope)) {
                    localPostStates.remove(e.getKey());
                } else {
                    postStates.set(p -> p.addAll(e.getValue()));
                }
            }
        }
        if (localHiddenVariables.get().anyMatch(e -> e.getKey().isAssignableFrom(scope))) {
            hiddenVariables.set(p -> outer.hiddenVariables.get());
            for (Entry<Type, Map<String, Set<Variable>>> e : localHiddenVariables.get()) {
                if (e.getKey().isAssignableFrom(scope)) {
                    localHiddenVariables.remove(e.getKey());
                } else {
                    hiddenVariables.set(p -> p.addAll(e.getValue()));
                }
            }
        }
    }

    // The patterns of an imported knowledge base. This context is a copy of its
    // outer one, so it does not see what an import adds to the knowledge base.
    // They are world scoped here, so that a finish keeps them.
    public void include(ParseContext imported) {
        Map<String, ParseState> pre = imported.preStates.get();
        Map<String, ParseState> post = imported.postStates.get();
        Map<String, Set<Variable>> hidden = imported.hiddenVariables.get();
        preStates.set(p -> p.addAll(pre));
        postStates.set(p -> p.addAll(post));
        hiddenVariables.set(p -> p.addAll(hidden));
        localPreStates.set(p -> p.add(Type.WORLD, pre));
        localPostStates.set(p -> p.add(Type.WORLD, post));
        localHiddenVariables.set(p -> p.add(Type.WORLD, hidden));
    }

    public void put(ParseContext inner) {
        localPreStates.set(p -> p.addAll(inner.localPreStates.get()));
        localPostStates.set(p -> p.addAll(inner.localPostStates.get()));
        localHiddenVariables.set(p -> p.addAll(inner.localHiddenVariables.get()));
        preStates.set(p -> inner.preStates.get());
        postStates.set(p -> inner.postStates.get());
        hiddenVariables.set(p -> inner.hiddenVariables.get());
    }

}
