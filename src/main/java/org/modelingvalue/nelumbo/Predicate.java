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

import java.io.Serial;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.util.Pair;
import org.modelingvalue.nelumbo.syntax.Token;

public class Predicate extends Node {
    @Serial
    private static final long      serialVersionUID   = -1605559565948158856L;

    protected static final boolean RANDOM_NELUMBO     = java.lang.Boolean.getBoolean("RANDOM_NELUMBO");
    protected static final boolean REVERSE_NELUMBO    = java.lang.Boolean.getBoolean("REVERSE_NELUMBO");
    protected static final int     MAX_LOGIC_DEPTH    = Integer.getInteger("MAX_LOGIC_DEPTH", 32);

    private static final int       MAX_LOGIC_DEPTH_D2 = MAX_LOGIC_DEPTH / 2;

    public static Node             INCOMPLETE         = new Predicate(Type.PREDICATE, Token.EMPTY, "..");

    //
    private final InferResult      cycleResult        = InferResult.cycle(Set.of(), Set.of(), this);
    private final Predicate        declaration;
    private Predicate              parent;
    private int                    parentIdx;

    public Predicate(Functor functor, Token[] tokens, Object... args) {
        super(functor, tokens, args);
        this.declaration = this;
        init();
    }

    public Predicate(Type type, Token[] tokens, Object... args) {
        super(type, tokens, args);
        this.declaration = this;
        init();
    }

    protected Predicate(Object[] args, int start, Predicate declaration) {
        super(args, start);
        this.declaration = declaration == null ? this : declaration;
    }

    private void init() {
        for (int i = 0; i < length(); i++) {
            Object e = get(i);
            if (e instanceof Predicate) {
                ((Predicate) e).init(this, i);
            }
        }
    }

    protected void init(Predicate parent, int idx) {
        assert (this.parent == null && this.parentIdx == 0);
        this.parent = parent;
        this.parentIdx = idx;
    }

    private Pair<Predicate, int[]> rootIdx() {
        if (parent != null) {
            Pair<Predicate, int[]> root = parent.rootIdx();
            if (root != null) {
                int[] idx = new int[root.b().length + 1];
                System.arraycopy(root.b(), 0, idx, 1, root.b().length);
                idx[0] = parentIdx;
                return Pair.of(root.a(), idx);
            } else {
                return Pair.of(parent, new int[]{parentIdx});
            }
        } else {
            return null;
        }
    }

    protected Predicate root() {
        Pair<Predicate, int[]> ri = rootIdx();
        return ri != null ? ri.a().set(ri.b(), Boolean.TRUE) : null;
    }

    public Predicate declaration() {
        return declaration;
    }

    protected Predicate castFrom(Predicate from) {
        Object[] array = from.toArray();
        array[0] = functor();
        return from.struct(array, from.start, declaration());
    }

    protected Predicate setFunctor(Functor functor) {
        Object[] array = toArray();
        array[0] = functor;
        return struct(array, start, null);
    }

    private Predicate resetDeclaration() {
        Object[] array = toArray();
        for (int i = start; i < array.length; i++) {
            if (array[i] instanceof Predicate) {
                array[i] = ((Predicate) array[i]).resetDeclaration();
            }
        }
        return struct(array, start, null);
    }

    @SuppressWarnings("unused")
    protected Predicate clearDeclaration() {
        return struct(toArray(), start, null);
    }

    @Override
    public String toString() {
        return setVariables().superToString();
    }

    private String superToString() {
        return super.toString();
    }

    private Predicate setVariables() {
        Map<Variable, Object> vars = getBinding();
        vars = vars.replaceAll(e -> e.getValue() instanceof Type ? Entry.of(e.getKey(), e.getKey()) : e);
        return setBinding(vars);
    }

    protected static Map<Variable, Object> literals(Map<Variable, Object> vars) {
        return vars.replaceAll(e -> Entry.of(e.getKey(), e.getKey().literal()));
    }

