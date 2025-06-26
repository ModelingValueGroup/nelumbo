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

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.util.Pair;

public abstract class Predicate extends Node {

    protected static final boolean RANDOM_NELUMBO   = java.lang.Boolean.getBoolean("RANDOM_NELUMBO");
    protected static final boolean REVERSE_NELUMBO  = java.lang.Boolean.getBoolean("REVERSE_NELUMBO");

    private static final long      serialVersionUID = -1605559565948158856L;
    public static final Type       TYPE             = new Type(Predicate.class);

    private final Set<Predicate>   singleton        = Set.of(this);
    private final Predicate        declaration;

    private Node                   parent;
    private int                    parentIdx;

    protected Predicate(Functor functor, Object... args) {
        super(functor, args);
        this.declaration = this;
    }

    protected void init(Node parent, int idx) {
        assert (this.parent == null && this.parentIdx == 0);
        this.parent = parent;
        this.parentIdx = idx;
    }

    private Pair<Node, int[]> rootIdx() {
        if (parent != null) {
            Pair<Node, int[]> root = parent instanceof Predicate ? ((Predicate) parent).rootIdx() : null;
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

    protected Node root() {
        Pair<Node, int[]> ri = rootIdx();
        return ri != null ? ri.a().set(ri.b(), Boolean.TRUE) : null;
    }

    protected Predicate(Object[] args, Predicate declaration) {
        super(args);
        this.declaration = declaration == null ? this : declaration;
    }

    public Predicate declaration() {
        return declaration;
    }

    protected Predicate castFrom(Predicate from) {
        throw new UnsupportedOperationException();
    }

    protected Predicate clearDeclaration() {
        return struct(toArray(), (Predicate) null);
    }

    @Override
    public String toString() {
        return setVariables().superToString();
    }

    private String superToString() {
        return super.toString();
    }

    private Predicate setVariables() {
        Map<Variable, Object> vars = getBinding(declaration, Map.of(), false);
        vars = vars.replaceAll(e -> e.getValue() instanceof Type ? Entry.of(e.getKey(), e.getKey()) : e);
        return setBinding(vars);
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
    protected final Predicate struct(Object[] array) {
        return struct(array, declaration);
    }

    protected abstract Predicate struct(Object[] array, Predicate declaration);

    public Type getType(int i) {
        Object v = get(i);
        return v instanceof Type ? (Type) v : v instanceof Node ? ((Node) v).type() : null;
    }

    public InferResult infer() {
        InferContext context = KnowledgeBase.CURRENT.get().context();
        Predicate predicate = setBinding(variables());
        if (context.trace()) {
            System.err.println(context.prefix() + "  " + predicate);
        }
        InferResult result = predicate.resolve(context);
        if (context.trace()) {
            System.err.println(context.prefix() + "  " + predicate + "\u2192" + result);
        }
        return result;
    }

    protected InferResult expand(InferContext context) {
        throw new UnsupportedOperationException();
    }

    protected abstract InferResult infer(InferContext context);

    public boolean contains(Predicate cond) {
        return equals(cond);
    }

    @Override
    public Predicate set(int i, Object... a) {
        return (Predicate) super.set(i, a);
    }

    protected Predicate replace(Predicate from, Predicate to) {
        return (Predicate) super.replace(from, to);
    }

    @Override
    public Predicate set(int[] idx, Object val) {
        return (Predicate) super.set(idx, val);
    }

    public Predicate copy(int from, int to) {
        return (Predicate) super.set(to, get(from));
    }

    @SuppressWarnings("unchecked")
    protected final Predicate set(int i, Predicate... a) {
        Object[] predArray = toArray();
        Object[] declArray = declaration.toArray();
        for (int x = 0; x < a.length; x++) {
            predArray[i + x] = a[x];
            declArray[i + x] = a[x].declaration;
        }
        return struct(predArray, declaration.struct(declArray, null));
    }

    public final InferResult unknown() {
        return InferResult.unknown(this);
    }

    public final InferResult factCC() {
        return InferResult.factsCC(singleton);
    }

    public final InferResult falsehoodCC() {
        return InferResult.falsehoodsCC(singleton);
    }

    public final InferResult factCI() {
        return InferResult.factsCI(singleton);
    }

    public final InferResult falsehoodIC() {
        return InferResult.falsehoodsCC(singleton);
    }

    public final Set<Predicate> singleton() {
        return singleton;
    }

    protected InferResult resolve(InferContext context) {
        Map<Map<Variable, Object>, Predicate> now, next = Map.of(Entry.of(getBinding(), this));
        Set<Predicate> facts = Set.of(), falsehoods = Set.of();
        boolean completeFacts = true, completeFalsehoods = true;
        Set<Relation> cycles = Set.of();
        InferContext reduce = context.reduce(true);
        do {
            now = next;
            next = Map.of();
            for (Entry<Map<Variable, Object>, Predicate> entry : now) {
                InferResult result = entry.getValue().infer(reduce);
                if (result.hasStackOverflow()) {
                    return result;
                } else if (result.isFalseCC()) {
                    falsehoods = falsehoods.add(setBinding(entry.getKey()));
                } else if (result.isTrueCC()) {
                    facts = facts.add(setBinding(entry.getKey()));
                } else {
                    Predicate predicate = result.unknown();
                    if (context.trace()) {
                        System.err.println(context.prefix() + "  " + predicate);
                    }
                    result = predicate.infer(context);
                    if (result.hasStackOverflow()) {
                        return result;
                    } else if (result.isEmpty()) {
                        completeFacts = false;
                        completeFalsehoods = false;
                        cycles = cycles.addAll(result.cycles());
                    } else {
                        for (Predicate pred : result.facts()) {
                            Map<Variable, Object> binding = entry.getKey().putAll(pred.getBinding());
                            next = next.put(binding, predicate.setBinding(binding).replace(pred, Boolean.TRUE));
                        }
                        for (Predicate pred : result.falsehoods()) {
                            Map<Variable, Object> binding = entry.getKey().putAll(pred.getBinding());
                            next = next.put(binding, predicate.setBinding(binding).replace(pred, Boolean.FALSE));
                        }
                        completeFacts &= result.completeFacts();
                        completeFalsehoods &= result.completeFalsehoods();
                        cycles = cycles.addAll(result.cycles());
                    }
                }
            }
        } while (!next.isEmpty());
        return InferResult.of(facts, completeFacts, falsehoods, completeFalsehoods, cycles);
    }

    @Override
    protected Predicate setType(int i, Type type) {
        Object[] array = setArray(i, type);
        return array != null ? struct(array, null) : this;
    }

    @Override
    protected Predicate setTyped(int i, Node typed) {
        Object[] array = setArray(i, typed);
        return array != null ? struct(array, null) : this;
    }

    @Override
    protected Predicate signature(int depth) {
        Object[] array = signatureArray(depth);
        return array != null ? struct(array, null) : this;
    }

}
