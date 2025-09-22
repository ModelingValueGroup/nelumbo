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
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParseResult;
import org.modelingvalue.nelumbo.syntax.Parser;
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
    public Token parse(Token token, String group, Parser parser, Pattern next, ParseResult result) throws ParseException {
        if (!result.isDone()) {
            result.add(token);
            token = token.next();
        }
        return token;
    }

    @Override
    public boolean peekIs(Token token, Parser parser) throws ParseException {
        return token.text().equals(tokenText());
    }

    @Override
    public Patterns patterns(Patterns patterns) {
        return Patterns.EMPTY.put(tokenText(), patterns);
    }

    @Override
    public List<Pattern> fixed(List<Pattern> fixed, boolean[] stop) {
        return fixed.add(this);
    }

    @Override
    public boolean isFixed() {
        return true;
    }

    @Override
    public String name() {
        return tokenText();
    }

    @Override
    public String toString() {
        return "t(\"" + tokenText() + "\")";
    }

}
