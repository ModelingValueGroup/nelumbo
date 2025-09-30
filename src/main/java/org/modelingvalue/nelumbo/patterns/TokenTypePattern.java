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
import org.modelingvalue.nelumbo.syntax.Patterns;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class TokenTypePattern extends Pattern {
    @Serial
    private static final long serialVersionUID = 2405616043878166113L;

    public TokenTypePattern(Type type, List<AstElement> elements, Object... args) {
        super(type, elements, args);
    }

    protected TokenTypePattern(Object[] args) {
        super(args);
    }

    @Override
    protected TokenTypePattern struct(Object[] array) {
        return new TokenTypePattern(array);
    }

    public TokenType tokenType() {
        return (TokenType) get(0);
    }

    @Override
    public Patterns patterns(Patterns nextPatterns, NodeTypePattern left) {
        return new Patterns(tokenType(), nextPatterns);
    }

    @Override
    public String toString() {
        return "t(" + tokenType() + ")";
    }

    @Override
    public int args(List<AstElement> elements, int i, Ref<List<Object>> args, boolean alt) {
        TokenType tokenType = tokenType();
        if (tokenType == TokenType.NEWLINE && i == elements.size()) {
            return i;
        }
        if (elements.get(i) instanceof Token token && tokenType.equals(token.type())) {
            if (alt || tokenType.variable()) {
                args.set(args.get().add(token.text()));
            }
            return i + 1;
        }
        return -1;
    }

    @Override
    public int string(List<Object> args, int i, Ref<String> string, boolean alt) {
        TokenType tokenType = tokenType();
        if (tokenType == TokenType.NEWLINE && i == args.size()) {
            return i;
        }
        if (alt || tokenType.variable()) {
            if (args.get(i) instanceof String text && tokenType.pattern().matcher(text).matches()) {
                string.set(string.get() + text);
                return i + 1;
            }
            return -1;
        }
        return i;
    }

}
