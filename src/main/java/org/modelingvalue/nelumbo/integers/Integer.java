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

package org.modelingvalue.nelumbo.integers;

import java.io.Serial;
import java.math.BigInteger;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Terminal;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.syntax.TokenType;

public final class Integer extends Terminal {
    @Serial
    private static final long       serialVersionUID = 2454372545442550574L;

    private static final BigInteger MIN              = BigInteger.valueOf(Long.MIN_VALUE);
    private static final BigInteger MAX              = BigInteger.valueOf(Long.MAX_VALUE);

    private static Functor          FUNCTOR;

    static {
        KnowledgeBase.registerFunctorSetter(Integer.class, f -> FUNCTOR = f);
    }

    public Integer(Functor functor, List<AstElement> elements, Object[] args) {
        super(functor, elements, parse((String) args[0]));
    }

    private Integer(Functor functor, List<AstElement> elements, BigInteger val) {
        super(functor, elements, val);
    }

    public static Integer of(BigInteger val) {
        return new Integer(FUNCTOR, List.of(), val);
    }

    private static BigInteger parse(String string) {
        int i = string.indexOf('#');
        if (i > 0) {
            int radix = java.lang.Integer.parseInt(string.substring(0, i));
            return new BigInteger(string.substring(i + 1), radix);
        }
        return new BigInteger(string);
    }

    private Integer(Object[] array, Integer declaration) {
        super(array, declaration);
    }

    @Override
    protected Integer struct(Object[] array, Node declaration) {
        return new Integer(array, (Integer) declaration);
    }

    @Override
    public Integer set(int i, Object... a) {
        return (Integer) super.set(i, a);
    }

    public BigInteger value() {
        return (BigInteger) get(0);
    }

    @Override
    public String toString(TokenType[] previous) {
        BigInteger value = value();
        String string = value.compareTo(MAX) > 0 || value.compareTo(MIN) < 0 ? (Character.MAX_RADIX + "#" + value.toString(Character.MAX_RADIX)) : value.toString();
        if (previous[0] == TokenType.NAME || previous[0] == TokenType.NUMBER || previous[0] == TokenType.DECIMAL) {
            previous[0] = TokenType.NUMBER;
            return " " + string;
        }
        previous[0] = TokenType.NUMBER;
        return string;
    }

}
