//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2025 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
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

package org.modelingvalue.nelumbo;

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.syntax.ParseException;

public final class Transform extends Node {
    @Serial
    private static final long serialVersionUID = -5542746717620873208L;

    public Transform(Functor functor, List<AstElement> elements, Object... args) throws ParseException {
        super(functor, elements, args);
    }

    private Transform(Object[] array, Transform declaration) {
        super(array, declaration);
    }

    @Override
    protected Transform struct(Object[] array, Node declaration) {
        return new Transform(array, (Transform) declaration);
    }

    @Override
    public Transform set(int i, Object... a) {
        return (Transform) super.set(i, a);
    }

    public Node source() {
        return (Node) get(0);
    }

    @SuppressWarnings("unchecked")
    public List<Node> targets() {
        return (List<Node>) get(1);
    }

    @Override
    public Node init(KnowledgeBase knowledgeBase) throws ParseException {
        knowledgeBase.addTransform(this);
        return this;
    }

    public void rewrite(Node node, KnowledgeBase knowledgeBase) throws ParseException {
        Node source = source();
        Map<Variable, Object> binding = node.getBinding(source);
        if (binding == null) {
            return;
        }
        Map<Functor, Functor> functors = Map.of();
        for (Node target : targets()) {
            if (target instanceof Functor functor && !Type.VARIABLE.isAssignableFrom(functor.resultType())) {
                Functor rewrite = functor.setBinding(binding).resetDeclaration();
                functors = functors.put(functor, rewrite);
                rewrite.init(knowledgeBase);
            }
        }
        Map<Functor, Functor> fm = functors;
        for (Node target : targets()) {
            if (!(target instanceof Functor)) {
                Node rewrite = target.replace(n -> {
                    if (n.typeOrFunctor() instanceof Functor f) {
                        Functor r = fm.get(f);
                        if (r != null) {
                            return n.setFunctor(r);
                        }
                    }
                    return n;
                }).setBinding(binding).resetDeclaration();
                rewrite.init(knowledgeBase);
            }
        }
    }

}
