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

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;

public abstract class FirstPattern extends LanguagePattern {
    private static final long        serialVersionUID = -6185026591038770193L;

    public final static FirstPattern PAREN_PATTERN    = of(TokenPattern.of(TokenType.LPAREN), null, TokenPattern.of(TokenType.RPAREN),   //
            0, (t, n, e) -> n);

    public static FirstPattern of(TokenPattern pattern, ThrowingFunction<Token[], Node> constructor) {
        return new FirstPattern(null, pattern, null, null, null, 0) {
            private static final long serialVersionUID = -7461452646929634385L;

            @Override
            public Node construct(Token[] tokens) throws ParseException {
                return constructor.apply(tokens);
            }
        };
    }

    public static FirstPattern of(TokenPattern pattern, Type rigth, int precedence, ThrowingBiFunction<Token[], Node, Node> constructor) {
        return new FirstPattern(null, pattern, rigth, null, TokenPattern.of(), precedence) {
            private static final long serialVersionUID = 32178584417152896L;

            @Override
            public Node construct(Token[] tokens, Node rigth, Token[] end) throws ParseException {
                return constructor.apply(tokens, rigth);
            }
        };
    }

    public static FirstPattern of(TokenPattern pattern, Type rigth, TokenPattern end, int precedence, ThrowingTriFunction<Token[], Node, Token[], Node> constructor) {
        return new FirstPattern(null, pattern, rigth, null, end, precedence) {
            private static final long serialVersionUID = -6608756324374932892L;

            @Override
            public Node construct(Token[] tokens, Node rigth, Token[] end) throws ParseException {
                return constructor.apply(tokens, rigth, end);
            }
        };
    }

    public static FirstPattern of(TokenPattern pattern, Type rigth, TokenPattern seperator, TokenPattern end) {
        return new FirstPattern(null, pattern, rigth, seperator, end, 0) {
            private static final long serialVersionUID = -7623705746326521488L;

            @Override
            public boolean isCall() {
                return true;
            }
        };
    }

    public static FirstPattern of(TokenPattern pattern, Type rigth, TokenPattern seperator, TokenPattern end, ThrowingTriFunction<Token[], List<Node>, Token[], Node> constructor) {
        return new FirstPattern(null, pattern, rigth, seperator, end, 0) {
            private static final long serialVersionUID = 8737334045216964736L;

            @Override
            public Node construct(Token[] tokens, List<Node> rigth, Token[] end) throws ParseException {
                return constructor.apply(tokens, rigth, end);
            }
        };
    }

    public static FirstPattern of(Type expected, TokenPattern pattern, ThrowingFunction<Token[], Node> constructor) {
        return new FirstPattern(expected, pattern, null, null, null, 0) {
            private static final long serialVersionUID = -1588497865531306689L;

            @Override
            public Node construct(Token[] tokens) throws ParseException {
                return constructor.apply(tokens);
            }
        };
    }

    public static FirstPattern of(Type expected, TokenPattern pattern, Type rigth, int precedence, ThrowingBiFunction<Token[], Node, Node> constructor) {
        return new FirstPattern(expected, pattern, rigth, null, TokenPattern.of(), precedence) {
            private static final long serialVersionUID = -8340731947928008879L;

            @Override
            public Node construct(Token[] tokens, Node rigth, Token[] end) throws ParseException {
                return constructor.apply(tokens, rigth);
            }
        };
    }

    public static FirstPattern of(Type expected, TokenPattern pattern, Type rigth, TokenPattern end, int precedence, ThrowingTriFunction<Token[], Node, Token[], Node> constructor) {
        return new FirstPattern(expected, pattern, rigth, null, end, precedence) {
            private static final long serialVersionUID = 6458021560074667246L;

            @Override
            public Node construct(Token[] tokens, Node rigth, Token[] end) throws ParseException {
                return constructor.apply(tokens, rigth, end);
            }
        };
    }

    public static FirstPattern of(Type expected, TokenPattern pattern, Type rigth, TokenPattern seperator, TokenPattern end) {
        return new FirstPattern(expected, pattern, rigth, seperator, end, 0) {
            private static final long serialVersionUID = -7678004400263925045L;

            @Override
            public boolean isCall() {
                return true;
            }
        };
    }

    public static FirstPattern of(Type expected, TokenPattern pattern, Type rigth, TokenPattern seperator, TokenPattern end, ThrowingTriFunction<Token[], List<Node>, Token[], Node> constructor) {
        return new FirstPattern(expected, pattern, rigth, seperator, end, 0) {
            private static final long serialVersionUID = 3309799934206408277L;

            @Override
            public Node construct(Token[] tokens, List<Node> rigth, Token[] end) throws ParseException {
                return constructor.apply(tokens, rigth, end);
            }
        };
    }

    private FirstPattern(Type expected, TokenPattern pattern, Type rigth, TokenPattern seperator, TokenPattern end, int precedence) {
        super(expected, null, pattern, rigth, seperator, end, precedence);
    }

}
