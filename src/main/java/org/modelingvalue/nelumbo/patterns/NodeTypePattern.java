//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2026 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
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
import java.util.function.Function;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.mutable.MutableList;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.ConstructionReason;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.NodeInfo;
import org.modelingvalue.nelumbo.lang.Functor;
import org.modelingvalue.nelumbo.lang.Type;
import org.modelingvalue.nelumbo.lang.Variable;
import org.modelingvalue.nelumbo.syntax.ParseContext;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParseState;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class NodeTypePattern extends Pattern {
    @Serial
    private static final long serialVersionUID = 6828401544789430678L;

    public NodeTypePattern(Type type, List<AstElement> elements, Type nodeType, Integer precedence, Boolean visible) {
        super(NodeInfo.of(type, elements), visible, nodeType, precedence);
    }

    @NelumboConstructor
    public NodeTypePattern(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    @Override
    protected NodeTypePattern set(NodeInfo nodeInfo, Object[] args) {
        return new NodeTypePattern(nodeInfo, args);
    }

    public Type nodeType() {
        return (Type) get(1);
    }

    @Override
    public Variable variable() {
        return nodeType().variable();
    }

    @Override
    public NodeTypePattern set(int i, Object... a) {
        return (NodeTypePattern) super.set(i, a);
    }

    @Override
    protected NodeTypePattern setBinding(Node declaration, Map<Variable, Object> vars, boolean setFunctorOrType) {
        Variable var = variable();
        if (var != null && vars.get(var) instanceof Type type) {
            return set(1, nodeType().rewrite(type));
        }
        return (NodeTypePattern) super.setBinding(declaration, vars, setFunctorOrType);
    }

    public Integer precedence() {
        return (Integer) get(2);
    }

    public Boolean visible() {
        return (Boolean) get(0);
    }

    @Override
    public ParseState state(ParseState next) {
        Boolean visible = visible();
        return new ParseState(nodeType(), precedence(), visible != null ? next.setVisibility(visible) : next);
    }

    @Override
    public String toString(TokenType[] previous) {
        return "<" + nodeType() + ">";
    }

    @Override
    public Pattern setPresedence(int precedence) {
        if (precedence() != null) {
            return this;
        }
        return set(2, precedence);
    }

    @Override
    public Pattern setTypes(Function<Type, Type> typeFunction) {
        return set(1, typeFunction.apply(nodeType()));
    }

    @Override
    public List<Type> argTypes(List<Type> types) {
        return types.add(nodeType());
    }

    @Override
    public Pattern setIsConnected() {
        return this;
    }

    @Override
    public int string(List<Object> args, int ai, StringBuffer sb, TokenType[] previous, boolean alt) {
        if (ai < 0 || args.size() <= ai) {
            return -1;
        }
        if (args.get(ai) instanceof Node node && (//
        nodeType().isAssignableFrom(node instanceof Type type ? type : node.type()) || //
                (node instanceof Variable && nodeType().isAssignableFrom(Type.VARIABLE)))) {
            boolean parenthetical = false;
            Functor functor = node.functor();
            ParseState post = functor != null ? functor.postStart() : null;
            if (post != null) {
                Integer inner = precedence();
                parenthetical = inner != null && inner > post.leftPrecedence();
            }
            if (parenthetical) {
                sb.append('(');
            }
            sb.append(node.toString(previous));
            if (parenthetical) {
                sb.append(')');
            }
            return ai + 1;
        }
        return -1;
    }

    @Override
    public int args(List<AstElement> elements, int i, MutableList<Object> args, boolean alt, Functor functor,
            Map<Variable, Type> typeArgs) {
        if (i < elements.size()) {
            AstElement e = elements.get(i);
            if (e instanceof Node n) {
                Type type = nodeType();
                if (type.isAssignableFrom(n.type())) {
                    args.add(n);
                    return i + 1;
                } else if (Type.VARIABLE.equals(type) && n instanceof Variable) {
                    args.add(n);
                    return i + 1;
                } else {
                    Variable var = type.variable();
                    type = var != null ? typeArgs.get(var) : null;
                    if (type != null && type.isAssignableFrom(n.type())) {
                        args.add(n);
                        return i + 1;
                    }
                }
            }
        }
        return -1;
    }

    @Override
    public Pattern declaration(Token token) {
        return null;
    }

    @Override
    public Node init(KnowledgeBase knowledgeBase, ParseContext ctx, ConstructionReason reason) throws ParseException {
        if (reason == ConstructionReason.parsing) {
            List<AstElement> elements = astElements();
            Boolean visible = null;
            String v = getVal(0);
            if (v != null) {
                visible = v.equals("visible");
            }
            Type type = (Type) get(1);
            Integer precedence = null;
            String p = getVal(2);
            if (p != null) {
                precedence = Integer.parseInt(p);
            }
            TokenType tt = type.tokenType();
            return tt != null ? t(elements, tt) : n(elements, type, precedence, visible);
        }
        return this;
    }
}