    protected Predicate setVariables(Map<Variable, Object> vars) {
        Predicate predicate = setBinding(vars);
        return predicate == this ? this : predicate.resetDeclaration();
    }

    public Map<Variable, Object> getBinding() {
        return super.getBinding(declaration, Map.of(), false);
    }

    protected final Predicate setBinding(Map<Variable, Object> vars) {
        return (Predicate) super.setBinding(declaration, vars);
    }

    protected Predicate set(Variable var, Object val) {
        return (Predicate) super.set(declaration, var, val);
    }

    protected final Object get(Variable var) {
        return super.get(declaration, var);
    }

    @Override
    protected final Predicate struct(Object[] array, int start) {
        return struct(array, start, declaration);
    }

    protected Predicate struct(Object[] array, int start, Predicate declaration) {
        return new Predicate(array, start, declaration);
    }

    public Type getType(int i) {
        Object v = get(i);
        return v instanceof Type ? (Type) v : v instanceof Node ? ((Node) v).type() : null;
    }

    public InferResult infer() {
        KnowledgeBase knowledgeBase = KnowledgeBase.CURRENT.get();
        if (knowledgeBase.noInfer()) {
            return unknown();
        }
        InferContext context = knowledgeBase.context();
        Predicate predicate = setBinding(variables());
        if (context.trace()) {
            System.out.println(context.prefix() + predicate);
        }
        InferResult result = predicate.resolve(context);
        if (context.trace()) {
            System.out.println(context.prefix() + predicate + " " + result);
        }
        return result;
    }

    @SuppressWarnings("unused")
    protected InferResult expand(InferContext context) {
        throw new UnsupportedOperationException();
    }

    public boolean contains(Predicate cond) {
        return equals(cond);
    }

    @Override
    public Predicate set(int i, Object... a) {
        return (Predicate) super.set(i, a);
    }

    protected Predicate replace(Predicate from, Predicate to) {
        if (equals(from)) {
            return to;
        } else {
            Predicate decl = declaration;
            Object[] array = null;
            for (int i = 0; i < length(); i++) {
                Object thisVal = get(i);
                if (thisVal instanceof Predicate fromDecl) {
                    Predicate toDecl = fromDecl.replace(from, to);
                    if (toDecl != fromDecl) {
                        decl = decl.set(i, toDecl.declaration);
                        if (array == null) {
                            array = toArray();
                        }
                        array[i + start] = toDecl;
                    }
                }
            }
            return array != null ? struct(array, start, decl) : this;
        }
    }

    @Override
    public Predicate set(int[] idx, Object val) {
        return (Predicate) super.set(idx, val);
    }

    public Predicate copy(int from, int to) {
        return (Predicate) super.set(to, get(from));
    }

    protected final Predicate set(int from, Predicate... a) {
        Object[] declArray = declaration.toArray();
        int i = from + declaration.start;
        for (int x = 0; x < a.length; x++) {
            declArray[i + x] = a[x].declaration;
        }
        Predicate newDeclaration = declaration.struct(declArray, declaration.start, null);

        Object[] predArray = toArray();
        System.arraycopy(a, 0, predArray, from + start, a.length);
        return struct(predArray, start, newDeclaration);
    }

    public final InferResult unknown() {
        return InferResult.unknown(this);
    }

    public final InferResult factCC() {
        return InferResult.factsCC(singleton());
    }

    public final InferResult falsehoodCC() {
        return InferResult.falsehoodsCC(singleton());
    }

    public final InferResult factCI() {
        return InferResult.factsCI(singleton());
    }

    @SuppressWarnings("unused")
    public final InferResult falsehoodIC() {
        return InferResult.falsehoodsIC(singleton());
    }

    @SuppressWarnings("unused")
    public final InferResult factIC() {
        return InferResult.factsIC(singleton());
    }

    public final InferResult falsehoodCI() {
        return InferResult.falsehoodsCI(singleton());
    }

