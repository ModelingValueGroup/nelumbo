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
    public Patterns patterns(Patterns nextPatterns, NodeTypePattern left) {
        return new Patterns(tokenText(), nextPatterns);
    }

    @Override
    public String name() {
        return tokenText();
    }

    @Override
    public String toString() {
        return "t(\"" + tokenText() + "\")";
    }

    @Override
    public int args(List<AstElement> elements, int i, Ref<List<Object>> args, boolean alt) {
        String tokenText = tokenText();
        if (elements.get(i) instanceof Token token && tokenText.equals(token.text())) {
            if (alt) {
                args.set(args.get().add(tokenText));
            }
            return i + 1;
        }
        return -1;
    }

    @Override
    public int string(List<Object> args, int i, Ref<String> string, boolean alt) {
        String tokenText = tokenText();
        if (alt) {
            if (args.get(i) instanceof String text && tokenText.equals(text)) {
                i++;
            } else {
                return -1;
            }
        }
        string.set(string.get() + tokenText);
        return i;
    }

}
