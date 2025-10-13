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

package org.modelingvalue.nelumbo;

import java.io.Serial;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.nelumbo.patterns.Functor;

public final class Variable extends Node {
    @Serial
    private static final long serialVersionUID = -8998368070388908726L;

    public Variable(List<AstElement> elements, Type type, String name) {
        super(Type.VARIABLE, elements, type, name);
    }

    private Variable(Object[] array) {
        super(array);
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
        return type.isLiteral() ? this : new Variable(astElements(), type.literal(), name());
    }

    public Variable rename(String name) {
        return new Variable(astElements(), type(), name);
    }

    @Override
    public Type type() {
        return (Type) get(0);
    }

    public String name() {
        return (String) get(1);
    }

    @Override
    public String toString() {
        return name();
    }

    @Override
    protected Variable struct(Object[] array) {
        return new Variable(array);
    }

    @Override
    public Variable set(int i, Object... a) {
        return (Variable) super.set(i, a);
    }

    @Override
    public final Map<Variable, Object> variables() {
        return Map.of(Entry.of(this, type()));
    }

}
