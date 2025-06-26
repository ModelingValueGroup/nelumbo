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

import org.modelingvalue.nelumbo.ListNode;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;

public final class PrefixOperatorParselet extends PrefixParselet {

    public final static PrefixOperatorParselet INSTANCE = new PrefixOperatorParselet();

    private PrefixOperatorParselet() {
    }

    @Override
    public Node parse(Parser parser, Token token) throws ParseException {
        PrefixOperator operator = operator(parser, token);
        if (operator == null) {
            throw new ParseException("Could not parse \"" + token.text() + "\" at position " + token.position() + ".", token.position());
        }
        Node right;
        Type rightType = operator.right();
        if (rightType.isList()) {
            Type elemType = rightType.element();
            right = new ListNode(elemType);
            do {
                int pos = parser.position();
                Node node = parser.parseNode(operator.precedence(), elemType);
                if (!elemType.isAssignableFrom(node.type())) {
                    throw new ParseException("Expected type " + elemType + " and found " + node + " of type " + node.type(), pos);
                }
                right = new ListNode((ListNode) right, node);
            } while (parser.match(TokenType.COMMA));
        } else {
            int pos = parser.position();
            right = parser.parseNode(operator.precedence(), rightType);
            if (!rightType.isAssignableFrom(right.type())) {
                throw new ParseException("Expected type " + rightType + " and found " + right + " of type " + right.type(), pos);
            }
        }
        return operator.construct(token, right);
    }

    private PrefixOperator operator(Parser parser, Token token) {
        PrefixOperator operator = operator(parser, token.text());
        if (operator == null) {
            operator = operator(parser, InfixOperator.WILDCARD);
        }
        return operator;
    }

    private PrefixOperator operator(Parser parser, String text) {
        return parser.knowledgeBase().prefixOperator(text);
    }

}
