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

package org.modelingvalue.nelumbo.strings;

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.InferContext;
import org.modelingvalue.nelumbo.InferResult;
import org.modelingvalue.nelumbo.Predicate;
import org.modelingvalue.nelumbo.patterns.Functor;

public final class Concat extends Predicate {
    @Serial
    private static final long serialVersionUID = -317279750710781401L;

    public Concat(Functor functor, List<AstElement> elements, Object[] args) {
        super(functor, elements, args[0], args[1], args[2]);
    }

    private Concat(Object[] array, Concat declaration) {
        super(array, declaration);
    }

    @Override
    protected Concat struct(Object[] array, Predicate declaration) {
        return new Concat(array, (Concat) declaration);
    }

    @Override
    protected InferResult infer(int nrOfUnbound, InferContext context) {
        if (nrOfUnbound > 1) {
            return unknown();
        }
        java.lang.String addend1 = getVal(0, 0);
        java.lang.String addend2 = getVal(1, 0);
        java.lang.String sum = getVal(2, 0);
        if (addend1 != null && addend2 != null) {
            java.lang.String s = addend1 + addend2;
            if (sum != null) {
                boolean eq = s.equals(sum);
                return eq ? factCC() : falsehoodCC();
            } else {
                return set(2, org.modelingvalue.nelumbo.strings.String.of(s)).factCI();
            }
        } else if (addend1 != null && sum != null) {
            if (sum.startsWith(addend1)) {
                return set(1, org.modelingvalue.nelumbo.strings.String.of(sum.substring(addend1.length()))).factCI();
            } else {
                return falsehoodCI();
            }
        } else if (addend2 != null && sum != null) {
            if (sum.endsWith(addend2)) {
                return set(0, org.modelingvalue.nelumbo.strings.String.of(sum.substring(0, addend2.length()))).factCI();
            } else {
                return falsehoodCI();
            }
        } else {
            return unknown();
        }
    }

}
