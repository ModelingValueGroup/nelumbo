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
import java.util.function.Function;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.*;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.syntax.ParseContext;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class Predicate extends Node {
    @Serial
    private static final long      serialVersionUID   = -1605559565948158856L;

    protected static final boolean RANDOM_NELUMBO     = Boolean.getBoolean("RANDOM_NELUMBO");
    protected static final boolean REVERSE_NELUMBO    = Boolean.getBoolean("REVERSE_NELUMBO");
    protected static final int     MAX_LOGIC_DEPTH    = Integer.getInteger("MAX_LOGIC_DEPTH", 32);

    private static final int       MAX_LOGIC_DEPTH_D2 = MAX_LOGIC_DEPTH / 2;

    public static Node             INCOMPLETE         = new Predicate(Type.BOOLEAN, List.of(), "..");

    private int                    nrOfUnbound        = -1;

    public Predicate(Functor functor, List<AstElement> elements, Object... args) {
        super(functor, elements, args);
    }

    public Predicate(Type type, List<AstElement> elements, Object... args) {
        super(type, elements, args);
    }

    protected Predicate(Object[] args, Predicate declaration) {
        super(args, declaration);
    }

    @Override
    public Predicate declaration() {
        return (Predicate) super.declaration();
    }

    protected final int nrOfUnbound() {
        if (nrOfUnbound < 0) {
            nrOfUnbound = countNrOfUnbound();
        }
        return nrOfUnbound;
    }

    protected int countNrOfUnbound() {
        return (int) getBinding().filter(e -> e.getValue() instanceof Type).count();
    }

    public final boolean isFullyBound() {
        return nrOfUnbound() == 0;
    }

    public Predicate castFrom(Predicate from) {
        Object[] array = from.toArray();
        array[0] = functor();
        return from.struct(array, declaration());
    }

    @SuppressWarnings("unused")
    protected Predicate clearDeclaration() {
        return struct(toArray(), null);
    }

    @Override
    public String toString(TokenType[] previous) {
        return setVariables().superToString(previous);
    }

    private String superToString(TokenType[] previous) {
        return super.toString(previous);
    }

    private Predicate setVariables() {
        Map<Variable, Object> vars = getBinding();
        vars = vars.replaceAll(e -> e.getValue() instanceof Type ? Entry.of(e.getKey(), e.getKey()) : e);
        return setBinding(vars);
    }

    public static Map<Variable, Object> literals(Map<Variable, Object> vars) {
        return vars.replaceAll(e -> Entry.of(e.getKey(), e.getKey().literal()));
    }

    protected static Map<Variable, Object> literals(Map<Variable, Object> vars, Function<String, String> rename) {
        return vars.replaceAll(e -> Entry.of(e.getKey(), e.getKey().literal().rename(rename.apply(e.getKey().name()))));
    }

    public Predicate setVariables(Map<Variable, Object> vars, ParseContext ctx) throws ParseException {
        Predicate predicate = setBinding(vars);
        if (predicate == this) {
            return this;
        }
        KnowledgeBase kb = KnowledgeBase.CURRENT.get();
        predicate = (Predicate) predicate.replace(n -> {
            Functor functor = n.functor();
            if (functor != null) {
                Functor lit = kb.literal(functor);
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
    protected Predicate set(Variable var, Object val) {
        return (Predicate) super.set(var, val);
    }

    @Override
    protected Predicate struct(Object[] array, Node declaration) {
        return new Predicate(array, (Predicate) declaration);
    }

    public Type getType(int i) {
        Object v = get(i);
        return v instanceof Type ? (Type) v : v instanceof Node ? ((Node) v).type() : null;
    }

    public InferResult infer() {
        KnowledgeBase knowledgeBase = KnowledgeBase.CURRENT.get();
        InferContext context = knowledgeBase.context();
        Predicate predicate = setBinding(getBinding());
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

    @Override
    public Predicate setFunctor(Functor functor) {
        return (Predicate) super.setFunctor(functor);
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
                        array[i + START] = toDecl;
                    }
                }
            }
            return array != null ? struct(array, decl) : this;
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
        int i = from + START;
        for (int x = 0; x < a.length; x++) {
            declArray[i + x] = a[x].declaration();
        }
        Predicate newDeclaration = declaration().struct(declArray, null);
        Object[] predArray = toArray();
        System.arraycopy(a, 0, predArray, from + START, a.length);
        return struct(predArray, newDeclaration);
    }

    public final InferResult unknown() {
        return InferResult.unknown(this);
    }

    public final InferResult unresolvable() {
        return InferResult.unresolvable(this);
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
        return InferResult.falsehoodCI(this);
    }

    public final InferResult falsehoodsII() {
        return InferResult.falsehoodsII(singleton());
    }

    public final Set<Predicate> singleton() {
        return Set.of(this);
    }

    @Override
    public Predicate setType(int i, Type type) {
        Object[] array = setArray(i, type);
        return array != null ? struct(array, null) : this;
    }

    @Override
    protected Predicate setTyped(int i, Node typed) {
        Object[] array = setArray(i, typed);
        return array != null ? struct(array, null) : this;
    }

    public InferResult resolve(InferContext context) {
        return infer(nrOfUnbound(), context);
    }

    protected InferResult infer(InferContext context) {
        int nrOfUnbound = nrOfUnbound();
        if (nrOfUnbound > 0 && context.reduce()) {
            return unresolvable();
        } else if (nrOfUnbound == 0 && context.shallow()) {
            return unresolvable();
        }
        InferResult result = infer(nrOfUnbound, context);
        if (context.trace() && !result.unresolvable() && getClass() != Predicate.class && !(this instanceof Quantifier)) {
            System.out.println(context.prefix() + "  " + this + " " + result);
        }
        return result;
    }

    protected InferResult infer(int nrOfUnbound, InferContext context) {
        Functor functor = functor();
        if (nrOfUnbound > 1 || (context.shallow() && !isShallow(nrOfUnbound, functor)) || (nrOfUnbound == 1 && functor.argTypes().size() == 1)) {
            return unresolvable();
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

    private boolean isShallow(int nrOfUnbound, Functor functor) {
        if (nrOfUnbound == 1 && KnowledgeBase.equalsFunctor().equals(functor)) {
            Node a = getVal(0);
            Node b = getVal(1);
            return (b == null && a != null && Type.LITERAL.isAssignableFrom(a.type())) || //
                    (a == null && b != null && Type.LITERAL.isAssignableFrom(b.type()));
        }
        return false;
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
        InferResult previousResult = null, cycleResult = InferResult.cycle(Set.of(), Set.of(), this), nextResult;
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
                    cycleResult = InferResult.of(nextResult.facts(), true, nextResult.falsehoods(), true, nextResult.cycles().remove(this));
                    context.knowledgebase().memoization(this, cycleResult);
                    nextResult = inferRules(context.putCycleResult(this, cycleResult));
                    if (nextResult.hasStackOverflow()) {
                        return nextResult;
                    }
                    return InferResult.of(nextResult.facts(), nextResult.completeFacts(), //
                            nextResult.falsehoods(), nextResult.completeFalsehoods(), //
                            nextResult.cycles().remove(this));
                }
            }
            return nextResult;
        } while (true);
    }

    private InferResult inferRules(InferContext context) {
        InferResult result = unknown();
        Set<Rule> rules = context.knowledgebase().getRules(this);
        for (Rule rule : REVERSE_NELUMBO ? rules.reverse() : RANDOM_NELUMBO ? rules.random() : rules) {
            result = rule.biimply(this, context, result);
            if (result.hasStackOverflow()) {
                return result;
            }
        }
        return result;
    }

    public boolean isFact() {
        return Type.FACT_TYPE.isAssignableFrom(type());
    }

}
