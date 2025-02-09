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

package org.modelingvalue.nelumbo.impl;

import org.modelingvalue.nelumbo.Lists;
import org.modelingvalue.nelumbo.Lists.ListCons;
import org.modelingvalue.nelumbo.Logic.Functor;
import org.modelingvalue.nelumbo.Logic.Structure;

public final class ListImpl<E extends Structure> extends StructureImpl<ListCons<E>> {
    private static final long                  serialVersionUID     = -916406585584150604L;

    @SuppressWarnings("rawtypes")
    private static final FunctorImpl<ListCons> LIST_FUNCTOR_0       = FunctorImpl.<ListCons> of(Lists::l);

    @SuppressWarnings("unchecked")
    public static final ListImpl<?>            EMPTY_LIST           = new ListImpl<>();
    public static final ListCons<?>            EMPTY_LIST_PROXY     = ListImpl.EMPTY_LIST.proxy();

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final FunctorImpl<ListCons> LIST_FUNCTOR_2       = FunctorImpl.<ListCons, Structure, ListCons> of(Lists::l);
    @SuppressWarnings("rawtypes")
    private static final Functor<ListCons>     LIST_FUNCTOR_2_PROXY = ListImpl.LIST_FUNCTOR_2.proxy();

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <E extends Structure> ListImpl<E> of(org.modelingvalue.collections.List<StructureImpl<E>> es) {
        ListImpl<E> l = (ListImpl) EMPTY_LIST;
        for (StructureImpl<E> e : es.reverse()) {
            l = of(e, l);
        }
        l.list = es;
        return l;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <E extends Structure> ListImpl<E> of(StructureImpl<E> head, ListImpl<E> tail) {
        return new ListImpl(LIST_FUNCTOR_2, head, tail);
    }

    private org.modelingvalue.collections.List<StructureImpl<E>> list;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public ListImpl() {
        super((FunctorImpl) LIST_FUNCTOR_0);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public ListImpl(E head, ListCons<E> tail) {
        super((Functor) LIST_FUNCTOR_2_PROXY, head, tail);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public ListImpl(FunctorImpl functor, StructureImpl<E> head, ListImpl<E> tail) {
        super(functor, head, tail);
    }

    private ListImpl(Object[] args, ListImpl<E> declaration) {
        super(args, declaration);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected ListImpl<E> struct(Object[] array) {
        return new ListImpl(array, declaration());
    }

    @Override
    public ListImpl<E> declaration() {
        return (ListImpl<E>) super.declaration();
    }

    @SuppressWarnings("unchecked")
    public StructureImpl<E> head() {
        return (StructureImpl<E>) get(1);
    }

    @SuppressWarnings("unchecked")
    public ListImpl<E> tail() {
        return (ListImpl<E>) get(2);
    }

    @Override
    public String toString() {
        return list().toString().substring(4);
    }

    public org.modelingvalue.collections.List<StructureImpl<E>> list() {
        if (list == null) {
            list = length() == 3 ? tail().list().prepend(head()) : org.modelingvalue.collections.List.of();
        }
        return list;
    }

    @Override
    public ListImpl<E> set(int i, Object... a) {
        return (ListImpl<E>) super.set(i, a);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Class<ListCons<E>> type() {
        return (Class) ListCons.class;
    }
}
