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

import org.modelingvalue.nelumbo.ConstructionReason;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.NelumboFunctorField;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.NodeInfo;
import org.modelingvalue.nelumbo.lang.Functor;
import org.modelingvalue.nelumbo.syntax.ParseContext;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.TokenType;

public final class NInteger extends Node {
    @Serial
    private static final long serialVersionUID = 2454372545442550574L;

    private static final BigInteger MIN = BigInteger.valueOf(Long.MIN_VALUE);
    private static final BigInteger MAX = BigInteger.valueOf(Long.MAX_VALUE);

    @NelumboFunctorField
    private static Functor FUNCTOR;

    public static NInteger of(BigInteger val) {
        return new NInteger(NodeInfo.of(FUNCTOR), val);
    }

    @NelumboConstructor
    public NInteger(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    private static BigInteger parse(String string) {
        int i = string.indexOf('#');
        if (i > 0) {
            int radix = Integer.parseInt(string.substring(0, i));
            return new BigInteger(string.substring(i + 1), radix);
        }
        return new BigInteger(string);
    }

    @Override
    public String toString(TokenType[] previous) {
        BigInteger value = getVal(0);
        String string = value.compareTo(MAX) > 0 || value.compareTo(MIN) < 0
                ? (Character.MAX_RADIX + "#" + value.toString(Character.MAX_RADIX))
                : value.toString();
        if (previous[0] == TokenType.NAME || previous[0] == TokenType.NUMBER) {
            previous[0] = TokenType.NUMBER;
            return " " + string;
        }
        previous[0] = TokenType.NUMBER;
        return string;
    }

    @Override
    public Node init(KnowledgeBase knowledgeBase, ParseContext ctx, ConstructionReason reason) throws ParseException {
        if (reason == ConstructionReason.parsing && length() == 2 && get(1) instanceof String string) {
            if ("-".equals(get(0))) {
                string = "-" + string;
            }
            BigInteger val = parse(string);
            return setArgs(new Object[] { val });
        }
        return this;
    }

}
