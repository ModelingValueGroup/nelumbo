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

package org.modelingvalue.nelumbo.patterns;

import java.io.Serial;
import java.util.Iterator;
import java.util.function.Function;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.syntax.ParseState;
import org.modelingvalue.nelumbo.syntax.TokenType;

public abstract class Pattern extends Node {
    @Serial
    private static final long serialVersionUID = -1788203180486332564L;

    public static Pattern a(Pattern... options) {
        return a(List.of(), options);
    }

    public static Pattern n(Type nodeType, Integer precedence) {
        return n(List.of(), nodeType, precedence);
    }

    public static Pattern o(Pattern optional) {
        return o(List.of(), optional);
    }

    public static Pattern r(Pattern repeated) {
        return r(List.of(), repeated);
    }

    public static Pattern s(Pattern... elements) {
        return s(List.of(), elements);
    }

    public static Pattern t(String tokenText) {
        return t(List.of(), tokenText);
    }

    public static Pattern t(TokenType tokenType) {
        return t(List.of(), tokenType);
    }

    public static Pattern a(List<AstElement> ast, Pattern... options) {
        return new AlternationPattern(Type.PATTERN, ast, List.of(options));
    }

    public static Pattern n(List<AstElement> ast, Type nodeType, Integer precedence) {
        return new NodeTypePattern(Type.PATTERN, ast, nodeType, precedence);
    }

    public static Pattern o(List<AstElement> ast, Pattern optional) {
        return new OptionalPattern(Type.PATTERN, ast, optional);
    }

    public static Pattern r(List<AstElement> ast, Pattern repeated) {
        return new RepetitionPattern(Type.PATTERN, ast, repeated);
    }

    public static Pattern s(List<AstElement> ast, Pattern... elements) {
        return new SequencePattern(Type.PATTERN, ast, List.of(elements).replaceAllAll(e -> {
            return e instanceof SequencePattern s ? s.elements() : List.of(e);
        }));
    }

    public static Pattern t(List<AstElement> ast, String tokenText) {
        return new TokenTextPattern(Type.PATTERN, ast, tokenText);
    }

    public static Pattern t(List<AstElement> ast, TokenType tokenType) {
        return new TokenTypePattern(Type.PATTERN, ast, tokenType);
    }

    protected Pattern(Type type, List<AstElement> ast, Object... args) {
        super(type, ast, args);
    }

    protected Pattern(Object[] args) {
        super(args);
    }

    @Override
    protected abstract Pattern struct(Object[] array);

    public abstract ParseState state(ParseState next, NodeTypePattern left, List<Integer> branche);

    public String name() {
        return "";
    }

    public List<Type> argTypes() {
        return List.of();
    }

    public Pattern setPresedence(List<Integer> precedence, int[] p) {
        return this;
    }

    public Pattern setTypes(Function<Type, Type> typeFunction) {
        return this;
    }

    @Override
    public Pattern set(int i, Object... a) {
        return (Pattern) super.set(i, a);
    }

    protected abstract List<Object> args(List<Object> args, ElementIterator it, List<Integer> branche, boolean alt);

    protected static final class ElementIterator {

        private final Iterator<AstElement> it;

        private List<ParseState>           states;
        private int                        stateIndex;

        protected AstElement               element;
        protected List<Integer>            branche;

        protected ElementIterator(List<AstElement> elements, ParseState start) {
            it = elements.iterator();
            states = List.of(start);
            next();
        }

        protected void next() {
            if (it.hasNext()) {
                element = it.next();
                stateIndex -= element.getCycleDepth();
                ParseState state = states.get(stateIndex);
                Object input = element.getInput();
                branche = state.branches().get(input);
                state = state.transitions().get(input);
                states = states.size() > ++stateIndex ? states.replace(stateIndex, state) : states.add(state);
            } else {
                element = null;
                stateIndex = 0;
                branche = List.of();
            }
        }

        protected boolean match(List<Integer> branche) {
            if (this.branche.size() <= branche.size()) {
                return false;
            }
            for (int i = 0; i < branche.size(); i++) {
                if (!this.branche.get(i).equals(branche.get(i))) {
                    return false;
                }
            }
            return true;
        }

    }

}
