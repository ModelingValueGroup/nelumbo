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

import java.lang.reflect.Constructor;
import java.util.function.Consumer;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.mutable.MutableMap;
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

    MutableMap<String, ParseState> preStates();

    MutableMap<String, ParseState> postStates();

    static ParseContext of(ParseState state, Token token, MutableMap<String, ParseState> preStates, ParseContext outer) {
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
            public MutableMap<String, ParseState> preStates() {
                return preStates;
            }

            @Override
            public MutableMap<String, ParseState> postStates() {
                return null;
            }

            @Override
            public ParseContext outer() {
                return outer;
            }

            @Override
            public String toString() {
                return "(" + state() + " " + precedence() + " " + group() + " " + preStates() + " " + outer() + ")";
            }

        };
    }

    static ParseContext of(String group, int precedence, MutableMap<String, ParseState> preStates, MutableMap<String, ParseState> postStates, ParseContext outer) {
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
            public MutableMap<String, ParseState> preStates() {
                return preStates;
            }

            @Override
            public MutableMap<String, ParseState> postStates() {
                return postStates;
            }

            @Override
            public ParseContext outer() {
                return outer;
            }

            @Override
            public String toString() {
                return "(" + precedence() + " " + group() + " " + preStates() + " " + postStates() + " " + outer() + ")";
            }

        };
    }

    default Functor register(Functor functor) throws ParseException {
        Type type = functor.resultType();
        String group = Type.VARIABLE.isAssignableFrom(type) ? //
                functor.construct(List.of(), new Object[0], this).type().group() : //
                type.group();
        boolean local = functor.local();
        try {
            ParseState pre = functor.preStart();
            ParseState post = functor.postStart();
            if (!local) {
                if (pre != null) {
                    prePatterns.set(p -> p.put(group, pre.merge(p.get(group))));
                }
                if (post != null) {
                    postPatterns.set(p -> p.put(group, post.merge(p.get(group))));
                }
            }
            if (pre != null) {
                localPrePatterns.set(p -> p.put(group, pre.merge(p.get(group))));
            }
            if (post != null) {
                localPostPatterns.set(p -> p.put(group, post.merge(p.get(group))));
            }
        } catch (PatternMergeException pme) {
            addException(new ParseException(pme.getMessage(), functor));
        }
        Constructor<? extends Node> constructor = functor.constructor();
        if (constructor != null && !FUNCTOR_REGISTRATION.get().isEmpty()) {
            Class<? extends Node> cls = constructor.getDeclaringClass();
            Consumer<Functor> setter = FUNCTOR_REGISTRATION.get().get(cls);
            if (setter != null) {
                setter.accept(functor);
                FUNCTOR_REGISTRATION.updateAndGet(map -> map.remove(cls));
            }
        }
        if (!local) {
            functors.accumulateAndGet(Set.of(functor), Set::addAll);
        }
        return functor;
    }

    default ParseState groupState(String group) {
        return preStates().get(group);
    }

    default Variable variable(Token token, Parser parser) throws ParseException {
        ParseState state = groupState(group());
        ParseState found = state != null ? state.tokenTexts().get(token.text()) : null;
        if (found != null && found.functor() != null && found.functor().resultType() == Type.VARIABLE) {
            return (Variable) found.functor().construct(List.of(token), new Object[0], parser, this);
        }
        return null;
    }

    default PatternResult preParse(Token token, Node left, Parser parser) throws ParseException {
        ParseState state = (left != null ? postStates() : preStates()).get().get(this.group());
        return state != null ? preParse(token, left, parser, state) : null;
    }

    default PatternResult preParse(Token token, Node left, Parser parser, ParseState state) throws ParseException {
        if (left != null) {
            for (Type sup : left.type().allSupers()) {
                ParseState found = state.nodeTypes().get(sup);
                if (found != null) {
                    PatternResult result = new PatternResult(parser, this);
                    result.left(left);
                    return found.parse(token, result, Map.of(), true) ? result : null;
                }
            }
            return null;
        }
        PatternResult result = new PatternResult(parser, this);
        return state.parse(token, result, Map.of(), true) ? result : null;
    }

}
