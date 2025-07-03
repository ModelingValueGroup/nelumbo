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

import java.util.function.Function;

import org.modelingvalue.collections.List;

public final class Functor extends Node {
    private static final long            serialVersionUID = 285147889847599160L;
    public static final Type             TYPE             = new Type(Functor.class, Type.ROOT);

    private final Function<Node, String> render;

    public Functor(Type resultType, String oper, Type... args) {
        super(TYPE, resultType, oper, List.of(args));
        this.render = null;
        KnowledgeBase.CURRENT.get().addFunctor(this);
    }

    public Functor(Type resultType, String oper, Function<Node, String> render, int precedence, Type... args) {
        super(TYPE, resultType, oper, List.of(args), precedence);
        this.render = render;
        KnowledgeBase.CURRENT.get().addFunctor(this);
    }

    public Functor(Type resultType, String name, List<Type> args) {
        super(TYPE, resultType, name, args);
        this.render = null;
        KnowledgeBase.CURRENT.get().addFunctor(this);
    }

    private Functor(Object[] array, Function<Node, String> render) {
        super(array);
        this.render = render;
    }

    public Type resultType() {
        return (Type) get(1);
    }

    @Override
    public String toString() {
        Function<Node, String> render = render();
        if (render != null) {
            return render.apply(new Node(this, args().toArray()));
        }
        String types = args().toString();
        return name() + "(" + types.substring(5, types.length() - 1) + ")";
    }

    public String name() {
        return ((String) get(2));
    }

    @SuppressWarnings("unchecked")
    public List<Type> args() {
        return (List<Type>) get(3);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Function<Node, String> render() {
        return render;
    }

    @Override
    public int precedence() {
        return (Integer) get(4);
    }

    @Override
    protected Functor struct(Object[] array) {
        return new Functor(array, render);
    }

    @Override
    public Functor set(int i, Object... a) {
        return (Functor) super.set(i, a);
    }

}
