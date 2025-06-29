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

import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;

public abstract class AtomicParselet extends Parselet {

    private final Type expected;

    protected AtomicParselet(Type expected, TokenType type1, String oper1, TokenType type2, String oper2) {
        super(type1, oper1, type2, oper2);
        this.expected = expected;
    }

    public Type expected() {
        return expected;
    }

    public Node parse(Parser parser, Token token) throws ParseException {
        return construct(token);
    }

    public Node construct(Token token) throws ParseException {
        throw new UnsupportedOperationException();
    }

    public static AtomicParselet of(String oper, ThrowingFunction<Token, Node> constructor) {
        return of(null, null, oper, null, null, constructor);
    }

    public static AtomicParselet of(TokenType type, ThrowingFunction<Token, Node> constructor) {
        return of(null, type, null, null, null, constructor);
    }

    public static AtomicParselet of(Type expected, String oper, ThrowingFunction<Token, Node> constructor) {
        return of(expected, null, oper, null, null, constructor);
    }

    public static AtomicParselet of(Type expected, TokenType type, ThrowingFunction<Token, Node> constructor) {
        return of(expected, type, null, null, null, constructor);
    }

    public static AtomicParselet of(String oper1, String oper2, ThrowingFunction<Token, Node> constructor) {
        return of(null, null, oper1, null, oper2, constructor);
    }

    public static AtomicParselet of(TokenType type1, TokenType type2, ThrowingFunction<Token, Node> constructor) {
        return of(null, type1, null, type2, null, constructor);
    }

    public static AtomicParselet of(String oper1, TokenType type2, ThrowingFunction<Token, Node> constructor) {
        return of(null, null, oper1, type2, null, constructor);
    }

    public static AtomicParselet of(TokenType type1, String oper2, ThrowingFunction<Token, Node> constructor) {
        return of(null, type1, null, null, oper2, constructor);
    }

    private static AtomicParselet of(Type expected, TokenType type1, String oper1, TokenType type2, String oper2, ThrowingFunction<Token, Node> constructor) {
        return new AtomicParselet(expected, type1, oper1, type2, oper2) {
            @Override
            public Node construct(Token token) throws ParseException {
                return constructor.apply(token);
            }
        };
    }

}
