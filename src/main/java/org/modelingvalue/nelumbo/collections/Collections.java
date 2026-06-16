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

package org.modelingvalue.nelumbo.collections;

import java.io.Serial;
import java.math.BigInteger;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.NelumboMethod;
import org.modelingvalue.nelumbo.NodeInfo;
import org.modelingvalue.nelumbo.integers.NInteger;
import org.modelingvalue.nelumbo.logic.InferResult;
import org.modelingvalue.nelumbo.logic.Predicate;

public class Collections extends Predicate {
    @Serial
    private static final long serialVersionUID = -2609193511212262794L;

    @NelumboConstructor
    public Collections(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    @NelumboMethod
    protected InferResult size(NCollection collection, NInteger size) {
        if (collection == null && size == null) {
            return unresolvable();
        }
        if (collection == null) {
            return unknown();
        }
        BigInteger found = BigInteger.valueOf(collection.size());
        if (size == null) {
            return set(1, NInteger.of(found)).factCI();
        }
        return size.value().equals(found) ? factCC() : falsehoodCC();
    }

    @NelumboMethod
    protected InferResult indexOf(NList list, Object element, NInteger index) {
        if (list == null && element == null && index == null) {
            return unresolvable();
        }
        if (list == null) {
            return unknown();
        }
        List<?> coll = list.collection();
        if (index != null) {
            Object e = coll.get(index.value().intValue());
            if (element != null) {
                return element.equals(e) ? factCC() : falsehoodCC();
            }
            return e != null ? set(1, e).factCI() : falsehoodCI();
        }
        if (element != null) {
            Set<NInteger> indexes = Set.of();
            for (int i : coll.indexesOf(element)) {
                indexes = indexes.add(NInteger.of(BigInteger.valueOf(i)));
            }
            Set<Predicate> facts = Set.of();
            for (NInteger i : indexes) {
                facts = facts.add(set(2, i));
            }
            return InferResult.factsCI(this, facts);
        }
        return unknown();
    }

    @NelumboMethod
    protected InferResult elementOf(NSet set, Object element) {
        if (set == null && element == null) {
            return unresolvable();
        }
        if (set == null) {
            return unknown();
        }
        Set<?> coll = set.collection();
        if (element != null) {
            return coll.contains(element) ? factCC() : falsehoodCC();
        }
        Set<Predicate> facts = Set.of();
        for (Object e : coll) {
            facts = facts.add(set(1, e));
        }
        return InferResult.factsCI(this, facts);
    }

}
