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

import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.util.Pair;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;

public final class BinaryOperatorParselet extends InfixParselet {

    public final static BinaryOperatorParselet                      INSTANCE        = new BinaryOperatorParselet();

    private final java.util.Map<Pair<Type, String>, BinaryOperator> binaryOperators =                              //
            Map.<Pair<Type, String>, BinaryOperator> of().toMutable();

    private BinaryOperatorParselet() {
    }

    @Override
    public Node parse(Parser parser, Node left, Token token) throws ParseException {
        BinaryOperator binaryOperator = getOperator(left, token);
        int pos = parser.position();
        Node right = parser.parseNode(binaryOperator.precedence());
        if (!binaryOperator.right().isAssignableFrom(right.type())) {
            throw new ParseException("Expected type " + binaryOperator.right() + " and found " + right.type(), pos);
        }
        return binaryOperator.construct(token, left, right);
    }

    @Override
    public int precedence(Node left, Token token) throws ParseException {
        BinaryOperator binaryOperator = getOperator(left, token);
        return binaryOperator.precedence();
    }

    private BinaryOperator getOperator(Node left, Token token) throws ParseException {
        Set<Type> pre, post = Set.of(left.type());
        BinaryOperator binaryOperator = null;
        while (!post.isEmpty()) {
            pre = post;
            post = Set.of();
            for (Type type : pre) {
                binaryOperator = binaryOperators.get(Pair.of(type, token.text()));
                if (binaryOperator != null) {
                    return binaryOperator;
                } else {
                    post = post.addAll(type.supers());
                }
            }
        }
        throw new ParseException("Could not parse \"" + token.text() + "\" at position " + token.position() + ".", token.position());
    }

    public void register(BinaryOperator operator) {
        Pair<Type, String> pair = Pair.of(operator.left(), operator.oper());
        if (binaryOperators.containsKey(pair)) {
            throw new IllegalArgumentException();
        }
        binaryOperators.put(pair, operator);
    }

}
