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

package org.modelingvalue.nelumbo;

import org.modelingvalue.collections.List;

public final class Type extends Structure {
    private static final long serialVersionUID = -4583279157841144493L;
    private static final Type TYPE             = new Type(Type.class);

    public Type(Class<?> clss) {
        super(TYPE, clss);
    }

    public Type(String name, List<Type> supers) {
        super(TYPE, name, supers);
    }

    private Type(Object[] array) {
        super(array);
    }

    @Override
    protected Type type() {
        return TYPE;
    }

    @Override
    public String toString() {
        Object type = get(1);
        return "<" + (type instanceof Class ? ((Class<?>) type).getSimpleName() : (String) type) + ">";
    }

    @SuppressWarnings("unchecked")
    public List<Type> supers() {
        return (List<Type>) get(2);
    }

    @Override
    protected Type struct(Object[] array) {
        return new Type(array);
    }

    @Override
    public Type set(int i, Object... a) {
        return (Type) super.set(i, a);
    }

    public boolean isAssignableFrom(Type type) {
        return equals(type) || supers().anyMatch(s -> s.isAssignableFrom(type));
    }

    public boolean isAssignableFrom(Class<?> type) {
        Object clss = get(1);
        return clss instanceof Class && ((Class<?>) clss).isAssignableFrom(type);
    }

}
