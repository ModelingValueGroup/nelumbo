//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2025 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
//                                                                                                                     ~
// Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in      ~
// compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0  ~
// Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on ~
// an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the  ~
// specific language governing permissions and limitations under the License.                                          ~
//                                                                                                                     ~
// Maintainers:                                                                                                        ~
//     Wim Bast, Tom Brus                                                                                              ~
//                                                                                                                     ~
// Contributors:                                                                                                       ~
//     Victor Lap                                                                                                      ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

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

    public int leftPrecedence() {
        Integer precedence = (Integer) get(1);
        return precedence != null ? precedence : Integer.MAX_VALUE;
    }

    public int innerPrecedence() {
        Integer precedence = (Integer) get(1);
        return precedence != null ? precedence : Integer.MIN_VALUE;
    }

    @Override
    public Token parse(Token token, String group, Parser parser, Pattern next, ParseResult result) throws ParseException {
        if (!result.isDone()) {
            Type type = nodeType();
            Node node = parser.parseNode(token, innerPrecedence(), type.group());
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
    public Patterns patterns(Patterns patterns) {
        return Patterns.EMPTY.put(nodeType(), patterns).setPrecedence(innerPrecedence()).setExpected(nodeType());
    }

    @Override
    public boolean isFixed() {
        return true;
    }

    @Override
    public List<Pattern> fixed(List<Pattern> fixed, boolean[] stop) {
        return fixed.add(this);
    }

    @Override
    public List<Type> args() {
        return List.of(nodeType());
    }

    @Override
    public String toString() {
        Integer precedence = (Integer) get(1);
        return "n(" + nodeType() + (precedence != null ? precedence : "") + ")";
    }

    @Override
    public Pattern setPresedence(List<Integer> precedence, int[] p) {
        int i = p[0];
        if (i < precedence.size() - 1) {
            p[0]++;
        }
        return set(1, precedence.get(i));
    }

}
