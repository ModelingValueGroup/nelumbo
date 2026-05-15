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

package org.modelingvalue.nelumbo.strings;

import java.io.Serial;
import java.math.BigInteger;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.lang.FunctorOrType;
import org.modelingvalue.nelumbo.logic.InferContext;
import org.modelingvalue.nelumbo.logic.InferResult;
import org.modelingvalue.nelumbo.logic.Predicate;

public final class ToInteger extends Predicate {
    @Serial
    private static final long serialVersionUID = -2874326869672600959L;

    @NelumboConstructor
    public ToInteger(FunctorOrType functorOrType, List<AstElement> elements, Node declaration, Object... args) {
        super(functorOrType, elements, declaration, args);
    }

    @Override
    protected ToInteger set(FunctorOrType functorOrType, List<AstElement> elements, Node declaration, Object[] args) {
        return new ToInteger(functorOrType, elements, declaration, args);
    }

    @Override
    protected InferResult infer(int nrOfUnbound, InferContext context) {
        if (nrOfUnbound > 1) {
            return unresolvable();
        }

        BigInteger integer = getVal(0, 0);
        String string = getVal(1, 0);
        if (string != null) {
            try {
                BigInteger parsed = BigInteger.valueOf(Integer.parseInt(string));
                if (integer != null) {
                    boolean eq = integer.equals(parsed);
                    return eq ? factCC() : falsehoodCC();
                } else {
                    return set(0, org.modelingvalue.nelumbo.integers.NInteger.of(parsed)).factCI();
                }
            } catch (NumberFormatException e) {
                return integer != null ? falsehoodCC() : falsehoodCI();
            }
        } else if (integer != null) {
            String s = integer.toString();
            return set(1, org.modelingvalue.nelumbo.strings.NString.of(s)).factCI();
        }

        return unknown();
    }

}
