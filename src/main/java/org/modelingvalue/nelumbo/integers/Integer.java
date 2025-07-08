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

package org.modelingvalue.nelumbo.integers;

import java.math.BigInteger;

import org.modelingvalue.nelumbo.Functor;
import org.modelingvalue.nelumbo.Terminal;

public final class Integer extends Terminal {

    private static final BigInteger MIN              = BigInteger.valueOf(Long.MIN_VALUE);
    private static final BigInteger MAX              = BigInteger.valueOf(Long.MAX_VALUE);

    private static final long       serialVersionUID = 2454372545442550574L;

    private static Functor          FUNCTOR;

    public Integer(Functor functor, Object[] args) {
        super(functor, parse((String) args[0]));
        if (FUNCTOR == null) {
            FUNCTOR = functor;
        }
    }

    private Integer(Functor functor, BigInteger val) {
        super(functor, val);
    }

    public static Integer of(BigInteger val) {
        return new Integer(FUNCTOR, val);
    }

    private static BigInteger parse(String string) {
        return BigInteger.valueOf(Long.parseLong(string));
    }

    private Integer(Object[] array) {
        super(array);
    }

    @Override
    protected Integer struct(Object[] array) {
        return new Integer(array);
    }

    @Override
    public Integer set(int i, Object... a) {
        return (Integer) super.set(i, a);
    }

    public BigInteger value() {
        return (BigInteger) get(1);
    }

    @Override
    public String toString() {
        BigInteger value = value();
        return value.compareTo(MAX) > 0 || value.compareTo(MIN) < 0 ? (value.toString(Character.MAX_RADIX) + "#" + Character.MAX_RADIX) : value.toString();
    }

}