    public final Set<Predicate> singleton() {
        return Set.of(this);
    }

    @Override
    protected Predicate setType(int i, Type type) {
        Object[] array = setArray(i, type);
        return array != null ? struct(array, start, null) : this;
    }

    @Override
    protected Predicate setTyped(int i, Node typed) {
        Object[] array = setArray(i, typed);
        return array != null ? struct(array, start, null) : this;
    }

    @Override
    protected Predicate signature(int depth) {
        Object[] array = signatureArray(depth);
        return array != null ? struct(array, start, null) : this;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected final Set<Predicate> generalize(boolean full) {
        return (Set<Predicate>) super.generalize(full);
    }

    protected InferResult resolve(InferContext context) {
        return infer(nrOfUnbound(), context);
    }

    protected InferResult infer(InferContext context) {
        int nrOfUnbound = nrOfUnbound();
        if (nrOfUnbound > 0 && context.reduce()) {
            return unknown();
        }
        InferResult result = infer(nrOfUnbound, context);
        if (context.trace() && getClass() != Predicate.class && !result.isEmpty()) {
            System.out.println(context.prefix() + "  " + this + " " + result);
        }
        return result;
    }

    protected InferResult infer(int nrOfUnbound, InferContext context) {
        if (KnowledgeBase.CURRENT.get().noInfer()) {
            return unknown();
        }
        Functor functor = functor();
        if (nrOfUnbound > 1 || (nrOfUnbound == 1 && functor.args().size() == 1)) {
            return unknown();
        }
        KnowledgeBase knowledgebase = context.knowledgebase();
        if (knowledgebase.getRules(this).isEmpty()) {
            return knowledgebase.getFacts(this, context);
        } else {
            InferResult result = knowledgebase.getMemoiz(this);
            if (result != null) {
                return result;
            }
            result = context.getCycleResult(this);
            if (result != null) {
                return context.reduce() ? unknown() : result;
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

    private static InferResult flatten(InferResult result, List<Predicate> overflow, InferContext context) {
        int stackSize = context.stack().size();
        List<Predicate> todo = overflow.sublist(stackSize, overflow.size());
        while (!todo.isEmpty()) {
            Predicate predicate = todo.last();
            result = predicate.fixpoint(context.pushOnStack(predicate));
            overflow = result.stackOverflow();
            if (overflow != null) {
                todo = todo.appendList(overflow.sublist(stackSize, overflow.size()));
            } else {
                context.knowledgebase().memoization(predicate, result);
                todo = todo.removeLast();
            }
        }
        return result;
    }

    private InferResult fixpoint(InferContext context) {
        InferResult previousResult = null, cycleResult = this.cycleResult, nextResult;
        do {
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
                    return InferResult.of(nextResult.facts(), nextResult.completeFacts(), //
                            nextResult.falsehoods(), nextResult.completeFalsehoods(), //
                            nextResult.cycles().remove(this));
                }
            }
            return nextResult;
        } while (true);
    }

    private InferResult inferRules(InferContext context) {
        KnowledgeBase knowledgebase = context.knowledgebase();
        InferResult result = knowledgebase.getFacts(this, context), ruleResult;
        if (result.isTrueCC()) {
            return result;
        }
        Set<Rule> rules = knowledgebase.getRules(this);
        for (Rule rule : REVERSE_NELUMBO ? rules.reverse() : RANDOM_NELUMBO ? rules.random() : rules) {
            ruleResult = rule.imply(this, context);
            if (ruleResult != null) {
                if (ruleResult.hasStackOverflow()) {
                    return ruleResult;
                } else if (ruleResult.isTrueCC()) {
                    return ruleResult;
                } else if (ruleResult.hasCycleWith(this)) {
                    ruleResult = ruleResult.complete();
                }
                if (rule.symmetric()) {
                    return ruleResult;
                } else {
                    result = result.or(ruleResult);
                }
            }
        }
        return result;
    }

}
