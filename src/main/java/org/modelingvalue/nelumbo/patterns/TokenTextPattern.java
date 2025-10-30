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

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.syntax.ParseState;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class TokenTextPattern extends Pattern {
    @Serial
    private static final long serialVersionUID = -7116490422223451839L;

    public TokenTextPattern(Type type, List<AstElement> elements, Object... args) {
        super(type, elements, args);
    }

    protected TokenTextPattern(Object[] args) {
        super(args);
    }

    @Override
    protected TokenTextPattern struct(Object[] array) {
        return new TokenTextPattern(array);
    }

    public String tokenText() {
        return (String) get(0);
    }

    @Override
    public ParseState state(ParseState next, NodeTypePattern left, Functor functor, List<Integer> branche) {
        return new ParseState(tokenText(), next.merge(new ParseState(functor, branche)));
    }

    @Override
    public String name() {
        return tokenText();
    }

    @Override
    public String toString() {
        return tokenText();
    }

    @Override
    public List<Type> argTypes(List<Type> types) {
        return types;
    }

    @Override
    protected List<Object> args(List<Object> args, ElementIterator it, List<Integer> branche, boolean alt) {
        if (alt) {
            args = args.add(((Token) it.element).text());
        }
        it.next();
        return args;
    }

    @Override
    protected int string(List<Object> args, int ai, StringBuffer sb, TokenType[] previous, boolean alt) {
        if (alt) {
            if (args.get(ai) instanceof String text && text.equals(tokenText())) {
                addText(sb, previous, text);
                return ai + 1;
            }
            return -1;
        }
        addText(sb, previous, tokenText());
        return ai;
    }

}
