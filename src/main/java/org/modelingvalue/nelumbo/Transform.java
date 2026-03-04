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

package org.modelingvalue.nelumbo;

import java.io.Serial;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.collections.NList;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.patterns.Pattern;
import org.modelingvalue.nelumbo.patterns.TokenTextPattern;
import org.modelingvalue.nelumbo.syntax.ParseContext;
import org.modelingvalue.nelumbo.syntax.ParseException;

public final class Transform extends Node {
    @Serial
    private static final long serialVersionUID = -5542746717620873208L;

    public Transform(Functor functor, List<AstElement> elements, Object... args) throws ParseException {
        super(functor, elements, args);
    }

    private Transform(Object[] array, List<AstElement> elements, Transform declaration) {
        super(array, elements, declaration);
    }

    @Override
    protected Transform struct(Object[] array, List<AstElement> elements, Node declaration) {
        return new Transform(array, elements, (Transform) declaration);
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

    public List<Node> targetsFlattened() {
        List<Node> result = List.of();
        for (Node e : targets()) {
            if (e instanceof NList nl) {
                result = result.addAll(nl.elements());
            } else {
                result = result.add(e);
            }
        }
        return result;
    }

    public Set<Functor> literals() {
        Set<Functor> literals = Set.of();
        for (Node target : targetsFlattened()) {
            if (target instanceof Functor functor && functor.pattern() instanceof TokenTextPattern) {
                literals = literals.add(functor);
            }
        }
        return literals;
    }

    @Override
    public Node init(KnowledgeBase knowledgeBase, ParseContext ctx) throws ParseException {
        knowledgeBase.addTransform(this);
        return this;
    }

    public Node transform(Node start, Node node, Node result, KnowledgeBase knowledgeBase, ParseContext ctx) throws ParseException {
        Map<Variable, Object> binding = node.getBinding(start);
        if (binding == null) {
            return result;
        }
        Map<Functor, Functor> functors = Map.of();
        for (Node target : targetsFlattened()) {
            if (target instanceof Functor functor && !Type.VARIABLE.isAssignableFrom(functor.resultType()) && !functor.pattern().equals(start)) {
                Functor rewrite = functor.setBinding(binding).resetDeclaration();
                for (Entry<Functor, Functor> e : functors) {
                    if (functor.equals(knowledgeBase.literal(e.getKey()))) {
                        knowledgeBase.addLiteral(e.getValue(), rewrite);
                        break;
                    }
                }
                functors = functors.put(functor, rewrite);
                rewrite.init(knowledgeBase, ctx);
                result = add(result, rewrite);
            }
        }
        if (start instanceof Pattern) {
            return result;
        }
        Map<Functor, Functor> fm = functors;
        for (Node target : targetsFlattened()) {
            if (!(target instanceof Functor)) {
                Node rewrite = target.replace(n -> {
                    if (n.typeOrFunctor() instanceof Functor f) {
                        Functor r = fm.get(f);
                        if (r != null) {
                            return n.setFunctor(r);
                        }
                    }
                    return n;
                }).setBinding(binding).setAstElements(node.astElements()).resetDeclaration();
                rewrite.init(knowledgeBase, ctx);
                result = add(result, rewrite);
            }
        }
        return result;
    }

    private static Node add(Node result, Node rewrite) {
        if (result instanceof NList list) {
            return new NList(List.of(), list, rewrite);
        } else if (result != null) {
            return new NList(Type.ROOT, List.of(result, rewrite), List.of(result, rewrite));
        }
        return null;
    }

}
