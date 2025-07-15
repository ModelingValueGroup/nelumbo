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

import java.util.concurrent.atomic.AtomicReference;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.struct.impl.StructImpl;
import org.modelingvalue.collections.util.Pair;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;

public abstract class LanguagePattern extends StructImpl {
    private static final long                     serialVersionUID = 3273746815229326265L;

    private final AtomicReference<List<WithArgs>> withArgs;

    protected LanguagePattern(Type expected, Type left, TokenPattern tokens, Integer precedence, RightPattern... rights) {
        super(expected, left, tokens, precedence, List.of(rights));
        this.withArgs = isCall() ? new AtomicReference<>() : null;
    }

    public final Type expected() {
        return (Type) get(0);
    }

    public final Type left() {
        return (Type) get(1);
    }

    public final TokenPattern tokens() {
        return (TokenPattern) get(2);
    }

    @SuppressWarnings("unchecked")
    public final List<RightPattern> rights() {
        return (List<RightPattern>) get(3);
    }

    public final Integer precedence() {
        return (Integer) get(4);
    }

    public boolean isCall() {
        return false;
    }

    public Node construct(Token[] tokens) throws ParseException {
        throw new UnsupportedOperationException();
    }

    public Node construct(Token[] tokens, Node rigth, Token[] end) throws ParseException {
        throw new UnsupportedOperationException();
    }

    public Node construct(Token[] tokens, List<Node> rigth, Token[] end) throws ParseException {
        throw new UnsupportedOperationException();
    }

    public Node construct(Node left, Token[] tokens) throws ParseException {
        throw new UnsupportedOperationException();
    }

    public Node construct(Node left, Token[] tokens, Node rigth, Token[] end) throws ParseException {
        throw new UnsupportedOperationException();
    }

    public Node construct(Node left, Token[] tokens, List<Node> rigth, Token[] end) throws ParseException {
        throw new UnsupportedOperationException();
    }

    public Node parse(Parser parser, Type expected, Token[] tokens) throws ParseException {
        return parse(parser, expected, null, tokens);
    }

    public Node parse(Parser parser, Type expected, Node left, Token[] tokens) throws ParseException {
        if (seperator() != null) {
            List<Node> list = List.of();
            Token[] end;
            Type type = right() != null ? right() : expected;
            if ((end = parser.match(end())) == null) {
                do {
                    Node node = parser.parseNode(0, isCall() ? Type.NODE : right());
                    if (!isCall() && !type.isAssignableFrom(node.type())) {
                        Pair<Token, Token> pos = parser.nodePosition.get(node);
                        throw new ParseException("Expected type " + type + " and found " + node + " of type " + node.type(), pos.a(), pos.b());
                    }
                    list = list.add(node);
                } while (parser.match(seperator()) != null);
                end = parser.consume(end());
            }
            if (isCall()) {
                List<Type> types = list.replaceAll(Node::type);
                WithArgs call = call(types);
                if (call != null) {
                    return left != null ? call.construct(left, tokens, list, end) : call.construct(tokens, list, end);
                }
                String signature = types.toString().substring(4).replace('[', '(').replace(']', ')');
                throw new ParseException("Could not call " + parser.toString(tokens) + signature, tokens[0], parser.last());
            } else {
                return left != null ? construct(left, tokens, list, end) : construct(tokens, list, end);
            }
        } else if (end() != null) {
            Type type = right() != null ? right() : expected;
            Node right = parser.parseNode(precedence(), type);
            if (!type.isAssignableFrom(right.type())) {
                Pair<Token, Token> pos = parser.nodePosition.get(right);
                throw new ParseException("Expected type " + type + " and found " + right + " of type " + right.type(), pos.a(), pos.b());
            }
            Token[] end = parser.consume(end());
            return left != null ? construct(left, tokens, right, end) : construct(tokens, right, end);
        } else {
            return left != null ? construct(left, tokens) : construct(tokens);
        }
    }

    private WithArgs call(List<Type> args) throws ParseException {
        List<WithArgs> calls = withArgs.get();
        if (calls != null) {
            for (WithArgs call : calls) {
                if (call.isAssignableFrom(args)) {
                    return call;
                }
            }
        }
        return null;
    }

    public void addCallWithArgs(WithArgs withArgs) {
        this.withArgs.updateAndGet(calls -> addCall(withArgs, calls));
    }

    private List<WithArgs> addCall(WithArgs withArgs, List<WithArgs> list) {
        if (list == null) {
            return List.of(withArgs);
        } else {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).isAssignableFrom(withArgs)) {
                    return list.insert(i, withArgs);
                }
            }
            return list.append(withArgs);
        }
    }

}
