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
import org.modelingvalue.collections.struct.impl.StructImpl;
import org.modelingvalue.collections.util.Pair;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;

public abstract class LanguagePattern extends StructImpl {
    private static final long serialVersionUID = 3273746815229326265L;

    protected LanguagePattern(Type expected, Integer precedence, Object... parts) {
        super(expected, left(parts), tokens(parts), parts(parts), precedence);
    }

    private static Type left(Object... parts) {
        return parts[0] instanceof Type && ((Type) parts[0]).tokenType() == null ? (Type) parts[0] : null;
    }

    private static List<Object> tokens(Object... parts) {
        List<Object> tokens = List.of();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i] instanceof Type && ((Type) parts[i]).tokenType() == null) {
                if (i > 0) {
                    break;
                }
            } else if (parts[i] instanceof Type && ((Type) parts[i]).tokenType() != null) {
                tokens = tokens.add(((Type) parts[i]).tokenType());
            } else if (parts[i] instanceof String) {
                tokens = tokens.add(parts[i]);
            } else {
                throw new IllegalArgumentException("Illegal pattern part " + parts[i]);
            }
        }
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("A pattern must contain tokens");
        }
        return tokens;
    }

    private static List<Object> parts(Object... parts) {
        List<Object> result = List.of();
        List<Object> tokens = List.of();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i] instanceof Type && ((Type) parts[i]).tokenType() == null) {
                if (i > 0) {
                    if (!result.isEmpty()) {
                        if (tokens.isEmpty()) {
                            throw new IllegalArgumentException("Types must be seperated by tokens");
                        } else {
                            result = result.add(tokens);
                        }
                    }
                    tokens = List.of();
                    result = result.add(parts[i]);
                }
            } else if (parts[i] instanceof Type && ((Type) parts[i]).tokenType() != null) {
                if (result.last() instanceof Type && ((Type) result.last()).isList()) {
                    result = result.add(((Type) parts[i]).tokenType());
                } else {
                    tokens = tokens.add(((Type) parts[i]).tokenType());
                }
            } else if (parts[i] instanceof String) {
                if (result.last() instanceof Type && ((Type) result.last()).isList()) {
                    result = result.add(parts[i]);
                } else {
                    tokens = tokens.add(parts[i]);
                }
            } else {
                throw new IllegalArgumentException("Illegal pattern part " + parts[i]);
            }
        }
        if (!tokens.isEmpty()) {
            result = result.add(tokens);
        }
        return result;
    }

    public final Type expected() {
        return (Type) get(0);
    }

    public final Type left() {
        return (Type) get(1);
    }

    @SuppressWarnings("unchecked")
    public final List<Object> tokens() {
        return (List<Object>) get(2);
    }

    @SuppressWarnings("unchecked")
    public final List<Object> parts() {
        return (List<Object>) get(3);
    }

    public final Integer precedence() {
        return (Integer) get(4);
    }

    @SuppressWarnings("unchecked")
    public final List<Type> args() {
        return parts().filter(Type.class).asList();
    }

    public Node construct(List<Token> tokens, List<Node> nodes) throws ParseException {
        throw new UnsupportedOperationException();
    }

    public Node construct(Node left, List<Token> tokens, List<Node> nodes) throws ParseException {
        throw new UnsupportedOperationException();
    }

    public Node parse(Parser parser, Type expected, List<Token> tokens) throws ParseException {
        return parse(parser, expected, null, tokens);
    }

    @SuppressWarnings("unchecked")
    public Node parse(Parser parser, Type expected, Node left, List<Token> tokens) throws ParseException {
        List<Node> nodes = List.of();
        Integer precedence = precedence();
        List<Object> parts = parts();
        for (int i = 0; i < parts.size(); i++) {
            Object part = parts.get(i);
            if (part instanceof Type) {
                Type type = (Type) part;
                if (type.isList()) {
                    type = type.element();
                    Object separator = parts.get(++i);
                    List<Object> end = (List<Object>) parts.get(++i);
                    List<Token> stop = parser.match(end);
                    if (stop == null) {
                        do {
                            Node node = parser.parseNode(precedence, type);
                            checkType(parser, type, node);
                            nodes = nodes.add(node);
                            Token sep = parser.match(separator);
                            if (sep != null) {
                                tokens = tokens.add(sep);
                            } else {
                                break;
                            }
                        } while (true);
                        stop = parser.consume(end);
                    }
                    tokens = tokens.addAll(stop);
                } else {
                    Node node = parser.parseNode(precedence, type);
                    checkType(parser, type, node);
                    nodes = nodes.add(node);
                }
            } else {
                tokens = tokens.addAll(parser.consume((List<Object>) part));
            }
        }
        return left != null ? construct(left, tokens, nodes) : construct(tokens, nodes);
    }

    private void checkType(Parser parser, Type type, Node node) throws ParseException {
        if (!type.isAssignableFrom(node.type())) {
            Pair<Token, Token> pos = parser.nodePosition.get(node);
            throw new ParseException("Expected type " + type + " and found " + node + " of type " + node.type(), pos.a(), pos.b());
        }
    }

}
