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

package org.modelingvalue.nelumbo;

import org.modelingvalue.collections.Map;

public final class Boolean extends Predicate {
    private static final long serialVersionUID = -8515171118744898263L;

    public static Boolean     TRUE;
    public static Boolean     FALSE;

    private InferResult       result;

    public Boolean(Functor functor, Object[] args) {
        super(functor, java.lang.Boolean.valueOf((String) args[0]));
        if (TRUE == null && isTrue()) {
            TRUE = this;
        } else if (FALSE == null && !isTrue()) {
            FALSE = this;
        }
    }

    private Boolean(Object[] args, Boolean declaration) {
        super(args, declaration);
    }

    @Override
    protected void init(Predicate parent, int idx) {
    }

    public boolean isTrue() {
        return (java.lang.Boolean) get(1);
    }

    public InferResult result() {
        return infer(null);
    }

    @Override
    protected Boolean struct(Object[] array, Predicate declaration) {
        return new Boolean(array, (Boolean) declaration);
    }

    @Override
    protected InferResult infer(InferContext context) {
        if (result == null) {
            result = isTrue() ? factCC() : falsehoodCC();
        }
        return result;
    }

    @Override
    public Map<Variable, Object> getBinding() {
        return Map.of();
    }

    @Override
    public Boolean set(int i, Object... a) {
        return (Boolean) super.set(i, a);
    }

}
