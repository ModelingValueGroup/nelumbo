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

package org.modelingvalue.logic.impl;

import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.logic.Logic;
import org.modelingvalue.logic.Logic.Predicate;

public final class TrueImpl extends PredicateImpl {
    private static final long                   serialVersionUID = -8515171118744898263L;

    private static final FunctorImpl<Predicate> TRUE_FUNCTOR     = FunctorImpl.<Predicate> of(Logic::T);

    public static final TrueImpl                TRUE             = new TrueImpl();

    private static final InferResult             TRUE_CONCLUSION  = InferResult.of(Set.of(TRUE), Set.of());

    private TrueImpl() {
        super(TRUE_FUNCTOR);
    }

    private TrueImpl(Object[] args) {
        super(args);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Predicate proxy() {
        return Logic.T();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected TrueImpl struct(Object[] array) {
        return new TrueImpl(array);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public InferResult infer(PredicateImpl declaration, InferContext context) {
        return TRUE_CONCLUSION;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Map<VariableImpl, Object> getBinding(StructureImpl<Predicate> pred, Map<VariableImpl, Object> vars) {
        return vars;
    }

    @Override
    public TrueImpl set(int i, Object... a) {
        return (TrueImpl) super.set(i, a);
    }
}
