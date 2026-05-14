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
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.NelumboFunctorField;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.lang.Functor;
import org.modelingvalue.nelumbo.syntax.TokenType;

public final class Rational extends Node {
    @Serial
    private static final long serialVersionUID = 5534246508330776916L;

    private static final BigInteger HUNDERD = BigInteger.valueOf(100);

    @NelumboFunctorField
    private static Functor FUNCTOR;

    @NelumboConstructor
    public Rational(Functor functor, List<AstElement> elements, Object[] args) {
        super(functor, elements, parse((String) args[0]));
    }

    private Rational(Functor functor, List<AstElement> elements, BigInteger numerator, BigInteger denominator) {
        super(functor, elements, normalize(numerator, denominator));
    }

    public static Rational of(BigInteger numerator, BigInteger denominator) {
        return new Rational(FUNCTOR, List.of(), numerator, denominator);
    }

    private static Object[] parse(String string) {
        while (string.charAt(string.length() - 1) == '0') {
            string = string.substring(0, string.length() - 1);
        }
        int i = string.indexOf('.');
        int dec = string.length() - i - 1;
        string = string.substring(0, i) + string.substring(i + 1);
        return normalize(new BigInteger(string), BigInteger.TEN.pow(dec));
    }

    private static Object[] normalize(BigInteger numerator, BigInteger denominator) {
        BigInteger gcd = numerator.gcd(denominator);
        return new Object[] { numerator.divide(gcd), denominator.divide(gcd) };
    }

    private Rational(Object[] array, Node functorOrType, List<AstElement> elements, Rational declaration) {
        super(array, functorOrType, elements, declaration);
    }

    @Override
    protected Rational struct(Object[] array, Node functorOrType, List<AstElement> elements, Node declaration) {
        return new Rational(array, functorOrType, elements, (Rational) declaration);
    }

    @Override
    public Rational set(int i, Object... a) {
        return (Rational) super.set(i, a);
    }

    public BigInteger numerator() {
        return (BigInteger) get(0);
    }

    public BigInteger denominator() {
        return (BigInteger) get(1);
    }

    @Override
    public String toString(TokenType[] previous) {
        BigInteger num = numerator();
        BigInteger den = denominator();
        String string = num.multiply(HUNDERD).divide(den).toString();
        if (string.length() > 2) {
            string = string.substring(0, string.length() - 2) + "." + string.substring(string.length() - 2);
        } else {
            string = "0." + string;
        }
        if (previous[0] == TokenType.NAME || previous[0] == TokenType.NUMBER || previous[0] == TokenType.DECIMAL) {
            previous[0] = TokenType.NUMBER;
            return " " + string;
        }
        previous[0] = TokenType.DECIMAL;
        return string;
    }

}
