//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//  (C) Copyright 2018-2025 Modeling Value Group B.V. (http://modelingvalue.org)                                         ~
//                                                                                                                       ~
//  Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in       ~
//  compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0   ~
//  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on  ~
//  an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the   ~
//  specific language governing permissions and limitations under the License.                                           ~
//                                                                                                                       ~
//  Maintainers:                                                                                                         ~
//      Wim Bast, Tom Brus                                                                                               ~
//                                                                                                                       ~
//  Contributors:                                                                                                        ~
//      Ronald Krijgsheld ✝, Arjan Kok, Carel Bast                                                                       ~
// --------------------------------------------------------------------------------------------------------------------- ~
//  In Memory of Ronald Krijgsheld, 1972 - 2023                                                                          ~
//      Ronald was suddenly and unexpectedly taken from us. He was not only our long-term colleague and team member      ~
//      but also our friend. "He will live on in many of the lines of code you see below."                               ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.nelumbo.patterns;

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParseResult;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Token;

public class AlternationPattern extends Pattern {
    @Serial
    private static final long serialVersionUID = -2652813935675033086L;

    public AlternationPattern(Type type, List<AstElement> elements, Object... args) {
        super(type, elements, args);
    }

    protected AlternationPattern(Object[] args) {
        super(args);
    }

    @Override
    protected AlternationPattern struct(Object[] array) {
        return new AlternationPattern(array);
    }

    @SuppressWarnings("unchecked")
    public List<Pattern> options() {
        return (List<Pattern>) get(0);
    }

    @Override
    public Token parse(Token token, String group, int precedence, Parser parser, Pattern next, ParseResult result) throws ParseException {
        for (Pattern option : options()) {
            if (option.peekIs(token, parser)) {
                return option.parse(token, group, precedence, parser, next, result);
            }
        }
        throw new ParseException("Expected " + this + " but found " + token.text() + " of type " + token.type(), token);
    }

    @Override
    public boolean peekIs(Token token, Parser parser) throws ParseException {
        List<Pattern> options = options();
        for (Pattern option : options) {
            if (option.peekIs(token, parser)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isFixed() {
        return false;
    }

    @Override
    public String toString() {
        String string = options().toString();
        return "a(" + string.substring(5, string.length() - 1) + ")";
    }

}
