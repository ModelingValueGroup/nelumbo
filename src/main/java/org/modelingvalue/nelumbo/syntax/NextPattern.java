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

public abstract class NextPattern extends LanguagePattern {
    private static final long serialVersionUID = 454522286804275900L;

    public static NextPattern of(Type left, TokenPattern pattern, int precedence, ThrowingBiFunction<Node, Token[], Node> constructor) {
        return new NextPattern(null, left, pattern, null, null, null, precedence) {
            private static final long serialVersionUID = -6180561090411405249L;

            @Override
            public Node construct(Node left, Token[] tokens) throws ParseException {
                return constructor.apply(left, tokens);
            }
        };
    }

    public static NextPattern of(Type left, TokenPattern pattern, Type rigth, int precedence, ThrowingTriFunction<Node, Token[], Node, Node> constructor) {
        return new NextPattern(null, left, pattern, rigth, null, TokenPattern.of(), precedence) {
            private static final long serialVersionUID = 2558378206850607266L;

            @Override
            public Node construct(Node left, Token[] tokens, Node right, Token[] end) throws ParseException {
                return constructor.apply(left, tokens, right);
            }
        };
    }

    public static NextPattern of(Type left, TokenPattern pattern, Type rigth, TokenPattern end, int precedence, ThrowingQuadFunction<Node, Token[], Node, Token[], Node> constructor) {
        return new NextPattern(null, left, pattern, rigth, null, end, precedence) {
            private static final long serialVersionUID = -7061370687053126798L;

            @Override
            public Node construct(Node left, Token[] tokens, Node right, Token[] end) throws ParseException {
                return constructor.apply(left, tokens, right, end);
            }
        };
    }

    public static NextPattern of(Type left, TokenPattern pattern, Type rigth, TokenPattern seperator, TokenPattern end, int precedence) {
        return new NextPattern(null, left, pattern, rigth, seperator, end, precedence) {
            private static final long serialVersionUID = -6245096078733853368L;

            @Override
            public boolean isCall() {
                return true;
            }
        };
    }

    public static NextPattern of(Type left, TokenPattern pattern, Type rigth, TokenPattern seperator, TokenPattern end, int precedence, ThrowingQuadFunction<Node, Token[], List<Node>, Token[], Node> constructor) {
        return new NextPattern(null, left, pattern, rigth, seperator, end, precedence) {
            private static final long serialVersionUID = 4182999238476953457L;

            @Override
            public Node construct(Node left, Token[] tokens, List<Node> right, Token[] end) throws ParseException {
                return constructor.apply(left, tokens, right, end);
            }
        };
    }

    public static NextPattern of(Type expected, Type left, TokenPattern pattern, int precedence, ThrowingBiFunction<Node, Token[], Node> constructor) {
        return new NextPattern(expected, left, pattern, null, null, null, precedence) {
            private static final long serialVersionUID = -6252017035858600699L;

            @Override
            public Node construct(Node left, Token[] tokens) throws ParseException {
                return constructor.apply(left, tokens);
            }
        };
    }

    public static NextPattern of(Type expected, Type left, TokenPattern pattern, Type rigth, int precedence, ThrowingTriFunction<Node, Token[], Node, Node> constructor) {
        return new NextPattern(expected, left, pattern, rigth, null, TokenPattern.of(), precedence) {
            private static final long serialVersionUID = 4269050340826884280L;

            @Override
            public Node construct(Node left, Token[] tokens, Node right, Token[] end) throws ParseException {
                return constructor.apply(left, tokens, right);
            }
        };
    }

    public static NextPattern of(Type expected, Type left, TokenPattern pattern, Type rigth, TokenPattern end, int precedence, ThrowingQuadFunction<Node, Token[], Node, Token[], Node> constructor) {
        return new NextPattern(expected, left, pattern, rigth, null, end, precedence) {
            private static final long serialVersionUID = 4352261095342068054L;

            @Override
            public Node construct(Node left, Token[] tokens, Node right, Token[] end) throws ParseException {
                return constructor.apply(left, tokens, right, end);
            }
        };
    }

    public static NextPattern of(Type expected, Type left, TokenPattern pattern, Type rigth, TokenPattern seperator, TokenPattern end, int precedence) {
        return new NextPattern(expected, left, pattern, rigth, seperator, end, precedence) {
            private static final long serialVersionUID = 2923285697314018477L;

            @Override
            public boolean isCall() {
                return true;
            }
        };
    }

    public static NextPattern of(Type expected, Type left, TokenPattern pattern, Type rigth, TokenPattern seperator, TokenPattern end, int precedence, ThrowingQuadFunction<Node, Token[], List<Node>, Token[], Node> constructor) {
        return new NextPattern(expected, left, pattern, rigth, seperator, end, precedence) {
            private static final long serialVersionUID = 3399062226908627075L;

            @Override
            public Node construct(Node left, Token[] tokens, List<Node> right, Token[] end) throws ParseException {
                return constructor.apply(left, tokens, right, end);
            }
        };
    }

    private NextPattern(Type expected, Type left, TokenPattern pattern, Type rigth, TokenPattern seperator, TokenPattern end, int precedence) {
        super(expected, left, pattern, rigth, seperator, end, precedence);
    }

}
