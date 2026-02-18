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

    MutableMap<String, ParseState> preStates();

    MutableMap<String, ParseState> postStates();

    static ParseContext of(ParseState state, Token token, MutableMap<String, ParseState> preStates, MutableMap<String, ParseState> postStates, ParseContext outer) {
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
                return "(" + precedence() + " " + group() + " " + outer() + ")";
            }

        };
    }

    default boolean preParse(String group, Token token, Node left, PatternResult result) throws ParseException {
        ParseState state = (left != null ? postStates() : preStates()).get().get(group);
        if (state == null) {
            return false;
        }
        if (left != null) {
            for (Type sup : left.type().allSupers()) {
                ParseState found = state.nodeTypes().get(sup);
                if (found != null) {
                    result.clear();
                    result.left(left);
                    return found.parse(token, result, Map.of(), true);
                }
            }
            return false;
        }
        result.clear();
        return state.parse(token, result, Map.of(), true);
    }

    default Functor register(KnowledgeBase knowledgeBase, String group, Functor functor) throws ParseException {
        try {
            ParseState pre = functor.preStart();
            ParseState post = functor.postStart();
            if (pre != null) {
                preStates().set(p -> p.put(group, pre.merge(p.get(group))));
            }
            if (post != null) {
                postStates().set(p -> p.put(group, post.merge(p.get(group))));
            }
        } catch (PatternMergeException pme) {
            knowledgeBase.addException(new ParseException(pme.getMessage(), functor));
        }
        knowledgeBase.register(functor);
        return functor;
    }

    default ParseState groupState(String group) {
        return preStates().get(group);
    }

    default Variable variable(String group, Token token, Parser parser) throws ParseException {
        ParseState state = groupState(group);
        ParseState found = state != null ? state.tokenTexts().get(token.text()) : null;
        if (found != null && found.functor() != null && found.functor().resultType() == Type.VARIABLE) {
            return (Variable) found.functor().construct(List.of(token), new Object[0], parser, this);
        }
        return null;
    }

}
