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

import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.NelumboMethod;
import org.modelingvalue.nelumbo.NodeInfo;
import org.modelingvalue.nelumbo.integers.NInteger;
import org.modelingvalue.nelumbo.logic.InferResult;
import org.modelingvalue.nelumbo.logic.Predicate;

public final class Strings extends Predicate {
    @Serial
    private static final long serialVersionUID = -317279750710781401L;

    @NelumboConstructor
    public Strings(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    @NelumboMethod
    protected InferResult string_concat(NString addend1, NString addend2, NString sum) {
        if (nrOfUnbound() > 1) {
            return unresolvable();
        }
        String a1 = addend1 == null ? null : addend1.value();
        String a2 = addend2 == null ? null : addend2.value();
        String s  = sum     == null ? null : sum.value();
        if (a1 != null && a2 != null) {
            String r = a1 + a2;
            if (s != null) {
                return r.equals(s) ? factCC() : falsehoodCC();
            }
            return set(2, NString.of(r)).factCI();
        } else if (a1 != null && s != null) {
            return s.startsWith(a1) ? set(1, NString.of(s.substring(a1.length()))).factCI() : falsehoodCI();
        } else if (a2 != null && s != null) {
            return s.endsWith(a2) ? set(0, NString.of(s.substring(0, a2.length()))).factCI() : falsehoodCI();
        }
        return unknown();
    }

    @NelumboMethod
    protected InferResult string_length(NString string, NInteger length) {
        if (nrOfUnbound() > 1) {
            return unresolvable();
        }
        if (string != null) {
            BigInteger actual = BigInteger.valueOf(string.value().length());
            if (length != null) {
                return length.value().equals(actual) ? factCC() : falsehoodCC();
            }
            return set(1, NInteger.of(actual)).factCI();
        }
        return unknown();
    }

    @NelumboMethod
    protected InferResult integer_string(NInteger integer, NString string) {
        if (nrOfUnbound() > 1) {
            return unresolvable();
        }
        if (string != null) {
            try {
                BigInteger parsed = BigInteger.valueOf(Integer.parseInt(string.value()));
                if (integer != null) {
                    return integer.value().equals(parsed) ? factCC() : falsehoodCC();
                }
                return set(0, NInteger.of(parsed)).factCI();
            } catch (NumberFormatException e) {
                return integer != null ? falsehoodCC() : falsehoodCI();
            }
        } else if (integer != null) {
            return set(1, NString.of(integer.value().toString())).factCI();
        }
        return unknown();
    }

}
