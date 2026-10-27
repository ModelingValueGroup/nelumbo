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

package org.modelingvalue.nelumbo.logic;

import static org.modelingvalue.nelumbo.patterns.Pattern.n;
import static org.modelingvalue.nelumbo.patterns.Pattern.s;
import static org.modelingvalue.nelumbo.patterns.Pattern.t;

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.ConstructionReason;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.NodeInfo;
import org.modelingvalue.nelumbo.lang.Functor;
import org.modelingvalue.nelumbo.lang.Type;
import org.modelingvalue.nelumbo.lang.Variable;
import org.modelingvalue.nelumbo.patterns.Pattern;
import org.modelingvalue.nelumbo.syntax.ParseContext;
import org.modelingvalue.nelumbo.syntax.ParseException;

public final class Lambda extends Node {
    @Serial
    private static final long serialVersionUID = -8085779803830595557L;

    private static Functor[] FUNCTOR;

    static {
        try {
            FUNCTOR = new Functor[Functor.REPETITION_MAX];
            for (int i = 1; i <= Functor.REPETITION_MAX; i++) {
                Pattern[] vars = new Pattern[i * 2 - 1];
                for (int ii = 0; ii < vars.length; ii += 2) {
                    if (ii > 1) {
                        vars[ii - 1] = t(",");
                    }
                    vars[ii] = n(Type.VARIABLE, 100);
                }
                FUNCTOR[i - 1] = Functor.of(s(t("["), s(vars), t("]"), t("("), n(Type.OBJECT, 0), t(")")), Type.LAMBDA,
                        null, Lambda.class, null);
            }
        } catch (ParseException e) {
            throw new IllegalStateException("Cannot create functor for " + Lambda.class, e);
        }
    }

    private Variable var;
    private NIs      is;

    @NelumboConstructor
    public Lambda(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    public Lambda(List<AstElement> elements, List<Variable> localVars, Predicate predicate) {
        super(NodeInfo.of(FUNCTOR[localVars.size() - 1], elements), localVars, predicate);
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
            is = new NIs(List.of(), expression(), var());
        }
        return is;
    }

    protected Variable var() {
        if (var == null) {
            Type t = type().arguments().last().toLiteral();
            var = new Variable(List.of(), false, t, "r$" + uniqueId());
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
    public List<Object> args() {
        List<Object> localVars = getVal(0);
        return localVars.add(expression());
    }

    @Override
    public Node init(KnowledgeBase knowledgeBase, ParseContext ctx, ConstructionReason reason) throws ParseException {
        List<Object> args = super.args();
        return set(0, args.removeLast(), args.last());
    }

    private Object setVariables(Object... vals) {
        Map<Variable, Object> binding = Map.of();
        List<Variable> localVars = localVars();
        for (int i = 0; i < vals.length; i++) {
            binding = binding.add(localVars.get(i), vals[i]);
        }
        Node exp = expression();
        if (exp instanceof Variable var) {
            Object val = binding.get(var);
            if (val != null) {
                return val;
            }
        }
        return exp.setBinding(exp, binding);
    }

    public boolean test(Object... vals) {
        Predicate p = (Predicate) setVariables(vals);
        InferResult result = resolve(p, true);
        return result != null && result.isTrueCC();
    }

    @SuppressWarnings("unchecked")
    public <R> R apply(Object... vals) {
        Predicate p = is().set(0, setVariables(vals));
        InferResult result = resolve(p, false);
        Predicate fact = result != null && result.isTrueCI() ? result.facts().findFirst().orElse(null) : null;
        return fact != null ? (R) fact.getBinding(result.predicate()).get(var()) : null;
    }

    private static InferResult resolve(Predicate p, boolean bool) {
        InferContext ctx = CURRENT_CONTEXT.get();
        InferResult incomplete = ctx.incompleteResult().get();
        if (incomplete != null && incomplete.hasStackOverflow()) {
            // Keep the first overflow path: flatten re-runs only that path, so a second
            // overflow would leave the first path's cycle placeholders in the memoization.
            return null;
        }
        InferResult result = p.resolve(ctx);
        if (result.hasStackOverflow()) {
            ctx.incompleteResult().set(result);
        } else if (!isComplete(result, bool)) {
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
        return ctx.hasIncompleteResult() ? null : result;
    }

    private static boolean isComplete(InferResult result, boolean bool) {
        return bool ? (result.isTrueCC() || result.isFalseCC()) : result.isTrueCI();
    }

}
