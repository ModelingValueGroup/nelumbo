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
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;

public final class CallWithArgsParselet extends AtomicParselet {

    public CallWithArgsParselet(Type expected, String name) {
        super(expected, null, name, TokenType.LPAREN, null);
    }

    public CallWithArgsParselet(Type expected, TokenType type) {
        super(expected, type, null, TokenType.LPAREN, null);
    }

    @Override
    public Node parse(Type expected, Parser parser, Token token) throws ParseException {
        Token lparen = parser.consume(TokenType.LPAREN);
        List<Node> args = List.of();
        do {
            args = args.add(parser.parseNode(0, Type.NODE));
        } while (parser.match(TokenType.COMMA));
        Token rparen = parser.consume(TokenType.RPAREN);
        List<Type> types = args.replaceAll(Node::type);
        CallWithArgs call = call(parser, token, types);
        if (call != null) {
            return call.construct(token, args).setTokens(Token.concat(token, lparen, args, rparen));
        }
        String signature = types.toString().substring(4).replace('[', '(').replace(']', ')');
        throw new ParseException("Could not call " + token.text() + signature, token, rparen);
    }

    private CallWithArgs call(Parser parser, Token token, List<Type> args) {
        KnowledgeBase kb = parser.knowledgeBase();
        List<CallWithArgs> calls = kb.callsWithArgs(expected(), token);
        if (calls != null) {
            for (CallWithArgs call : calls) {
                if (call.isAssignableFrom(args)) {
                    return call;
                }
            }
        }
        return null;
    }
}
