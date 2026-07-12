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

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.ConstructionReason;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.NelumboFunctorField;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.NodeInfo;
import org.modelingvalue.nelumbo.logic.InferContext;
import org.modelingvalue.nelumbo.logic.InferResult;
import org.modelingvalue.nelumbo.logic.NIs;
import org.modelingvalue.nelumbo.logic.Predicate;
import org.modelingvalue.nelumbo.syntax.ParseContext;
import org.modelingvalue.nelumbo.syntax.ParseException;

public final class Lambda extends Node {
    @Serial
    private static final long serialVersionUID = -8085779803830595557L;

    @NelumboFunctorField
    private static Functor FUNCTOR;

    private Variable var;
    private NIs      is;

    @NelumboConstructor
    public Lambda(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    public Lambda(List<AstElement> elements, List<Variable> localVars, Predicate predicate) {
        super(NodeInfo.of(FUNCTOR, elements), localVars, predicate);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Variable> localVars() {
        return (List<Variable>) get(0);
    }

    public Node expression() {
        return (Node) get(1);
    }

    protected NIs is() {
        if (is == null) {
            Variable r = var();
            is = new NIs(List.of(), expression(), r).setBinding(Map.of(Entry.of(r, r.type())));
        }
        return is;
    }

    protected Variable var() {
        if (var == null) {
            Type t = type().arguments().last().toLiteral();
            var = new Variable(List.of(), false, t, "$r");
        }
        return var;
    }

    @Override
    protected boolean doGetBinding(Object varVal, int i) {
        return i > 0 || varVal instanceof Variable;
    }

    @Override
    protected boolean doSetBinding(Object varVal, int i) {
        return i > 0 || varVal instanceof Variable;
    }

    @Override
    public Node init(KnowledgeBase knowledgeBase, ParseContext ctx, ConstructionReason reason) throws ParseException {
        List<Object> args = args();
        return set(0, args.removeLast(), args.last());
    }

    private Node setVariables(Object... vals) {
        Map<Variable, Object> binding = Map.of();
        List<Variable> localVars = localVars();
        for (int i = 0; i < vals.length; i++) {
            binding = binding.add(localVars.get(i), vals[i]);
        }
        return expression().setBinding(binding);
    }

    public boolean test(Object... vals) {
        Predicate p = (Predicate) setVariables(vals);
        return resolve(p, true).isTrueCC();
    }

    @SuppressWarnings("unchecked")
    public <R> R apply(Object... vals) {
        Predicate p = is().set(0, setVariables(vals));
        InferResult result = resolve(p, false);
        Predicate fact = result != null && result.isTrueCI() ? result.facts().findFirst().orElse(null) : null;
        return fact != null ? (R) fact.getBinding().get(var()) : null;
    }

    private static InferResult resolve(Predicate p, boolean bool) {
        InferContext ctx = CURRENT_CONTEXT.get();
        InferResult result = p.resolve(ctx);
        if (result.hasStackOverflow()) {
            ctx.incompleteResult().set(result);
        }
        if (!isComplete(result, bool)) {
            ctx.incompleteResult().accumulateAndGet(result, (a, b) -> {
                if (a == null) {
                    return b;
                } else if (a.hasStackOverflow()) {
                    return a;
                } else {
                    return a.add(b);
                }
            });
        }
        return result;
    }

    private static boolean isComplete(InferResult result, boolean bool) {
        return bool ? (result.isTrueCC() || result.isFalseCC()) : result.isTrueCI();
    }

}
