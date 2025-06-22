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

package org.modelingvalue.nelumbo.syntax;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.Structure;
import org.modelingvalue.nelumbo.syntax.Token.TokenType;

public class CallParselet extends Prefix2Parselet {

    public final static CallParselet            INSTANCE  = new CallParselet();

    private final Map<String, FunctionWithArgs> functions = new HashMap<>();

    private CallParselet() {
    }

    @Override
    public Structure parse(Parser parser, Token token1, Token token2) throws ParseException {
        FunctionWithArgs function = getFunction(token1);
        List<Structure> args = List.of();
        for (int i = 0; i < function.nrOfArgs(); i++) {
            args = args.add(parser.parseExpression(0));
            if (i < function.nrOfArgs() - 1) {
                parser.consume(TokenType.COMMA);
            }
        }
        parser.consume(TokenType.RPAREN);
        return function.construct(token1, args);
    }

    private FunctionWithArgs getFunction(Token token) throws ParseException {
        FunctionWithArgs function = functions.get(token.text());
        if (function == null) {
            throw new ParseException("Could not parse \"" + token.text() + "\" at position " + token.position() + ".", token.position());
        }
        return function;
    }

    public static void register(FunctionWithArgs function) {
        INSTANCE.functions.put(function.text(), function);
    }

}
