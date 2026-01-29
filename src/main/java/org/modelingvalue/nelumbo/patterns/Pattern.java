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

package org.modelingvalue.nelumbo.patterns;

import java.io.Serial;
import java.util.function.Function;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.mutable.MutableList;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.Variable;
import org.modelingvalue.nelumbo.syntax.ParseState;
import org.modelingvalue.nelumbo.syntax.Token;
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

    public static Pattern r(Pattern repeated, Boolean mandatory, Pattern separator) {
        return r(List.of(), repeated, mandatory, separator);
    }

    public static Pattern s(Pattern... elements) {
        return s(List.of(), elements);
    }

    public static Pattern t(String tokenText) {
        return t(List.of(), tokenText);
    }

    public static Pattern t(Variable var) {
        return t(List.of(), var);
    }

    public static Pattern t(TokenType tokenType) {
        return t(List.of(), tokenType);
    }

    public static Pattern v(Variable var) {
        return v(List.of(), var);
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

    public static Pattern r(List<AstElement> ast, Pattern repeated, Boolean mandatory, Pattern separator) {
        return new RepetitionPattern(Type.PATTERN, ast, repeated, mandatory, separator);
    }

    public static Pattern s(List<AstElement> ast, Pattern... elements) {
        return new SequencePattern(Type.PATTERN, ast, List.of(elements).replaceAllAll(e -> e instanceof SequencePattern s ? s.elements() : List.of(e)));
    }

    public static Pattern t(List<AstElement> ast, String tokenText) {
        return new TokenTextPattern(Type.PATTERN, ast, tokenText);
    }

    public static Pattern t(List<AstElement> ast, Variable var) {
        return new TokenTextPattern(Type.PATTERN, ast, var);
    }

    public static Pattern t(List<AstElement> ast, TokenType tokenType) {
        return new TokenTypePattern(Type.PATTERN, ast, tokenType);
    }

    public static Pattern v(List<AstElement> ast, Variable var) {
        Type type = var.type();
        TokenType tt = type.tokenType();
        return tt != null ? new TokenTextPattern(Type.PATTERN, ast, var) : //
                new NodeTypePattern(Type.PATTERN, ast, new Type(var), null);
    }

    protected Pattern(Type type, List<AstElement> ast, Object... args) {
        super(type, ast, args);
    }

    protected Pattern(Object[] args, Pattern declaration) {
        super(args, declaration);
    }

    @Override
    protected abstract Pattern struct(Object[] array, Node declaration);

    public abstract ParseState state(ParseState next, Functor functor);

    public String name() {
        return "";
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

    public abstract List<Type> argTypes(List<Type> types);

    protected abstract int string(List<Object> args, int ai, StringBuffer sb, TokenType[] previous, boolean alt);

    protected abstract int args(List<AstElement> elements, int i, MutableList<Object> args, boolean alt, Functor functor);

    public static boolean isEndOfLine(Token token) {
        return token.type() == TokenType.ENDOFFILE || (token.previous() != null && token.line() > token.previous().line());
    }

    protected void addText(StringBuffer sb, TokenType[] previous, String text) {
        TokenType type = TokenType.of(text);
        if (previous[0] == TokenType.NAME || previous[0] == TokenType.NUMBER || previous[0] == TokenType.DECIMAL) {
            if (type == TokenType.NAME || type == TokenType.NUMBER || type == TokenType.DECIMAL) {
                sb.append(" ");
            }
        }
        sb.append(text);
        previous[0] = type;
    }

    public abstract Pattern declaration(Token token);

}
