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

package org.modelingvalue.nelumbo.collections;

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.InferContext;
import org.modelingvalue.nelumbo.InferResult;
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Variable;
import org.modelingvalue.nelumbo.logic.Quantifier;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.syntax.TokenType;

public final class SetBuilder extends Quantifier {
    @Serial
    private static final long serialVersionUID = 331700694148320063L;

    @NelumboConstructor
    public SetBuilder(Functor functor, List<AstElement> elements, Object[] args) {
        super(functor, elements, args);
    }

    private SetBuilder(Object[] array, List<AstElement> elements, SetBuilder declaration) {
        super(array, elements, declaration);
    }

    @Override
    public final List<Variable> localVars() {
        return List.of((Variable) get(0));
    }

    @Override
    protected SetBuilder struct(Object[] array, List<AstElement> elements, Node declaration) {
        return new SetBuilder(array, elements, (SetBuilder) declaration);
    }

    @Override
    public SetBuilder set(int i, Object... a) {
        return (SetBuilder) super.set(i, a);
    }

    @Override
    public String toString(TokenType[] previous) {
        return "{[" + variable() + "]" + predicate() + "}";
    }

    @Override
    protected InferResult resolve(InferContext context, InferResult predResult) {
        // TODO Auto-generated method stub
        return null;
    }

}
