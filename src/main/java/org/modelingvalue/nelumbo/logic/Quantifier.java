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
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.lang.Functor;
import org.modelingvalue.nelumbo.lang.Type;
import org.modelingvalue.nelumbo.lang.Variable;

public abstract class Quantifier extends CompoundPredicate {

    @Serial
    private static final long serialVersionUID = -4838100281214165385L;

    protected Quantifier(Functor functor, List<AstElement> elements, Node declaration, Object... args) {
        super(functor, elements, declaration, args);
    }

    protected Quantifier(Object[] args, Node functorOrType, List<AstElement> elements, Quantifier declaration) {
        super(args, functorOrType, elements, declaration);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Variable> localVars() {
        return (List<Variable>) get(0);
    }

    public final Predicate predicate() {
        return predicate(1);
    }

    @Override
    protected int countNrOfUnbound() {
        return (int) getBinding().removeAllKey(localVars()).filter(e -> e.getValue() instanceof Type).count();
    }

    @Override
    protected boolean doGetBinding(Object varVal, int i) {
        return i > 0;
    }

    @Override
    protected boolean doSetBinding(Object varVal, int i) {
        return i > 0 || varVal instanceof Variable;
    }

    @Override
    protected InferResult infer(int nrOfUnbound, InferContext context) {
        return context.shallow() ? unresolvable() : resolve(context.toDeep());
    }

    @Override
    public final InferResult resolve(InferContext context) {
        Predicate predicate = predicate();
        InferResult predResult = predicate.resolve(context);
        if (predResult.hasStackOverflow()) {
            return predResult;
        }
        return resolve(context, predResult);
    }

    protected abstract InferResult resolve(InferContext context, InferResult predResult);

}
