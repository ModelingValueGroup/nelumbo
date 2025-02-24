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

package org.modelingvalue.nelumbo.impl;

import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.util.SerializableFunction;
import org.modelingvalue.nelumbo.Logic;
import org.modelingvalue.nelumbo.Logic.Relation;

public final class BooleanImpl extends PredicateImpl {
    private static final long          serialVersionUID = -8515171118744898263L;

    @SuppressWarnings("rawtypes")
    private static final FunctorImpl   BOOLEAN_FUNCTOR  = FunctorImpl.of((SerializableFunction<Boolean, Relation>) BooleanImpl::b);

    public static final BooleanImpl    TRUE             = new BooleanImpl(true);
    public static final BooleanImpl    FALSE            = new BooleanImpl(false);

    protected static final InferResult TRUE_CONCLUSION  = TRUE.fact();
    protected static final InferResult FALSE_CONCLUSION = FALSE.falsehood();

    private static Relation b(boolean val) {
        return val ? Logic.T() : Logic.F();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private BooleanImpl(boolean val) {
        super(BOOLEAN_FUNCTOR, val);
    }

    private BooleanImpl(Object[] args, BooleanImpl declaration) {
        super(args, declaration);
    }

    @Override
    public BooleanImpl declaration() {
        return (BooleanImpl) super.declaration();
    }

    public boolean isTrue() {
        return (Boolean) get(1);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Relation proxy() {
        return b(isTrue());
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected BooleanImpl struct(Object[] array) {
        return new BooleanImpl(array, declaration());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    protected InferResult infer(InferContext context) {
        return isTrue() ? TRUE_CONCLUSION : FALSE_CONCLUSION;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Map<VariableImpl, Object> getBinding() {
        return Map.of();
    }

    @Override
    public BooleanImpl set(int i, Object... a) {
        return (BooleanImpl) super.set(i, a);
    }

    @Override
    public String toString() {
        return PRETTY_NELUMBO ? (isTrue() ? "\u22A4" : "\u22A5") : super.toString();
    }

}
