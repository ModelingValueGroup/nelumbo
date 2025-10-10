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
import java.math.BigInteger;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.InferContext;
import org.modelingvalue.nelumbo.InferResult;
import org.modelingvalue.nelumbo.Predicate;
import org.modelingvalue.nelumbo.patterns.Functor;

public final class Length extends Predicate {
    @Serial
    private static final long serialVersionUID = 4405805306602130025L;

    public Length(Functor functor, List<AstElement> elements, Object[] args) {
        super(functor, elements, args[0], args[1]);
    }

    private Length(Object[] array, Length declaration) {
        super(array, declaration);
    }

    @Override
    protected Length struct(Object[] array, Predicate declaration) {
        return new Length(array, (Length) declaration);
    }

    @Override
    protected InferResult infer(int nrOfUnbound, InferContext context) {
        if (nrOfUnbound > 1) {
            return unknown();
        }

        java.lang.String string = getVal(0, 0);
        BigInteger length = getVal(1, 0);
        if (string != null) {
            BigInteger actual = BigInteger.valueOf(string.length());
            if (length != null) {
                boolean eq = length.equals(actual);
                return eq ? factCC() : falsehoodCC();
            } else {
                return set(1, org.modelingvalue.nelumbo.integers.Integer.of(actual)).factCI();
            }
        }

        return unknown();
    }

}
