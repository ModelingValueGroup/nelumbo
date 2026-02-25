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
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.mutable.MutableList;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.Variable;
import org.modelingvalue.nelumbo.syntax.ParseState;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class TokenTextPattern extends Pattern {
    @Serial
    private static final long serialVersionUID = -7116490422223451839L;

    public TokenTextPattern(Type type, List<AstElement> elements, String text, Boolean isKeyword) {
        super(type, elements, text, isKeyword);
    }

    public TokenTextPattern(Type type, List<AstElement> elements, Variable var, Boolean isKeyword) {
        super(type, elements, var, isKeyword);
    }

    protected TokenTextPattern(Object[] args, TokenTextPattern declaration) {
        super(args, declaration);
    }

    @Override
    protected TokenTextPattern struct(Object[] array, Node declaration) {
        return new TokenTextPattern(array, (TokenTextPattern) declaration);
    }

    public String tokenText() {
        Object val = get(0);
        return val instanceof Variable var ? var.name() : (String) val;
    }

    @Override
    public Variable variable() {
        Object val = get(0);
        return val instanceof Variable var ? var : null;
    }

    public boolean isKeyword() {
        return (Boolean) get(1);
    }

    @Override
    public TokenTextPattern set(int i, Object... a) {
        return (TokenTextPattern) super.set(i, a);
    }

    @Override
    protected TokenTextPattern setBinding(Node declaration, Map<Variable, Object> vars) {
        Variable var = variable();
        if (var != null) {
            Object val = vars.get(var);
            if (val instanceof String text) {
                return set(0, text);
            }
            if (val instanceof Node node) {
                Token token = node.firstToken();
                return set(0, node.toString(), token.isKeyword() || token.isLitteralNode());
            }
        }
        return (TokenTextPattern) super.setBinding(declaration, vars);
    }

    @Override
    public ParseState state(ParseState next) {
        return new ParseState(tokenText(), isKeyword(), next);
    }

    @Override
    public String name() {
        return tokenText();
    }

    @Override
    public String toString(TokenType[] previous) {
        return tokenText();
    }

    @Override
    public List<Type> argTypes(List<Type> types) {
        return types;
    }

    @Override
    protected int string(List<Object> args, int ai, StringBuffer sb, TokenType[] previous, boolean alt) {
        if (alt) {
            if (ai < 0 || args.size() <= ai) {
                return -1;
            }
            if (args.get(ai) instanceof String text && text.equals(tokenText())) {
                addText(sb, previous, text);
                return ai + 1;
            }
            return -1;
        }
        addText(sb, previous, tokenText());
        return ai;
    }

    @Override
    protected int args(List<AstElement> elements, int i, MutableList<Object> args, boolean alt, Functor functor, Map<Variable, Type> typeArgs) {
        if (i < elements.size()) {
            AstElement e = elements.get(i);
            if (e instanceof Token t && t.text().equals(tokenText())) {
                if (alt) {
                    args.add(t.text());
                }
                return i + 1;
            }
        }
        return -1;
    }

    @Override
    public Pattern declaration(Token token) {
        return tokenText().equals(token.text()) ? this : null;
    }

}
