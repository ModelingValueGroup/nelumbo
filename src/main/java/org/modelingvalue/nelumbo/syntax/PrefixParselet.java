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

import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;

public abstract class PrefixParselet extends AtomicParselet {

    private final Type right;
    private final int  precedence;

    protected PrefixParselet(Type expected, TokenType type1, String oper1, TokenType type2, String oper2, Type right, int precedence) {
        super(expected, type1, oper1, type2, oper2);
        this.right = right;
        this.precedence = precedence;
    }

    public Type right() {
        return right;
    }

    public int precedence() {
        return precedence;
    }

    @Override
    public Node parse(Parser parser, Token token) throws ParseException {
        Node right = parser.parseNode(precedence(), right());
        return construct(token, right);
    }

    public Node construct(Token token, Node right) throws ParseException {
        throw new UnsupportedOperationException();
    }

    public static PrefixParselet of(String oper, Type right, int precedence, ThrowingBiFunction<Token, Node, Node> constructor) {
        return of(null, null, oper, null, null, right, precedence, constructor);
    }

    public static PrefixParselet of(TokenType type, Type right, int precedence, ThrowingBiFunction<Token, Node, Node> constructor) {
        return of(null, type, null, null, null, right, precedence, constructor);
    }

    public static PrefixParselet of(Type expected, String oper, Type right, int precedence, ThrowingBiFunction<Token, Node, Node> constructor) {
        return of(expected, null, oper, null, null, right, precedence, constructor);
    }

    public static PrefixParselet of(Type expected, TokenType type, Type right, int precedence, ThrowingBiFunction<Token, Node, Node> constructor) {
        return of(expected, type, null, null, null, right, precedence, constructor);
    }

    public static PrefixParselet of(String oper1, String oper2, Type right, int precedence, ThrowingBiFunction<Token, Node, Node> constructor) {
        return of(null, null, oper1, null, oper2, right, precedence, constructor);
    }

    public static PrefixParselet of(TokenType type1, TokenType type2, Type right, int precedence, ThrowingBiFunction<Token, Node, Node> constructor) {
        return of(null, type1, null, type2, null, right, precedence, constructor);
    }

    public static PrefixParselet of(String oper1, TokenType type2, Type right, int precedence, ThrowingBiFunction<Token, Node, Node> constructor) {
        return of(null, null, oper1, type2, null, right, precedence, constructor);
    }

    public static PrefixParselet of(TokenType type1, String oper2, Type right, int precedence, ThrowingBiFunction<Token, Node, Node> constructor) {
        return of(null, type1, null, null, oper2, right, precedence, constructor);
    }

    private static PrefixParselet of(Type expected, TokenType type1, String oper1, TokenType type2, String oper2, Type right, int precedence, ThrowingBiFunction<Token, Node, Node> constructor) {
        return new PrefixParselet(expected, type1, oper1, type2, oper2, right, precedence) {
            @Override
            public Node construct(Token token, Node right) throws ParseException {
                return constructor.apply(token, right);
            }
        };
    }

}
