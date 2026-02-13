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

package org.modelingvalue.nelumbo.logic;

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.InferContext;
import org.modelingvalue.nelumbo.InferResult;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.Variable;
import org.modelingvalue.nelumbo.patterns.Functor;

public class BooleanVariable extends Predicate {

    @Serial
    private static final long serialVersionUID = 5130317339420169185L;

    private InferResult       result;

    public BooleanVariable(Functor functor, List<AstElement> elements, Variable var) {
        super(functor, elements, var);
    }

    @Override
    public List<Object> args() {
        return List.of(variable());
    }

    private BooleanVariable(Object[] args, BooleanVariable declaration) {
        super(args, declaration);
    }

    @Override
    protected BooleanVariable struct(Object[] array, Node declaration) {
        return new BooleanVariable(array, (BooleanVariable) declaration);
    }

    @Override
    public BooleanVariable set(int i, Object... a) {
        return (BooleanVariable) super.set(i, a);
    }

    @Override
    public BooleanVariable setAstElements(List<AstElement> elements) {
        return (BooleanVariable) super.setAstElements(elements);
    }

    @Override
    protected BooleanVariable setBinding(Node declaration, Map<Variable, Object> vars) {
        Variable var = variable();
        if (var != null && vars.get(var) instanceof Type t && !t.equals(functor().resultType())) {
            return (BooleanVariable) super.setBinding(declaration, vars).setFunctor(functor().setResultType(t));
        }
        if (var != null && vars.get(var) instanceof Variable v && !v.type().equals(functor().resultType())) {
            return (BooleanVariable) super.setBinding(declaration, vars).setFunctor(functor().setResultType(v.type()));
        }
        return (BooleanVariable) super.setBinding(declaration, vars);
    }

    @Override
    public InferResult resolve(InferContext context) {
        if (get(0) instanceof Predicate pred) {
            return pred.resolve(context);
        }
        return infer(context);
    }

    @Override
    protected InferResult infer(InferContext context) {
        if (get(0) instanceof Predicate pred) {
            return pred.infer(context);
        }
        if (context != null && context.shallow()) {
            return unresolvable();
        }
        if (result == null) {
            result = InferResult.of(declaration(), Set.of(set(0, NBoolean.TRUE)), true, //
                    Set.of(set(0, NBoolean.FALSE)), true, Set.of());
        }
        return result;
    }

    @Override
    public Variable variable() {
        if (get(0) instanceof Variable var) {
            return var;
        }
        return null;
    }

}
