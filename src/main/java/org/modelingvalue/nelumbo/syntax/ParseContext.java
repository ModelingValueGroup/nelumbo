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

package org.modelingvalue.nelumbo.syntax;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.mutable.MutableMap;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.Variable;
import org.modelingvalue.nelumbo.patterns.Functor;

public interface ParseContext {

    ParseState state();

    Token token();

    int precedence();

    String group();

    ParseContext outer();

    MutableMap<String, Map<Type, ParseState>> preStates();

    MutableMap<String, Map<Type, ParseState>> postStates();

    static ParseContext of(ParseState state, Token token, MutableMap<String, Map<Type, ParseState>> preStates, MutableMap<String, Map<Type, ParseState>> postStates, ParseContext outer) {
        return new ParseContext() {

            @Override
            public ParseState state() {
                return state;
            }

            @Override
            public Token token() {
                return token;
            }

            @Override
            public int precedence() {
                Integer inner = state().innerPrecedence();
                return inner == null ? Integer.MIN_VALUE : inner;
            }

            @Override
            public String group() {
                return state().group();
            }

            @Override
            public MutableMap<String, Map<Type, ParseState>> preStates() {
                return preStates;
            }

            @Override
            public MutableMap<String, Map<Type, ParseState>> postStates() {
                return postStates;
            }

            @Override
            public ParseContext outer() {
                return outer;
            }

            @Override
            public String toString() {
                return "(" + precedence() + " " + group() + " " + outer() + ")";
            }

        };
    }

    static ParseContext of(String group, int precedence, MutableMap<String, Map<Type, ParseState>> preStates, MutableMap<String, Map<Type, ParseState>> postStates, ParseContext outer) {
        return new ParseContext() {

            @Override
            public ParseState state() {
                return null;
            }

            @Override
            public Token token() {
                return null;
            }

            @Override
            public int precedence() {
                return precedence;
            }

            @Override
            public String group() {
                return group;
            }

            @Override
            public MutableMap<String, Map<Type, ParseState>> preStates() {
                return preStates;
            }

            @Override
            public MutableMap<String, Map<Type, ParseState>> postStates() {
                return postStates;
            }

            @Override
            public ParseContext outer() {
                return outer;
            }

            @Override
            public String toString() {
                return "(" + precedence() + " " + group() + " " + outer() + ")";
            }

        };
    }

    default boolean preParse(String group, Token token, Node left, PatternResult result) throws ParseException {
        Map<Type, ParseState> states = (left != null ? postStates() : preStates()).get().get(group);
        if (states == null) {
            return false;
        }
        if (left != null) {
            for (ParseState state : states.toValues()) {
                for (Type sup : left.type().allSupers()) {
                    ParseState found = state.nodeTypes().get(sup);
                    if (found != null) {
                        result.clear();
                        result.left(left);
                        return found.parse(token, result, Map.of(), true);
                    }
                }
            }
            return false;
        }
        for (ParseState state : states.toValues()) {
            result.clear();
            if (state.parse(token, result, Map.of(), true)) {
                return true;
            }
        }
        return false;
    }

    default Functor register(KnowledgeBase knowledgeBase, String group, Type type, Functor functor) throws ParseException {
        try {
            ParseState preStart = functor.preStart();
            ParseState postStart = functor.postStart();
            if (preStart != null) {
                preStates().set(p -> merge(group, type, preStart, p));
            }
            if (postStart != null) {
                postStates().set(p -> merge(group, type, postStart, p));
            }
        } catch (PatternMergeException pme) {
            knowledgeBase.addException(new ParseException(pme.getMessage(), functor));
        }
        knowledgeBase.register(functor);
        return functor;
    }

    default Map<String, Map<Type, ParseState>> merge(String group, Type type, ParseState state, Map<String, Map<Type, ParseState>> m) {
        Map<Type, ParseState> ts = m.get(group);
        ts = ts != null ? ts.put(type, state.merge(ts.get(type))) : Map.of(Entry.of(type, state));
        return m.put(group, ts);
    }

    default Map<Type, ParseState> groupStates(String group) {
        return preStates().get(group);
    }

    default Variable variable(String group, Token token, Parser parser) throws ParseException {
        Map<Type, ParseState> states = groupStates(group);
        if (states != null) {
            for (ParseState state : states.toValues()) {
                ParseState found = state.tokenTexts().get(token.text());
                if (found != null && found.functor() != null && found.functor().resultType() == Type.VARIABLE) {
                    return (Variable) found.functor().construct(List.of(token), new Object[0], parser, this);
                }
            }
        }
        return null;
    }

    default void finish(Type type) {
        for (Entry<String, Map<Type, ParseState>> pre : preStates().get()) {
            for (Entry<Type, ParseState> states : pre.getValue()) {
                if (states.getKey().isAssignableFrom(type)) {
                    Map<Type, ParseState> m = preStates().get(pre.getKey());
                    if (m.size() == 1) {
                        preStates().remove(pre.getKey());
                    } else {
                        preStates().put(pre.getKey(), m.removeKey(states.getKey()));
                    }
                }
            }
        }
        for (Entry<String, Map<Type, ParseState>> post : preStates().get()) {
            for (Entry<Type, ParseState> states : post.getValue()) {
                if (states.getKey().isAssignableFrom(type)) {
                    Map<Type, ParseState> m = preStates().get(post.getKey());
                    if (m.size() == 1) {
                        preStates().remove(post.getKey());
                    } else {
                        preStates().put(post.getKey(), m.removeKey(states.getKey()));
                    }
                }
            }
        }
    }

}
