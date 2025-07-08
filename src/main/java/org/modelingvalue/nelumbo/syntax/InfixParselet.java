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

public abstract class InfixParselet extends PostfixParselet {

    private final Type right;

    public InfixParselet(Type expected, Type left, TokenType type1, String oper1, TokenType type2, String oper2, Type right, int precedence) {
        super(expected, left, type1, oper1, type2, oper2, precedence);
        this.right = right;
    }

    public Type right() {
        return right;
    }

    @Override
    public Node parse(Type expected, Parser parser, Node left, Token token) throws ParseException {
        Token position = parser.peek();
        Node right = parser.parseNode(precedence(), right());
        if (!right().isAssignableFrom(right.type())) {
            throw new ParseException("Expected right of type " + right() + " and found " + right + " of type " + right.type(), position);
        }
        return construct(left, token, right);
    }

    public Node construct(Node left, Token token, Node right) throws ParseException {
        throw new UnsupportedOperationException();
    }

    public static InfixParselet of(Type left, String oper, Type right, int precedence, ThrowingTriFunction<Node, Token, Node, Node> constructor) {
        return of(null, left, null, oper, null, null, right, precedence, constructor);
    }

    public static InfixParselet of(Type left, TokenType type, Type right, int precedence, ThrowingTriFunction<Node, Token, Node, Node> constructor) {
        return of(null, left, type, null, null, null, right, precedence, constructor);
    }

    public static InfixParselet of(Type left, String oper1, String oper2, Type right, int precedence, ThrowingTriFunction<Node, Token, Node, Node> constructor) {
        return of(null, left, null, oper1, null, oper2, right, precedence, constructor);
    }

    public static InfixParselet of(Type left, TokenType type1, TokenType type2, Type right, int precedence, ThrowingTriFunction<Node, Token, Node, Node> constructor) {
        return of(null, left, type1, null, type2, null, right, precedence, constructor);
    }

    public static InfixParselet of(Type left, String oper1, TokenType type2, Type right, int precedence, ThrowingTriFunction<Node, Token, Node, Node> constructor) {
        return of(null, left, null, oper1, type2, null, right, precedence, constructor);
    }

    public static InfixParselet of(Type left, TokenType type1, String oper2, Type right, int precedence, ThrowingTriFunction<Node, Token, Node, Node> constructor) {
        return of(null, left, type1, null, null, oper2, right, precedence, constructor);
    }

    public static InfixParselet of(Type expected, Type left, String oper, Type right, int precedence, ThrowingTriFunction<Node, Token, Node, Node> constructor) {
        return of(expected, left, null, oper, null, null, right, precedence, constructor);
    }

    public static InfixParselet of(Type expected, Type left, TokenType type, Type right, int precedence, ThrowingTriFunction<Node, Token, Node, Node> constructor) {
        return of(expected, left, type, null, null, null, right, precedence, constructor);
    }

    public static InfixParselet of(Type expected, Type left, String oper1, String oper2, Type right, int precedence, ThrowingTriFunction<Node, Token, Node, Node> constructor) {
        return of(expected, left, null, oper1, null, oper2, right, precedence, constructor);
    }

    public static InfixParselet of(Type expected, Type left, TokenType type1, TokenType type2, Type right, int precedence, ThrowingTriFunction<Node, Token, Node, Node> constructor) {
        return of(expected, left, type1, null, type2, null, right, precedence, constructor);
    }

    public static InfixParselet of(Type expected, Type left, String oper1, TokenType type2, Type right, int precedence, ThrowingTriFunction<Node, Token, Node, Node> constructor) {
        return of(expected, left, null, oper1, type2, null, right, precedence, constructor);
    }

    public static InfixParselet of(Type expected, Type left, TokenType type1, String oper2, Type right, int precedence, ThrowingTriFunction<Node, Token, Node, Node> constructor) {
        return of(expected, left, type1, null, null, oper2, right, precedence, constructor);
    }

    private static InfixParselet of(Type expected, Type left, TokenType type1, String oper1, TokenType type2, String oper2, Type right, int precedence, ThrowingTriFunction<Node, Token, Node, Node> constructor) {
        return new InfixParselet(expected, left, type1, oper1, type2, oper2, right, precedence) {
            @Override
            public Node construct(Node left, Token token, Node right) throws ParseException {
                return constructor.apply(left, token, right);
            }
        };
    }

}
