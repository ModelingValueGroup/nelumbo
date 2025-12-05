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
import java.util.function.Function;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.Variable;
import org.modelingvalue.nelumbo.syntax.ParseState;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class VariablePattern extends Pattern {
    @Serial
    private static final long serialVersionUID = 2405616043878166113L;

    public VariablePattern(Type type, List<AstElement> elements, Object... args) {
        super(type, elements, args);
    }

    protected VariablePattern(Object[] args, VariablePattern declaration) {
        super(args, declaration);
    }

    @Override
    protected VariablePattern struct(Object[] array, Node declaration) {
        return new VariablePattern(array, (VariablePattern) declaration);
    }

    public Variable variable() {
        return (Variable) get(0);
    }

    @Override
    public String toString(TokenType[] previous) {
        Variable var = variable();
        return var.name();
    }

    @Override
    public ParseState state(ParseState next, NodeTypePattern left, Functor functor, List<Integer> branche) {
        Type type = variable().type();
        TokenType tokenType = type.tokenType();
        if (tokenType != null) {
            return t(tokenType).state(next, left, functor, branche);
        } else {
            return n(type, null).state(next, left, functor, branche);
        }
    }

    @Override
    public List<Type> argTypes(List<Type> types) {
        return types.add(variable().type());
    }

    @Override
    public Pattern setTypes(Function<Type, Type> typeFunction) {
        Variable var = variable();
        return set(0, new Variable(List.of(), typeFunction.apply(var.type()), var.name()));
    }

    @Override
    protected List<Object> args(List<Object> args, ElementIterator it, List<Integer> branche, boolean alt) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected int string(List<Object> args, int ai, StringBuffer sb, TokenType[] previous, boolean alt) {
        throw new UnsupportedOperationException();
    }

}
