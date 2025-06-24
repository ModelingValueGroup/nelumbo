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

public final class UnaryOperatorParselet extends PrefixParselet {

    public final static UnaryOperatorParselet INSTANCE = new UnaryOperatorParselet();

    private UnaryOperatorParselet() {
    }

    @Override
    public Node parse(Parser parser, Token token) throws ParseException {
        UnaryOperator unaryOperator = getOperator(parser, token);
        Type rightType = unaryOperator.right();
        int pos = parser.position();
        Node right = parser.parseNode(unaryOperator.precedence(), rightType);
        if (!rightType.isAssignableFrom(right.type())) {
            throw new ParseException("Expected type " + rightType + " and found " + right.type(), pos);
        }
        return unaryOperator.construct(token, right);
    }

    private UnaryOperator getOperator(Parser parser, Token token) throws ParseException {
        UnaryOperator unaryOperator = parser.knowledgeBase().unaryOperator(token.text());
        if (unaryOperator == null) {
            throw new ParseException("Could not parse \"" + token.text() + "\" at position " + token.position() + ".", token.position());
        }
        return unaryOperator;
    }

}
