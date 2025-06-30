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

public abstract class PostfixParselet extends Parselet {

    private final Type left;
    private final int  precedence;

    protected PostfixParselet(Type left, TokenType type1, String oper1, TokenType type2, String oper2, int precedence) {
        super(type1, oper1, type2, oper2);
        this.left = left;
        this.precedence = precedence;
    }

    public Type left() {
        return left;
    }

    public int precedence() {
        return precedence;
    }

    public Node parse(Parser parser, Node left, Token token) throws ParseException {
        return construct(left, token);
    }

    public Node construct(Node left, Token token) throws ParseException {
        throw new UnsupportedOperationException();
    }

    public static PostfixParselet of(Type left, String oper, int precedence, ThrowingBiFunction<Node, Token, Node> constructor) {
        return of(left, null, oper, null, null, precedence, constructor);
    }

    public static PostfixParselet of(Type left, TokenType type, int precedence, ThrowingBiFunction<Node, Token, Node> constructor) {
        return of(left, type, null, null, null, precedence, constructor);
    }

    public static PostfixParselet of(Type left, String oper1, String oper2, int precedence, ThrowingBiFunction<Node, Token, Node> constructor) {
        return of(left, null, oper1, null, oper2, precedence, constructor);
    }

    public static PostfixParselet of(Type left, TokenType type1, TokenType type2, int precedence, ThrowingBiFunction<Node, Token, Node> constructor) {
        return of(left, type1, null, type2, null, precedence, constructor);
    }

    public static PostfixParselet of(Type left, String oper1, TokenType type2, int precedence, ThrowingBiFunction<Node, Token, Node> constructor) {
        return of(left, null, oper1, type2, null, precedence, constructor);
    }

    public static PostfixParselet of(Type left, TokenType type1, String oper2, int precedence, ThrowingBiFunction<Node, Token, Node> constructor) {
        return of(left, type1, null, null, oper2, precedence, constructor);
    }

    private static PostfixParselet of(Type left, TokenType type1, String oper1, TokenType type2, String oper2, int precedence, ThrowingBiFunction<Node, Token, Node> constructor) {
        return new PostfixParselet(left, type1, oper1, type2, oper2, precedence) {
            @Override
            public Node construct(Node left, Token token) throws ParseException {
                return constructor.apply(left, token);
            }
        };
    }

}
