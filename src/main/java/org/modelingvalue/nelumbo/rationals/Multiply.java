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

package org.modelingvalue.nelumbo.rationals;

import java.io.Serial;
import java.math.BigInteger;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.InferContext;
import org.modelingvalue.nelumbo.InferResult;
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.logic.Predicate;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.syntax.ParseContext;

public final class Multiply extends Predicate {
    @Serial
    private static final long serialVersionUID = 6350761148609377565L;

    @NelumboConstructor
    public Multiply(Functor functor, List<AstElement> elements, ParseContext ctx, Object[] args) {
        super(functor, elements, args[0], args[1], args[2]);
    }

    private Multiply(Object[] array, List<AstElement> elements, Multiply declaration) {
        super(array, elements, declaration);
    }

    @Override
    protected Multiply struct(Object[] array, List<AstElement> elements, Node declaration) {
        return new Multiply(array, elements, (Multiply) declaration);
    }

    @Override
    protected InferResult infer(int nrOfUnbound, InferContext context) {
        if (nrOfUnbound > 1) {
            return unresolvable();
        }

        BigInteger f1n = getVal(0, 0);
        BigInteger f1d = getVal(0, 1);
        BigInteger f2n = getVal(1, 0);
        BigInteger f2d = getVal(1, 1);
        BigInteger prn = getVal(2, 0);
        BigInteger prd = getVal(2, 1);
        if (f1n != null && f2n != null) {
            BigInteger pn = f1n.multiply(f2n);
            BigInteger pd = f1d.multiply(f2d);
            Rational p = Rational.of(pn, pd);
            if (prn != null) {
                boolean eq = p.equals(get(2));
                return eq ? factCC() : falsehoodCC();
            } else {
                return set(2, p).factCI();
            }
        } else if (f1n != null) {
            BigInteger dn = prn.multiply(f1d);
            BigInteger dd = prd.multiply(f1n);
            return set(1, Rational.of(dn, dd)).factCI();
        } else if (f2n != null) {
            BigInteger dn = prn.multiply(f2d);
            BigInteger dd = prd.multiply(f2n);
            return set(0, Rational.of(dn, dd)).factCI();
        }

        return unknown();
    }

}
