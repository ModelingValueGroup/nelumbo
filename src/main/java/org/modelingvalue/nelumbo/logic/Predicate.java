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

import java.io.Serial;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.mutable.MutableMap;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.NelumboTimeoutException;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.NodeInfo;
import org.modelingvalue.nelumbo.lang.Functor;
import org.modelingvalue.nelumbo.lang.FunctorOrType;
import org.modelingvalue.nelumbo.lang.Type;
import org.modelingvalue.nelumbo.lang.Variable;
import org.modelingvalue.nelumbo.syntax.ParseContext;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class Predicate extends Node {
    @Serial
    private static final long serialVersionUID = -1605559565948158856L;

    protected static final boolean RANDOM_NELUMBO  = Boolean.getBoolean("RANDOM_NELUMBO");
    protected static final boolean REVERSE_NELUMBO = Boolean.getBoolean("REVERSE_NELUMBO");
    protected static final int     MAX_LOGIC_DEPTH = Integer.getInteger("MAX_LOGIC_DEPTH", 64);

    private static final int MAX_LOGIC_DEPTH_D2 = MAX_LOGIC_DEPTH / 2;

    public static Node INCOMPLETE = new Predicate(NodeInfo.of(Type.BOOLEAN), "..");

    private int           nrOfUnbound    = -1;
    private Set<Variable> localVariables = null;

    @NelumboConstructor
    public Predicate(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    @Override
    public Predicate declaration() {
        return (Predicate) super.declaration();
    }

    protected final int nrOfUnbound() {
        if (nrOfUnbound < 0) {
            nrOfUnbound = (int) getBinding().removeAllKey(allLocalVars()).filter(e -> e.getValue() instanceof Type)
                    .count();
        }
        return nrOfUnbound;
    }

    @Override
    public Set<Variable> allLocalVars() {
        if (localVariables == null) {
            localVariables = super.allLocalVars();
        }
        return localVariables;
    }

    public final boolean isFullyBound() {
        return nrOfUnbound() == 0;
    }

    public Predicate castFrom(Predicate from) {
        return set(nodeInfo(), from.toArray());
    }

    @Override
    public String toString(TokenType[] previous) {
        return setVariables().superToString(previous);
    }

    @Override
    public Predicate setVariables() {
        return (Predicate) super.setVariables();
    }

    @Override
    public Predicate setTypes() {
        return (Predicate) super.setTypes();
    }

    private String superToString(TokenType[] previous) {
        return super.toString(previous);
    }

    public static Map<Variable, Object> literals(Map<Variable, Object> vars) {
        return vars.replaceAll(e -> Entry.of(e.getKey(), e.getKey().literal()));
    }

    protected static Map<Variable, Object> literals(Map<Variable, Object> vars, Function<String, String> rename) {
        return vars.replaceAll(e -> Entry.of(e.getKey(), e.getKey().literal().rename(rename)));
    }

    public Predicate setVariables(KnowledgeBase kb, Map<Variable, Object> vars, ParseContext ctx)
            throws ParseException {
        Predicate predicate = setBinding(vars);
        predicate = (Predicate) predicate.replace(n -> {
            Functor functor = n.functor();
            if (functor != null) {
                Functor lit = kb.literal(functor);
                if (lit == null) {
                    lit = kb.literal(functor.declaration());
                }
                if (lit != null) {
                    List<Object> args = n.args();
                    if (args.allMatch(a -> a instanceof Node node && node.type().isLiteral())) {
                        return lit.construct(n.astElements(), args.toArray(), kb, ctx);
                    }
                }
            }
            return n;
        });
        return predicate.resetDeclaration();
    }

    @Override
    public Predicate resetDeclaration() {
        return (Predicate) super.resetDeclaration();
    }

    @Override
    public Predicate setBinding(Map<Variable, Object> vars) {
        return (Predicate) super.setBinding(vars);
    }

    @Override
    public Predicate set(Variable var, Object val) {
        return (Predicate) super.set(var, val);
    }

    @Override
    protected Predicate set(NodeInfo nodeInfo, Object[] args) {
        if (getClass() == Predicate.class) {
            return new Predicate(nodeInfo, args);
        } else {
            return (Predicate) super.set(nodeInfo, args);
        }
    }

    public Type getType(int i) {
        Object v = get(i);
        return v instanceof Type ? (Type) v : v instanceof Node ? ((Node) v).type() : null;
    }

    public InferResult infer() {
        KnowledgeBase knowledgeBase = KnowledgeBase.CURRENT.get();
        InferContext context = knowledgeBase.context();
        Predicate predicate = setTypes();
        if (context.trace()) {
            System.out.println(context.prefix() + predicate);
        }
        InferResult result = predicate.resolve(context);
        if (context.trace()) {
            System.out.println(context.prefix() + predicate + " " + result);
        }
        return result;
    }

    @Override
    public Predicate setFunctorOrType(FunctorOrType functorOrType) {
        return (Predicate) super.setFunctorOrType(functorOrType);
    }

    @Override
    public Predicate setArgs(Object... args) {
        return (Predicate) super.setArgs(args);
    }

    @Override
    public Predicate set(int i, Object... a) {
        return (Predicate) super.set(i, a);
    }

    protected Predicate replace(Predicate from, Predicate to) {
        if (equals(from)) {
            return to;
        } else {
            Predicate decl = declaration();
            Object[] array = null;
            for (int i = 0; i < length(); i++) {
                Object thisVal = get(i);
                if (thisVal instanceof Predicate fromDecl) {
                    Predicate toDecl = fromDecl.replace(from, to);
                    if (toDecl != fromDecl) {
                        decl = decl.setPredicates(i, toDecl.declaration());
                        if (array == null) {
                            array = toArray();
                        }
                        array[i] = toDecl;
                    }
                }
            }
            return array != null ? setArgs(array) : this;
        }
    }

    @Override
    public Predicate set(int[] idx, Object val) {
        return (Predicate) super.set(idx, val);
    }

    public Predicate copy(int from, int to) {
        return (Predicate) super.set(to, get(from));
    }

    protected final Predicate setPredicates(int from, Predicate... a) {
        Object[] declArray = declaration().toArray();
        int i = from;
        for (int x = 0; x < a.length; x++) {
            declArray[i + x] = a[x].declaration();
        }
        Predicate newDeclaration = declaration().setArgs(declArray);
        Object[] predArray = toArray();
        System.arraycopy(a, 0, predArray, from, a.length);
        return set(nodeInfo().setDeclaration(newDeclaration), predArray);
    }

    public final InferResult unknown() {
        return InferResult.unknown(this, Set.of());
    }

    public final InferResult unknown(Set<Predicate> cycles) {
        return InferResult.unknown(this, cycles);
    }

    public final InferResult factCC() {
        return InferResult.factsCC(this, singleton());
    }

    public final InferResult falsehoodCC() {
        return InferResult.falsehoodsCC(this, singleton());
    }

    public final InferResult factCI() {
        return InferResult.factsCI(this, singleton());
    }

    public final InferResult falsehoodIC() {
        return InferResult.falsehoodsIC(this, singleton());
    }

    public final InferResult factIC() {
        return InferResult.factsIC(this, singleton());
    }

    public final InferResult falsehoodCI() {
        return InferResult.falsehoodCI(this);
    }

    public final InferResult falsehoodsII() {
        return InferResult.falsehoodsII(this, singleton());
    }

    public final Set<Predicate> singleton() {
        return Set.of(this);
    }

    @Override
    public Predicate setType(int i, Type type) {
        Object[] array = setArray(i, type);
        return array != null ? setArgs(array) : this;
    }

    @Override
    protected Predicate setTyped(int i, Node typed) {
        Object[] array = setArray(i, typed);
        return array != null ? setArgs(array) : this;
    }

    public InferResult resolve(InferContext context) {
        return doInfer(nrOfUnbound(), context);
    }

    protected InferResult infer(InferContext context) {
        int nrOfUnbound = nrOfUnbound();
        if (nrOfUnbound > 0 == context.reduce()) {
            return unknown();
        }
        InferResult result = doInfer(nrOfUnbound, context);
        if (context.trace() && context.deep() && getClass() != Predicate.class && !isSyntatic()) {
            System.out.println(context.prefix() + "  " + this + " " + result.predicate(setVariables()));
        }
        return result;
    }

    private InferResult doInfer(int nrOfUnbound, InferContext context) {
        Method method = functor().method();
        return method != null ? callMethod(method, context) : infer(nrOfUnbound, context);
    }

    private InferResult callMethod(Method method, InferContext context) {
        return CURRENT_CONTEXT.get(context.withResult(), () -> {
            try {
                Object[] args = toArray();
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof Type) {
                        args[i] = null;
                    }
                }
                return (InferResult) method.invoke(this, args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalArgumentException(e);
            }
        });
    }

    protected InferResult infer(int nrOfUnbound, InferContext context) {
        Functor functor = functor();
        if (nrOfUnbound > 1 || //
                (context.shallow() && !isShallow(nrOfUnbound, functor)) || //
                (nrOfUnbound == 1 && functor.argTypes().size() == 1)) {
            return unknown();
        }
        KnowledgeBase knowledgebase = context.knowledgebase();
        if (isFact()) {
            return knowledgebase.getFacts(this, context);
        } else {
            InferResult result = knowledgebase.getMemoiz(this);
            if (result != null) {
                return result;
            }
            result = context.getCycleResult(this);
            if (result != null) {
                return result;
            }
            List<Predicate> stack = context.stack();
            if (stack.size() >= MAX_LOGIC_DEPTH) {
                return InferResult.overflow(stack.append(this));
            }
            result = fixpoint(context.pushOnStack(this));
            if (stack.size() >= MAX_LOGIC_DEPTH_D2) {
                List<Predicate> overflow = result.stackOverflow();
                if (overflow != null) {
                    if (stack.size() == MAX_LOGIC_DEPTH_D2) {
                        result = flatten(result, overflow, context);
                    }
                    return result;
                }
            }
            knowledgebase.memoization(this, result);
            return result;
        }
    }

    protected boolean isShallow(int nrOfUnbound, Functor functor) {
        return false;
    }

    private static InferResult flatten(InferResult result, List<Predicate> overflow, InferContext context) {
        int stackSize = context.stack().size();
        List<Predicate> todo = overflow.sublist(stackSize, overflow.size());
        KnowledgeBase knowledgebase = context.knowledgebase();
        while (!todo.isEmpty()) {
            Predicate predicate = todo.last();
            result = predicate.fixpoint(context.pushOnStack(predicate));
            overflow = result.stackOverflow();
            if (overflow != null) {
                todo = todo.appendList(overflow.sublist(stackSize + 1, overflow.size()));
            } else {
                knowledgebase.memoization(predicate, result);
                todo = todo.removeLast();
            }
        }
        return result;
    }

    private InferResult fixpoint(InferContext context) {
        InferResult previousResult = null, cycleResult = InferResult.cycle(Set.of(), Set.of(), this), nextResult;
        do {
            if (context.knowledgebase().isPastDeadline()) {
                throw new NelumboTimeoutException();
            }
            nextResult = inferRules(context.putCycleResult(this, cycleResult));
            if (nextResult.hasStackOverflow()) {
                return nextResult;
            }
            if (nextResult.hasCycleWith(this)) {
                if (!nextResult.equals(previousResult)) {
                    previousResult = nextResult;
                    cycleResult = InferResult.cycle(nextResult.facts(), nextResult.falsehoods(), this);
                    context.knowledgebase().memoization(this, cycleResult);
                    continue;
                } else {
                    cycleResult = InferResult.of(this, nextResult.facts(), true, nextResult.falsehoods(), true,
                            nextResult.cycles().remove(this));
                    context.knowledgebase().memoization(this, cycleResult);
                    nextResult = inferRules(context.putCycleResult(this, cycleResult));
                    if (nextResult.hasStackOverflow()) {
                        return nextResult;
                    }
                    return InferResult.of(this, nextResult.facts(), nextResult.completeFacts(), //
                            nextResult.falsehoods(), nextResult.completeFalsehoods(), //
                            nextResult.cycles().remove(this));
                }
            }
            return nextResult;
        } while (true);
    }

    private InferResult inferRules(InferContext context) {
        InferResult result = unknown();
        MutableMap<Variable, Type> typeArgs = MutableMap.of(Map.of());
        Set<Rule> rules = context.knowledgebase().getRules(this, typeArgs);
        for (Rule rule : REVERSE_NELUMBO ? rules.reverse() : RANDOM_NELUMBO ? rules.random() : rules) {
            result = rule.biimply(this, context, result);
            if (result.hasStackOverflow()) {
                return result;
            }
        }
        if (typeArgs.isEmpty()) {
            return result;
        } else {
            return result.setTypeArgs(typeArgs.get());
        }
    }

    public boolean isFact() {
        return Type.FACT_TYPE.isAssignableFrom(type());
    }

    public Predicate replaveVars(Map<String, Variable> map) {
        try {
            return (Predicate) replace(from -> (from instanceof Variable || from instanceof BooleanVariable)
                    && map.get(from.toString()) instanceof Node to ? to : from);
        } catch (ParseException e) {
            return null;
        }
    }

    @Override
    public Predicate setTypeArgs(Map<Variable, Type> typeArgs) {
        return (Predicate) super.setTypeArgs(typeArgs);
    }

    protected final InferResult incompleteResult() {
        return CURRENT_CONTEXT.get().incompleteResult(this);
    }

    protected final boolean hasIncompleteResult() {
        return CURRENT_CONTEXT.get().hasIncompleteResult();
    }

}
