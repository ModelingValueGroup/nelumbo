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

package org.modelingvalue.logic;

import static org.modelingvalue.logic.Logic.pred;

import org.modelingvalue.collections.Set;
import org.modelingvalue.logic.Logic.Constant;
import org.modelingvalue.logic.Logic.Function;
import org.modelingvalue.logic.Logic.Functor;
import org.modelingvalue.logic.Logic.LogicLambda;
import org.modelingvalue.logic.Logic.Relation;
import org.modelingvalue.logic.Logic.Structure;
import org.modelingvalue.logic.impl.FunctorImpl;
import org.modelingvalue.logic.impl.InferContext;
import org.modelingvalue.logic.impl.InferResult;
import org.modelingvalue.logic.impl.ListImpl;
import org.modelingvalue.logic.impl.PredicateImpl;
import org.modelingvalue.logic.impl.StructureImpl;

public final class Lists {

    private Lists() {
    }

    public interface List<E extends Structure> extends Structure {
    }

    public interface ListCons<E extends Structure> extends List<E>, Constant<List<E>> {
    }

    public interface ListFunc<E extends Structure> extends List<E>, Function<List<E>> {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <E extends Structure> ListCons<E> l() {
        return (ListCons) ListImpl.EMPTY_LIST_PROXY;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <E extends Structure> ListCons<E> l(E head, ListCons<E> tail) {
        return new ListImpl<E>(head, tail).proxy();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <E extends Structure> ListCons<E> l(E... elements) {
        return l(org.modelingvalue.collections.List.of(elements));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <E extends Structure> ListCons<E> l(org.modelingvalue.collections.List<E> elements) {
        ListImpl<E> l = (ListImpl) ListImpl.EMPTY_LIST;
        for (E e : elements.reverse()) {
            l = ListImpl.of(StructureImpl.<E, StructureImpl<E>> unproxy(e), l);
        }
        return l.proxy();
    }

    // Add

    private static <E extends Structure> org.modelingvalue.collections.List<StructureImpl<E>> addOrdered(org.modelingvalue.collections.List<StructureImpl<E>> l, StructureImpl<E> e) {
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).compareTo(e) > 0) {
                return l.insert(i, e);
            }
        }
        return l.append(e);
    }

    private static <E extends Structure> Set<org.modelingvalue.collections.List<StructureImpl<E>>> permRemove(org.modelingvalue.collections.List<StructureImpl<E>> l, StructureImpl<E> e) {
        Set<org.modelingvalue.collections.List<StructureImpl<E>>> ls = Set.of();
        for (int i = l.firstIndexOf(e); i >= 0; i = l.firstIndexOf(i, l.size(), e)) {
            ls = ls.add(l.removeIndex(i));
        }
        return ls;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final FunctorImpl<Relation> ADD_FUNCTOR = FunctorImpl.<Relation, Structure, ListCons, ListCons> of(Lists::add, (LogicLambda) Lists::addLogic);

    private static InferResult addLogic(PredicateImpl predicate, InferContext context) {
        StructureImpl<Structure> element = predicate.getVal(1);
        ListImpl<Structure> subListImpl = predicate.getVal(2);
        ListImpl<Structure> superListImpl = predicate.getVal(3);
        org.modelingvalue.collections.List<StructureImpl<Structure>> sublist = subListImpl != null ? subListImpl.list() : null;
        org.modelingvalue.collections.List<StructureImpl<Structure>> superlist = superListImpl != null ? superListImpl.list() : null;
        if (element != null && sublist != null && superlist != null) {
            boolean eq = addOrdered(sublist, element).equals(superlist);
            return InferResult.trueFalse(eq ? predicate.singleton() : Set.of(), eq ? Set.of() : predicate.singleton());
        } else if (element != null && sublist != null && superlist == null) {
            return InferResult.trueFalse(predicate.set(3, ListImpl.of(addOrdered(sublist, element))).singleton(), predicate.singleton());
        } else if (element != null && sublist == null && superlist != null) {
            return InferResult.trueFalse(permRemove(superlist, element).replaceAll(l -> predicate.set(2, ListImpl.of(l))), predicate.singleton());
        } else if (element == null && sublist != null && superlist != null) {
            if (sublist.anyMatch(superlist::notContains)) {
                return InferResult.trueFalse(Set.of(), predicate.singleton());
            } else {
                return InferResult.trueFalse(superlist.asSet().removeAll(sublist).replaceAll(r -> predicate.set(1, r)), predicate.singleton());
            }
        } else {
            return InferResult.trueFalse(predicate.singleton(), predicate.singleton());
        }
    }

    @SuppressWarnings("rawtypes")
    private static final Functor<Relation> ADD_FUNCTOR_PROXY = ADD_FUNCTOR.proxy();

    public static <E extends Structure> Relation add(E e, ListCons<E> i, ListCons<E> o) {
        return pred(ADD_FUNCTOR_PROXY, e, i, o);
    }

}
