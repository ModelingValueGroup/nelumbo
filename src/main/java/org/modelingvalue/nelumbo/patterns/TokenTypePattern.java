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

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.mutable.MutableList;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.Variable;
import org.modelingvalue.nelumbo.syntax.ParseState;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class TokenTypePattern extends Pattern {
    @Serial
    private static final long serialVersionUID = 2405616043878166113L;

    public TokenTypePattern(Type type, List<AstElement> elements, Object... args) {
        super(type, elements, args);
    }

    protected TokenTypePattern(Object[] args, TokenTypePattern declaration) {
        super(args, declaration);
    }

    @Override
    protected TokenTypePattern struct(Object[] array, Node declaration) {
        return new TokenTypePattern(array, (TokenTypePattern) declaration);
    }

    public TokenType tokenType() {
        return (TokenType) get(0);
    }

    @Override
    public String toString(TokenType[] previous) {
        return "<" + tokenType() + ">";
    }

    @Override
    public ParseState state(ParseState next, Functor functor) {
        return new ParseState(tokenType(), next);
    }

    @Override
    public List<Type> argTypes(List<Type> types) {
        TokenType type = tokenType();
        return !isEmpty(type) ? types.add(Type.$STRING) : types;
    }

    @Override
    protected int string(List<Object> args, int ai, StringBuffer sb, TokenType[] previous, boolean alt) {
        TokenType type = tokenType();
        if (!isEmpty(type)) {
            Object val = args.get(ai);
            if (val instanceof String text && type.matches(text)) {
                addText(sb, previous, text);
                return ai + 1;
            } else {
                String text = val != null ? val.toString() : null;
                if (text != null && type.matches(text)) {
                    addText(sb, previous, text);
                    return ai + 1;
                } else {
                    return -1;
                }
            }
        }
        return ai;
    }

    @Override
    protected int args(List<AstElement> elements, int i, MutableList<Object> args, boolean alt, Functor functor) {
        if (i < elements.size()) {
            AstElement e = elements.get(i);
            TokenType type = tokenType();
            if (e instanceof Token t) {
                if (t.isKeyword() && type.isVariableContent()) {
                    return -1;
                } else if (t.type().equals(type)) {
                    if (!isEmpty(type)) {
                        args.add(t.text());
                    }
                    return i + 1;
                } else if (TokenType.NEWLINE.equals(type) && Pattern.isEndOfLine(t)) {
                    return i;
                }
            } else if (e instanceof Variable v && type.equals(v.type().tokenType())) {
                args.add(v);
                return i + 1;
            }
        }
        return -1;
    }

    private static boolean isEmpty(TokenType type) {
        return type == TokenType.NEWLINE || type == TokenType.BEGINOFFILE || type == TokenType.ENDOFFILE || type == TokenType.ENDOFLINE;
    }

    @Override
    public Pattern declaration(Token token) {
        return null;
    }

}
