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

package org.modelingvalue.nelumbo.lang;

import java.io.Serial;
import java.util.function.Function;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.util.Pair;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.ConstructionReason;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.NodeInfo;
import org.modelingvalue.nelumbo.collections.NList;
import org.modelingvalue.nelumbo.logic.BooleanVariable;
import org.modelingvalue.nelumbo.syntax.ParseContext;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;

public final class Variable extends Node {
    @Serial
    private static final long serialVersionUID = -8998368070388908726L;

    @NelumboConstructor
    public Variable(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    public Variable(List<AstElement> elements, boolean hidden, Type type, String name) {
        super(NodeInfo.of(Type.VARIABLE, elements), hidden, type, name);
    }

    public Variable(List<AstElement> elements, boolean hidden, Type type, String name, Object id) {
        super(NodeInfo.of(Type.VARIABLE, elements), hidden, type, Pair.of(name, id));
    }

    @Override
    protected Variable set(NodeInfo nodeInfo, Object[] args) {
        return new Variable(nodeInfo, args);
    }

    @Override
    public Variable setFunctorOrType(FunctorOrType functorOrType) {
        return (Variable) super.setFunctorOrType(functorOrType);
    }

    @Override
    public Variable setAstElements(List<AstElement> elements) {
        return (Variable) super.setAstElements(elements);
    }

    public Variable makeUnique(int id) {
        return set(2, Pair.of(baseName(), id));
    }

    public Variable literal() {
        Type type = type();
        Object n = get(2);
        return type.isLiteral() ? this
                : n instanceof Pair p ? new Variable(astElements(), hidden(), type.toLiteral(), (String) p.a(), p.b())
                        : new Variable(astElements(), hidden(), type.toLiteral(), (String) n);
    }

    public Variable rename(Function<String, String> rename) {
        Object n = get(2);
        return n instanceof Pair p ? new Variable(astElements(), hidden(), type(), rename.apply((String) p.a()), p.b())
                : new Variable(astElements(), hidden(), type(), rename.apply((String) n));
    }

    public boolean hidden() {
        return (Boolean) get(0);
    }

    @Override
    public Type type() {
        return length() > 0 ? (Type) get(1) : super.type();
    }

    private String baseName() {
        Object n = get(2);
        return n instanceof Pair p ? (String) p.a() : (String) n;
    }

    public String name() {
        Object n = get(2);
        return n instanceof Pair p ? ((String) p.a()) + p.b() : (String) n;
    }

    @Override
    public Variable set(int i, Object... a) {
        return (Variable) super.set(i, a);
    }

    @Override
    public String toString(TokenType[] previous) {
        if (previous[0] == TokenType.NAME || previous[0] == TokenType.NUMBER) {
            previous[0] = TokenType.NAME;
            return " " + name();
        }
        previous[0] = TokenType.NAME;
        return name();
    }

    @Override
    protected Object typeForEquals() {
        return Type.VARIABLE;
    }

    @Override
    public Variable setType(Type type) {
        return set(1, type);
    }

    @Override
    public Variable variable() {
        return this;
    }

    @Override
    public Node init(KnowledgeBase knowledgeBase, ParseContext ctx, ConstructionReason reason) throws ParseException {
        if (reason == ConstructionReason.parsing) {
            if (length() > 0 && get(0) instanceof Boolean) {
                return this;
            }
            if (super.functorOrType() instanceof Functor functor
                    && functor.astElements().first() instanceof Variable var) {
                if (Type.BOOLEAN.isAssignableFrom(var.type())) {
                    return new BooleanVariable(functor, astElements(), var);
                } else {
                    return var.setAstElements(astElements()).setFunctorOrType(functor);
                }
            }
            boolean hidden = get(0) != null;
            List<AstElement> elements = astElements();
            Type type = (Type) get(1);
            NList roots = new NList(List.of(type), Type.ROOT);
            int start = hidden ? 1 : 0;
            for (int i = start + 1; i < elements.size(); i++) {
                AstElement e = elements.get(i);
                if (e instanceof Token t && t.text().equals(",")) {
                    roots = roots.setAstElements(roots.astElements().add(t));
                    e = elements.get(++i);
                }
                Variable var = new Variable(List.of(e), hidden, type, ((Token) e).text());
                Functor varFun = knowledgeBase.addVariable(var, ctx);
                roots = new NList(List.of(), roots, varFun);
            }
            return roots;
        }
        return this;
    }

}
