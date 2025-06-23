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
import java.util.HashMap;
import java.util.Map;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;

public class CallWithArgsParselet extends Prefix2Parselet {

    public final static CallWithArgsParselet      INSTANCE   = new CallWithArgsParselet();

    private final Map<String, List<CallWithArgs>> signatures = new HashMap<>();

    private CallWithArgsParselet() {
    }

    @Override
    public Node parse(Parser parser, Token token1, Token token2) throws ParseException {
        List<Node> args = List.of();
        do {
            args = args.add(parser.parseNode(0));
        } while (parser.match(TokenType.COMMA));
        parser.consume(TokenType.RPAREN);
        CallWithArgs call = getCall(token1, args);
        return call.construct(token1, args);
    }

    private CallWithArgs getCall(Token token, List<Node> args) throws ParseException {
        List<CallWithArgs> calls = signatures.get(token.text());
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

    public void register(CallWithArgs call) {
        signatures.compute(call.name(), (k, v) -> {
            if (v == null) {
                return List.of(call);
            } else {
                for (int i = 0; i < v.size(); i++) {
                    if (v.get(i).isAssignableFrom(call)) {
                        return v.insert(i, call);
                    }
                }
                return v.append(call);
            }
        });
    }

}
