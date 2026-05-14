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

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.ConstructionReason;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.Node;
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
    public Variable(Functor functor, List<AstElement> elements, Object[] args) {
        super(functor, elements, init(args));
    }

    private static Object[] init(Object[] args) {
        if (args.length > 0) {
            Object hidden = args[0];
            Object type = args[1];
            args[0] = type;
            args[1] = hidden;
        }
        return args;
    }

    public Variable(List<AstElement> elements, Type type, String name, boolean hidden) {
        super(Type.VARIABLE, elements, type, name, hidden);
    }

    private Variable(Object[] array, Node functorOrType, List<AstElement> elements, Variable declaration) {
        super(array, Type.VARIABLE, elements, declaration);
    }

    @Override
    public Variable setFunctor(Functor functor) {
        return (Variable) super.setFunctor(functor);
    }

    @Override
    public Variable setAstElements(List<AstElement> elements) {
        return (Variable) super.setAstElements(elements);
    }

    public Variable literal() {
        Type type = type();
        return type.isLiteral() ? this : new Variable(astElements(), type.toLiteral(), name(), hidden());
    }

    public Variable rename(String name) {
        return new Variable(astElements(), type(), name, hidden());
    }

    @Override
    public Type type() {
        return length() > 0 ? (Type) get(0) : super.type();
    }

    public String name() {
        return (String) get(1);
    }

    public boolean hidden() {
        return (Boolean) get(2);
    }

    @Override
    protected Variable struct(Object[] array, Node functorOrType, List<AstElement> elements, Node declaration) {
        return new Variable(array, functorOrType, elements, (Variable) declaration);
    }

    @Override
    public Variable set(int i, Object... a) {
        return (Variable) super.set(i, a);
    }

    @Override
    public String toString(TokenType[] previous) {
        if (previous[0] == TokenType.NAME || previous[0] == TokenType.NUMBER || previous[0] == TokenType.DECIMAL) {
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

    public Variable setType(Type type) {
        return set(0, type);
    }

    @Override
    public Variable variable() {
        return this;
    }

    @Override
    public Node init(KnowledgeBase knowledgeBase, ParseContext ctx, ConstructionReason reason) throws ParseException {
        if (reason == ConstructionReason.parsing) {
            if (length() > 0 && get(2) instanceof Boolean) {
                return this;
            }
            if (super.functorOrType() instanceof Functor functor
                    && functor.astElements().first() instanceof Variable var) {
                if (Type.BOOLEAN.isAssignableFrom(var.type())) {
                    return new BooleanVariable(functor, astElements(), var);
                } else {
                    return var.setAstElements(astElements()).setFunctor(functor);
                }
            }
            boolean hidden = get(1) != null;
            List<AstElement> elements = astElements();
            Type type = (Type) get(0);
            NList roots = new NList(List.of(type), Type.ROOT);
            int start = hidden ? 1 : 0;
            for (int i = start + 1; i < elements.size(); i++) {
                AstElement e = elements.get(i);
                if (e instanceof Token t && t.text().equals(",")) {
                    roots = roots.setAstElements(roots.astElements().add(t));
                    e = elements.get(++i);
                }
                Variable var = new Variable(List.of(e), type, ((Token) e).text(), hidden);
                Functor varFun = knowledgeBase.addVariable(var, ctx);
                roots = new NList(List.of(), roots, varFun);
            }
            return roots;
        }
        return this;
    }

}
