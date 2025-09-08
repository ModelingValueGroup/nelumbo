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

import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParseResult;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Token;

public class RepetitionPattern extends AbstractPattern {
    @Serial
    private static final long           serialVersionUID = 7257418785045060245L;

    public static final AbstractPattern PATTERN          = SequencePattern.of(TokenTextPattern.of("`{`"), SequencePattern.PATTERN, TokenTextPattern.of("`}`"));

    public static RepetitionPattern of(AbstractPattern repeated) {
        return new RepetitionPattern(Type.PATTERN, Token.EMPTY, repeated);
    }

    public RepetitionPattern(Type type, Token[] tokens, Object... args) {
        super(type, tokens, args);
    }

    protected RepetitionPattern(Object[] args, int start) {
        super(args, start);
    }

    @Override
    protected RepetitionPattern struct(Object[] array, int start) {
        return new RepetitionPattern(array, start);
    }

    public AbstractPattern repeated() {
        return (AbstractPattern) get(0);
    }

    @Override
    public void parse(Type expected, int precedence, Parser parser, AbstractPattern next, ParseResult result) throws ParseException {
        AbstractPattern repeated = repeated();
        while (repeated.peekIs(parser)) {
            repeated.parse(expected, precedence, parser, next, result);
        }
    }

    @Override
    public boolean isFixed() {
        return false;
    }

}
