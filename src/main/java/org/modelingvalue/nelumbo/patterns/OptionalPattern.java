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
import org.modelingvalue.nelumbo.syntax.Token;

public class OptionalPattern extends Pattern {
    @Serial
    private static final long serialVersionUID = 3011113311569598643L;

    public OptionalPattern(Type type, List<AstElement> elements, Object... args) {
        super(type, elements, args);
    }

    protected OptionalPattern(Object[] args) {
        super(args);
    }

    @Override
    protected OptionalPattern struct(Object[] array) {
        return new OptionalPattern(array);
    }

    public Pattern optional() {
        return (Pattern) get(0);
    }

    @Override
    public Token parse(Token token, String group, Parser parser, Pattern next, ParseResult result) throws ParseException {
        Pattern optional = optional();
        if (optional.peekIs(token, parser) || (next != null && !next.peekIs(token, parser))) {
            token = optional.parse(token, group, parser, next, result);
        }
        return token;
    }

    @Override
    public List<Type> args() {
        return optional().args();
    }

    @Override
    public String toString() {
        return "o(" + optional() + ")";
    }

    @Override
    public Pattern setPresedence(List<Integer> precedence, int[] p) {
        return set(0, optional().setPresedence(precedence, p));
    }

}
