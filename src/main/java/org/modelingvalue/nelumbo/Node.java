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

import static org.modelingvalue.nelumbo.KnowledgeBase.TRACE_SYNTATIC;

import java.io.Serial;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.modelingvalue.collections.ContainingCollection;
import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.mutable.MutableMap;
import org.modelingvalue.collections.mutable.MutableSet;
import org.modelingvalue.collections.struct.impl.StructImpl;
import org.modelingvalue.collections.util.Context;
import org.modelingvalue.collections.util.Pair;
import org.modelingvalue.collections.util.StringUtil;
import org.modelingvalue.nelumbo.collections.NList;
import org.modelingvalue.nelumbo.lang.Functor;
import org.modelingvalue.nelumbo.lang.FunctorOrType;
import org.modelingvalue.nelumbo.lang.Transform;
import org.modelingvalue.nelumbo.lang.Type;
import org.modelingvalue.nelumbo.lang.Variable;
import org.modelingvalue.nelumbo.logic.InferContext;
import org.modelingvalue.nelumbo.logic.Predicate;
import org.modelingvalue.nelumbo.syntax.ParseContext;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ThrowingFunction;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class Node extends StructImpl implements AstElement {
    @Serial
    private static final long serialVersionUID = 7315776001191198132L;

    protected static final Context<InferContext> CURRENT_CONTEXT = Context.of(null);

    private static final AtomicLong UNIQUE_COUNTER = new AtomicLong(0);

    protected static String uniqueId() {
        long l = UNIQUE_COUNTER.getAndIncrement();
        return Long.toUnsignedString(l, Character.MAX_RADIX);
    }

    private final NodeInfo nodeInfo;

    // cash
    private int hashCodeCache;

    @NelumboConstructor
    public Node(NodeInfo nodeInfo, Object... args) {
        super(removeOptionals(args));
        this.nodeInfo = nodeInfo;
    }

    public NodeInfo nodeInfo() {
        return nodeInfo;
    }

    public FunctorOrType functorOrType() {
        return nodeInfo.functorOrType();
    }

    public final List<AstElement> astElements() {
        return nodeInfo.elements();
    }

    public Node setFunctorOrType(FunctorOrType functorOrType) {
        return functorOrType.equals(functorOrType()) ? this : set(nodeInfo.setFunctorOrType(functorOrType), toArray());
    }

    public Node setAstElements(List<AstElement> elements) {
        return elements.equals(astElements()) ? this : set(nodeInfo.setElements(elements), toArray());
    }

    private static Object[] removeOptionals(Object[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Optional<?> opt) {
                args[i] = opt.orElse(null);
            }
        }
        return args;
    }

    public Type type() {
        return functorOrType().resultType();
    }

    public Functor functor() {
        FunctorOrType tf = functorOrType();
        return tf instanceof Functor ? (Functor) tf : null;
    }

    public List<Object> args() {
        List<Object> args = List.of();
        for (int i = 0; i < length(); i++) {
            Object a = get(i);
            args = args.add(a == null ? Optional.empty() : a);
        }
        return args;
    }

    public List<Node> children() {
        List<Node> children = List.of();
        for (int i = 0; i < length(); i++) {
            children = children(get(i), children);
        }
        return children;
    }

    private List<Node> children(Object a, List<Node> children) {
        if (a instanceof Node n) {
            children = children.add(n);
        } else if (a instanceof ContainingCollection<?> coll) {
            for (Object e : coll) {
                children = children(e, children);
            }
        }
        return children;
    }

    @Override
    public int hashCode() {
        if (hashCodeCache == 0) {
            int hc = 31 * super.hashCode() + functorOrTypeForEquals().hashCode();
            hashCodeCache = hc == 0 ? 1 : hc;
        }
        return hashCodeCache;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof Node other)) {
            return false;
        } else if (obj.hashCode() != hashCode()) {
            return false;
        } else if (obj.getClass() != getClass()) {
            return false;
        } else if (!functorOrTypeForEquals().equals(other.functorOrTypeForEquals())) {
            return false;
        }
        return super.equals(obj);
    }

    public final Set<Pair<Object, Object>> diff(Node other) {
        MutableSet<Pair<Object, Object>> diff = MutableSet.of(Set.of());
        diff(other, diff);
        return diff.get();
    }

    public static final Set<Pair<Object, Object>> diff(Object a, Object b) {
        MutableSet<Pair<Object, Object>> diff = MutableSet.of(Set.of());
        diff(a, b, diff);
        return diff.get();
    }

    private void diff(Node other, MutableSet<Pair<Object, Object>> diff) {
        Object tfe = functorOrType(), otfe = other.functorOrType();
        if (!Objects.equals(tfe, otfe)) {
            diff.add(Pair.of(tfe, otfe));
        } else {
            Object[] a = toArray(), b = other.toArray();
            if (a.length != b.length) {
                diff.add(Pair.of(a, b));
            } else {
                for (int i = 0; i < a.length; i++) {
                    diff(a[i], b[i], diff);
                }
            }
        }
    }

    private static void diff(Object a, Object b, MutableSet<Pair<Object, Object>> diff) {
        if (a instanceof Node na && b instanceof Node nb) {
            na.diff(nb, diff);
        } else if (a instanceof ContainingCollection ca && b instanceof ContainingCollection cb) {
            if (ca.size() != cb.size()) {
                diff.add(Pair.of(ca, cb));
            } else {
                for (int i = 0; i < ca.size(); i++) {
                    diff(ca.get(i), cb.get(i), diff);
                }
            }
        } else if (!Objects.equals(a, b)) {
            diff.add(Pair.of(a, b));
        }
    }

    public Object functorOrTypeForEquals() {
        return functorOrType();
    }

    public List<Variable> localVars() {
        return List.of();
    }

    @Override
    public final String toString() {
        return toString(new TokenType[1]);
    }

    public String toString(TokenType[] previous) {
        Functor functor = functor();
        if (functor != null) {
            String string = functor.string(args(), previous);
            if (string != null) {
                return string;
            }
            functor.string(args(), previous);
        }
        StringBuilder sb = new StringBuilder();
        if (functor != null) {
            sb.append(functor.name());
        } else {
            sb.append(type().name());
        }
        sb.append('(');
        String sep = "";
        for (int i = 0; i < length(); i++) {
            sb.append(sep).append(toString(i));
            sep = ",";
        }
        sb.append(')');
        return sb.toString();
    }

    public final String toString(int i) {
        return StringUtil.toString(get(i));
    }

    @SuppressWarnings("unchecked")
    public <V> V getVal(int... is) {
        Object val = this;
        for (int i : is) {
            val = ((Node) val).get(i);
            if (val instanceof Type || val instanceof Variable) {
                return null;
            }
        }
        return (V) val;
    }

    protected final Object[] setArray(int f, Object... a) {
        Object[] array = null;
        for (int i = 0; i < a.length; i++) {
            Object v = get(i + f);
            if (!Objects.equals(a[i], v)) {
                if (array == null) {
                    array = toArray();
                }
                array[i + f] = a[i];
            }
        }
        return array;
    }

    public Node set(int f, Object... a) {
        Object[] array = setArray(f, a);
        return array != null ? setArgs(array) : this;
    }

    public Node set(int[] idx, Object val) {
        return set(0, idx, val);
    }

    private Node set(int ii, int[] idx, Object val) {
        Object[] array = toArray();
        int i = idx[ii];
        if (ii < idx.length - 1) {
            Node s = (Node) array[i];
            array[i] = s.set(ii + 1, idx, val);
        } else {
            array[i] = val;
        }
        return setArgs(array);
    }

    public Node setArgs(Object... args) {
        return set(nodeInfo, args);
    }

    protected Node set(NodeInfo nodeInfo, Object[] args) {
        if (getClass() != Node.class) {
            Functor functor = functor();
            if (functor != null) {
                Constructor<?> constructor = functor.constructor();
                if (constructor != null) {
                    try {
                        return (Node) constructor.newInstance(nodeInfo, args);
                    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                            | InvocationTargetException e) {
                        throw new IllegalArgumentException(e);
                    }
                }
            }
            throw new IllegalStateException(
                    "Specialisation of Node " + this.getClass().getName() + " without @NelumboConstructor");
        }
        return new Node(nodeInfo, args);
    }

    public Set<Variable> allVars() {
        Set<Variable> allVars = Set.of();
        for (int i = 0; i < length(); i++) {
            allVars = allVars(get(i), allVars);
        }
        return allVars;
    }

    private static Set<Variable> allVars(Object val, Set<Variable> allVars) {
        if (val instanceof Node node) {
            allVars = allVars.addAll(node.allVars());
        } else if (val instanceof ContainingCollection<?> coll) {
            for (Object e : coll) {
                allVars = allVars(e, allVars);
            }
        }
        return allVars;
    }

    public Set<Variable> allLocalVars() {
        Set<Variable> allLocalVars = localVars().asSet();
        for (int i = 0; i < length(); i++) {
            allLocalVars = allLocalVars(get(i), allLocalVars);
        }
        return allLocalVars;
    }

    private static Set<Variable> allLocalVars(Object val, Set<Variable> allLocalVars) {
        if (val instanceof Node node) {
            allLocalVars = allLocalVars.addAll(node.allLocalVars());
        } else if (val instanceof ContainingCollection<?> coll) {
            for (Object e : coll) {
                allLocalVars = allLocalVars(e, allLocalVars);
            }
        }
        return allLocalVars;
    }

    public final Map<Variable, Object> getBinding(Node declaration) {
        return getBinding(declaration, Map.of());
    }

    private Map<Variable, Object> getBinding(Node declaration, Map<Variable, Object> vars) {
        for (int i = 0; vars != null && i < length(); i++) {
            vars = getBinding(declaration.get(i), get(i), vars, i);
        }
        return vars;
    }

    private Map<Variable, Object> getBinding(Object declVal, Object thisIn, Map<Variable, Object> vars, int i) {
        Object thisVal = thisIn instanceof Variable ? null : thisIn;
        if (declVal instanceof Type declType) {
            declVal = declType.variable();
        }
        if (declVal instanceof Variable declVar) {
            Object varVal = vars.get(declVar);
            varVal = varVal instanceof Type || varVal instanceof Variable ? null : varVal;
            if (varVal != null) {
                if (thisVal != null && !thisVal.equals(varVal)) {
                    return null;
                }
            } else {
                if (thisVal == null) {
                    thisVal = varOf(declVar, thisIn);
                }
                if (thisVal != null && doGetBinding(thisVal, i)) {
                    vars = vars.put(declVar, thisVal);
                }
            }
        } else if (declVal instanceof Node declNode && thisVal instanceof Node thisNode) {
            // noinspection ConstantValue
            assert !(declVal instanceof Type || declVal instanceof Variable);
            vars = thisNode.getBinding(declNode, vars);
        } else if (declVal instanceof ContainingCollection<?> declList
                && thisVal instanceof ContainingCollection<?> thisList && //
                declList.size() == thisList.size()) {
            for (int ii = 0; ii < declList.size(); ii++) {
                vars = getBinding(declList.get(ii), thisList.get(ii), vars, i);
            }
        }
        return vars;
    }

    private static Variable varOf(Variable declVar, Object v) {
        return v instanceof Variable var ? declVar.setType(var.type()) : declVar;
    }

    public Node set(Variable var, Object val) {
        return setBinding(this, Map.of(Entry.of(var, val)), false);
    }

    public Node setBinding(Node declaration, Map<Variable, Object> vars) {
        return setBinding(declaration, vars, false);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Node setTypeArgs(Map<Variable, Type> typeArgs) {
        return typeArgs.isEmpty() ? this : setBinding(this, (Map) typeArgs, true);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected Node setBinding(Node declaration, Map<Variable, Object> vars, boolean setFunctorOrType) {
        Object[] array = null;
        for (int i = 0; i < length(); i++) {
            Object thisVal = get(i);
            Object bound = setBinding(declaration.get(i), thisVal, vars, i, setFunctorOrType);
            if (bound != thisVal) {
                if (array == null) {
                    array = toArray();
                }
                array[i] = bound;
            }
        }
        if (setFunctorOrType && !(this instanceof Type)) {
            FunctorOrType fot = functorOrType().setTypeArgs((Map) vars);
            return array != null ? set(nodeInfo.setFunctorOrType(fot), array) : setFunctorOrType(fot);
        } else {
            return array != null ? set(nodeInfo, array) : this;
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected final Object setBinding(Object declVal, Object thisVal, Map<Variable, Object> vars, int i,
            boolean setFunctorOrType) {
        if (declVal instanceof Variable declVar) {
            Object varVal = vars.get(declVar);
            if (varVal != null && doSetBinding(varVal, i)) {
                return varVal;
            }
            if (thisVal instanceof Variable thisVar) {
                Type from = thisVar.type();
                Variable var = from.variable();
                if (var != null && vars.get(var) instanceof Type to) {
                    return thisVar.setType(from.rewrite(to));
                }
            }
        } else if (declVal instanceof Node declNode && !(declNode instanceof Type) && //
                thisVal instanceof Node thisNode && !(thisNode instanceof Type)) {
            return thisNode.setBinding(declNode, vars, setFunctorOrType);
        } else if (declVal instanceof ContainingCollection declList && thisVal instanceof ContainingCollection thisList
                && //
                declList.size() == thisList.size()) {
            ContainingCollection list = declList.clear();
            for (int ii = 0; ii < declList.size(); ii++) {
                list = list.add(setBinding(declList.get(ii), thisList.get(ii), vars, i, setFunctorOrType));
            }
            return thisList.equals(list) ? thisList : list;
        } else if (declVal instanceof Type declType && thisVal instanceof Type thisType) {
            Variable declVar = declType.variable();
            if (declVar != null) {
                Object varVal = vars.get(declVar);
                if (varVal instanceof Type type) {
                    return thisType.rewrite(type);
                } else if (varVal instanceof Variable valVar) {
                    return new Type(valVar);
                }
            } else {
                return thisType.setBinding(declType, vars, setFunctorOrType);
            }
        }
        return thisVal;
    }

    protected boolean doGetBinding(Object varVal, int i) {
        return true;
    }

    protected boolean doSetBinding(Object varVal, int i) {
        return true;
    }

    public Node makeVariablesUnique(ParseContext ctx) throws ParseException {
        return makeVariablesUnique(ctx, uniqueId());
    }

    public Node makeVariablesUnique(ParseContext ctx, String id) throws ParseException {
        return replace(o -> {
            if (o instanceof Variable v
                    && (ctx.outer().type(v.name()) != null || ctx.outer().variable(v.name()) != null)) {
                return v.makeUnique(id);
            }
            return o;
        });
    }

    public final Node replace(ThrowingFunction<Object, Object> replacer) throws ParseException {
        Object[] array = null;
        for (int i = 0; i < length(); i++) {
            Object fromVal = get(i);
            Object toVal = replace(fromVal, replacer);
            if (toVal != fromVal) {
                if (array == null) {
                    array = toArray();
                }
                array[i] = toVal;
            }
        }
        return (Node) replacer.apply(array != null ? set(nodeInfo, array) : this);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object replace(Object from, ThrowingFunction<Object, Object> replacer) throws ParseException {
        if (from instanceof Node fromNode) {
            return fromNode.replace(replacer);
        } else if (from instanceof ContainingCollection fromColl) {
            ContainingCollection toColl = null;
            for (int i = 0; i < fromColl.size(); i++) {
                Object fromVal = fromColl.get(i);
                Object toVal = replace(fromVal, replacer);
                if (toVal != fromVal || toColl != null) {
                    if (toColl == null) {
                        toColl = fromColl.clear();
                        for (int ii = 0; ii < i; ii++) {
                            toColl = toColl.add(fromColl.get(ii));
                        }
                    }
                    toColl = toColl.add(toVal);
                }
            }
            return toColl != null ? toColl : fromColl;
        }
        return replacer.apply(from);
    }

    public Node setType(int i, Type type) {
        return set(i, type);
    }

    protected Node setTyped(int i, Node typed) {
        return set(i, typed);
    }

    @Override
    public Token firstToken() {
        return AstElement.firstToken(astElements());
    }

    @Override
    public Token lastToken() {
        return AstElement.lastToken(astElements());
    }

    public Token nextToken() {
        return lastToken().next();
    }

    public List<Token> tokens() {
        Token first = firstToken();
        return first != null ? first.list(lastToken()) : List.of();
    }

    public <E extends Node> MatchState<E> state(MatchState<E> next) {
        for (Object arg : args().reverse()) {
            switch (arg) {
            case Type type    -> {
                next = matchType(next, type);
                break;
            }
            case Variable var -> {
                next = matchType(next, var.type());
                break;
            }
            case Node node    -> {
                next = node.state(next);
                break;
            }
            default           -> {
                next = new MatchState<>(arg.getClass(), next);
            }
            }
        }
        Functor functor = functor();
        assert functor != null;
        return new MatchState<>(functor, next);
    }

    private static <E extends Node> MatchState<E> matchType(MatchState<E> next, Type type) {
        TokenType tt = type.tokenType();
        if (tt != null) {
            return new MatchState<>(tt, next);
        } else {
            return new MatchState<>(type, next);
        }
    }

    public Node init(KnowledgeBase knowledgeBase, ParseContext ctx, ConstructionReason reason) throws ParseException {
        Node rewrite = this;
        if (reason == ConstructionReason.parsing && Type.ROOT.isAssignableFrom(type())) {
            MutableMap<Variable, Type> typeArgs = MutableMap.of(Map.of());
            for (Transform transform : knowledgeBase.getTransforms(this, typeArgs)) {
                rewrite = transform.setTypeArgs(typeArgs.get()).transform(transform, this, rewrite, knowledgeBase, ctx);
            }
        }
        return rewrite;
    }

    public Variable variable() {
        return null;
    }

    @Override
    public void deparse(StringBuffer sb) {
        for (AstElement e : astElements()) {
            e.deparse(sb);
        }
    }

    public final boolean isSyntatic() {
        return !TRACE_SYNTATIC && (astElements().isEmpty() || firstToken().fileName().contains("/nelumbo/"));
    }

    protected final Predicate predicate(int... i) {
        return getVal(i);
    }

    public Node add(Node added) {
        return new NList(Type.ROOT, List.of(this, added), List.of(this, added));
    }

    public Node setType(Type type) {
        return type.equals(type()) ? this : setFunctorOrType(type);
    }

    public Node castFrom(Node from) {
        Object[] fromArray = from.toArray();
        for (int i = 0; i < fromArray.length; i++) {
            fromArray[i] = castFrom(get(i), fromArray[i]);
        }
        return set(nodeInfo(), fromArray);
    }

    private static Object castFrom(Object to, Object from) {
        if (from instanceof Node fromNode && to instanceof Node toNode) {
            Functor fromFunctor = fromNode.functor();
            Functor toFunctor = toNode.functor();
            if (fromFunctor != null && toFunctor != null && !fromFunctor.equals(toFunctor)
                    && fromFunctor.original().equals(toFunctor.original())) {
                return toNode.castFrom(fromNode);
            }
        }
        return from;
    }

}
