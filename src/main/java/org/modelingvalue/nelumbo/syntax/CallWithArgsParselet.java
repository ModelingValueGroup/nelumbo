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

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;

public final class CallWithArgsParselet extends PrefixParselet {

    public final static CallWithArgsParselet INSTANCE = new CallWithArgsParselet();

    private CallWithArgsParselet() {
    }

    @Override
    public Node parse(Parser parser, Token token) throws ParseException {
        parser.consume(TokenType.LPAREN);
        List<Node> args = List.of();
        do {
            args = args.add(parser.parseNode(0, Node.TYPE));
        } while (parser.match(TokenType.COMMA));
        parser.consume(TokenType.RPAREN);
        CallWithArgs call = call(parser, token, args);
        return call.construct(token, args);
    }

    private CallWithArgs call(Parser parser, Token token, List<Node> args) throws ParseException {
        List<CallWithArgs> calls = parser.knowledgeBase().callsWithArgs(token.text());
        if (calls != null) {
            for (CallWithArgs call : calls) {
                if (call.isAssignableFrom(args)) {
                    return call;
                }
            }
        }
        calls = parser.knowledgeBase().callsWithArgs(CallWithArgs.WILDCARD);
        if (calls != null) {
            for (CallWithArgs call : calls) {
                if (call.isAssignableFrom(args)) {
                    return call;
                }
            }
        }
        List<Type> types = args.replaceAll(Node::type);
        String signature = types.toString().substring(4).replace('[', '(').replace(']', ')');
        throw new ParseException("Could not call " + token.text() + signature + ", at position " + token.position() + ".", token.position());
    }

}
