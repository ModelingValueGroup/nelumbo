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

package org.modelingvalue.nelumbo.lang;

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.ConstructionReason;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.NodeInfo;
import org.modelingvalue.nelumbo.collections.NList;
import org.modelingvalue.nelumbo.patterns.Pattern;
import org.modelingvalue.nelumbo.patterns.TokenTextPattern;
import org.modelingvalue.nelumbo.syntax.ParseContext;
import org.modelingvalue.nelumbo.syntax.ParseException;

public final class Transform extends Node {
    @Serial
    private static final long serialVersionUID = -5542746717620873208L;

    @NelumboConstructor
    public Transform(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    @Override
    protected Transform set(NodeInfo nodeInfo, Object[] args) {
        return new Transform(nodeInfo, args);
    }

    @Override
    public Transform set(int i, Object... a) {
        return (Transform) super.set(i, a);
    }

    public Node source() {
        return (Node) get(0);
    }

    public List<Node> targets() {
        return ((NList) get(1)).<Node>collection();
    }

    public List<Node> targetsFlattened() {
        List<Node> result = List.of();
        for (Node e : targets()) {
            if (e instanceof NList nl) {
                result = result.addAll(nl.collection());
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
    public Transform makeVariablesUnique(ParseContext ctx) throws ParseException {
        return (Transform) super.makeVariablesUnique(ctx);
    }

    @Override
    public Node init(KnowledgeBase knowledgeBase, ParseContext ctx, ConstructionReason reason) throws ParseException {
        Transform to = makeVariablesUnique(ctx);
        to = rewireFunctors(to, knowledgeBase);
        return knowledgeBase.addTransform(to);
    }

    @Override
    public Transform setTypeArgs(Map<Variable, Type> typeArgs) {
        Transform to = (Transform) super.setTypeArgs(typeArgs);
        try {
            return rewireFunctors(to, KnowledgeBase.CURRENT.get());
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Node transform(Node node, Node result, KnowledgeBase knowledgeBase, ParseContext ctx) throws ParseException {
        Node source = source();
        Map<Variable, Object> binding = node.getBinding(source);
        if (binding == null) {
            return result;
        }
        Map<Functor, Functor> functors = Map.of();
        List<Node> targets = targetsFlattened();
        for (Node target : targets) {
            if (target instanceof Functor functor && !Type.VARIABLE.isAssignableFrom(functor.resultType())
                    && !functor.pattern().equals(source)) {
                Functor rewrite = functor.setBinding(functor, binding).makeVariablesUnique(ctx).resetOriginal();
                functors = functors.put(functor, rewrite);
                rewrite.init(knowledgeBase, ctx, ConstructionReason.transforming);
                result = add(result, rewrite);
            }
        }
        if (source instanceof Pattern) {
            return result;
        }
        Map<Functor, Functor> fm = functors;
        for (Node target : targets) {
            if (!(target instanceof Functor)) {
                Node rewrite = target.replace(o -> {
                    if (o instanceof Node n) {
                        if (n.functorOrType() instanceof Functor f) {
                            Functor r = fm.get(f);
                            if (r != null) {
                                return n.setFunctorOrType(r);
                            }
                        }
                        return n;
                    }
                    return o;
                }).setBinding(target, binding).setAstElements(node.astElements()).makeVariablesUnique(ctx);
                rewrite.init(knowledgeBase, ctx, ConstructionReason.transforming);
                result = add(result, rewrite);
            }
        }
        return result;
    }

    private static Node add(Node result, Node rewrite) {
        return result != null ? result.add(rewrite) : null;
    }

    private Transform rewireFunctors(Transform to, KnowledgeBase knowledgeBase) throws ParseException {
        List<Node> ftl = targetsFlattened();
        List<Node> ttl = to.targetsFlattened();
        return (Transform) to.replace(o -> {
            if (o instanceof Node n) {
                if (n.functorOrType() instanceof Functor f) {
                    int i = ftl.index(f);
                    if (i >= 0) {
                        return n.setFunctorOrType((Functor) ttl.get(i));
                    }
                }
                return n;
            }
            return o;
        });
    }

}
