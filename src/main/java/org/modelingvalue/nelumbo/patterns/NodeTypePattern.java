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

package org.modelingvalue.nelumbo.patterns;

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParseResult;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Patterns;
import org.modelingvalue.nelumbo.syntax.Token;

public class NodeTypePattern extends Pattern {
    @Serial
    private static final long serialVersionUID = 6828401544789430678L;

    public NodeTypePattern(Type type, List<AstElement> elements, Object... args) {
        super(type, elements, args);
    }

    protected NodeTypePattern(Object[] args) {
        super(args);
    }

    @Override
    protected NodeTypePattern struct(Object[] array) {
        return new NodeTypePattern(array);
    }

    public Type nodeType() {
        return (Type) get(0);
    }

    @Override
    public Token parse(Token token, String group, int precedence, Parser parser, Pattern next, ParseResult result) throws ParseException {
        if (!result.isDone()) {
            Type type = nodeType();
            Node node = parser.parseNode(token, precedence, type.group());
            if (!type.isAssignableFrom(node.type())) {
                throw new ParseException("Expected element of type " + type + " but found " + node + " of type " + node.type(), node);
            }
            result.add(node);
            token = node.nextToken();
        }
        return token;
    }

    @Override
    public boolean peekIs(Token token, Parser parser) throws ParseException {
        return parser.preParse(token, nodeType().group(), null) != null;
    }

    @Override
    public Patterns patterns(Patterns patterns, int precedence) {
        return Patterns.EMPTY.put(nodeType(), patterns).setPrecedence(precedence).setExpected(nodeType());
    }

    @Override
    public boolean isFixed() {
        return true;
    }

    @Override
    public List<Type> args() {
        return List.of(nodeType());
    }

    @Override
    public String toString() {
        return "n(" + nodeType() + ")";
    }

}
