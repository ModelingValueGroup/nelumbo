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
import java.util.concurrent.atomic.AtomicInteger;

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

    private final NodeInfo nodeInfo;

    //
    private Map<Variable, Object> binding;
    private int                   hashCodeCache;

    @NelumboConstructor
    public Node(NodeInfo nodeInfo, Object... args) {
        super(removeOptionals(args));
        this.nodeInfo = nodeInfo.declaration() == null ? nodeInfo.setDeclaration(this) : nodeInfo;
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

    public Node declaration() {
        return nodeInfo.declaration();
    }

    public Node setFunctorOrType(FunctorOrType functorOrType) {
        return functorOrType.equals(functorOrType()) ? this : set(nodeInfo.setFunctorOrType(functorOrType), toArray());
    }

    public Node setAstElements(List<AstElement> elements) {
        return elements.equals(astElements()) ? this : set(nodeInfo.setElements(elements), toArray());
    }

    public Node resetDeclaration() {
        Object[] array = toArray();
        for (int i = 0; i < array.length; i++) {
            array[i] = resetDeclaration(array[i]);
        }
        return set(nodeInfo.resetDeclaration(), array);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Object resetDeclaration(Object from) {
        if (from instanceof Node node) {
            return node.resetDeclaration();
        } else if (from instanceof ContainingCollection coll) {
            ContainingCollection c = coll.clear();
            for (Object e : coll) {
                c = c.add(resetDeclaration(e));
            }
            return c;
        }
        return from;
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
            int hc = 31 * super.hashCode() + typeForEquals().hashCode();
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
        } else if (!typeForEquals().equals(other.typeForEquals())) {
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
        Object tfe = typeForEquals(), otfe = other.typeForEquals();
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

    protected Object typeForEquals() {
        return functorOrType().declaration();
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

    public Set<Variable> allLocalVars() {
        Set<Variable> allLocalVars = localVars().asSet();
        for (int i = 0; i < length(); i++) {
            allLocalVars = allLocalVars(get(i), allLocalVars);
        }
        return allLocalVars;
    }

    private Set<Variable> allLocalVars(Object val, Set<Variable> allLocalVars) {
        if (val instanceof Node node) {
            allLocalVars = allLocalVars.addAll(node.allLocalVars());
        } else if (val instanceof ContainingCollection<?> coll) {
            for (Object e : coll) {
                allLocalVars = allLocalVars(e, allLocalVars);
            }
        }
        return allLocalVars;
    }

    public final Map<Variable, Object> getBinding() {
        if (binding == null) {
            binding = getBinding(declaration());
        }
        return binding;
    }

    public final Map<Variable, Object> getBinding(Node declaration) {
        return declaration == null ? getBinding() : getBinding(declaration, Map.of());
    }

    private Map<Variable, Object> getBinding(Node declaration, Map<Variable, Object> vars) {
        for (int i = 0; vars != null && i < length(); i++) {
            vars = getBinding(declaration.get(i), get(i), vars, i);
        }
        return vars;
    }

    private Map<Variable, Object> getBinding(Object declVal, Object thisIn, Map<Variable, Object> vars, int i) {
        Object thisVal = thisIn instanceof Type || thisIn instanceof Variable ? null : thisIn;
        if (declVal instanceof Type declType) {
            declVal = declType.variable();
        }
        if (declVal instanceof Variable declVar) {
            Object varVal = vars.get(declVar);
            varVal = varVal instanceof Type ? null : varVal;
            if (varVal != null) {
                if (thisVal != null && !thisVal.equals(varVal)) {
                    return null;
                }
            } else {
                if (thisVal == null) {
                    if (thisIn instanceof Variable thisVar && !thisVar.equals(declVar)) {
                        thisVal = thisVar;
                    } else {
                        thisVal = typeOf(thisIn);
                    }
                }
                if (thisVal != null && doGetBinding(thisVal, i)) {
                    vars = vars.put(declVar, thisVal);
                    if (thisVal instanceof Node thisNode && !(thisNode instanceof Type)) {
                        vars = vars.putAll(thisNode.getBinding().removeAllKey(allLocalVars()).replaceAll(e -> {
                            Variable nodeVar = e.getKey();
                            return Entry.of(nodeVar.rename(n -> "$" + n), e.getValue());
                        }));
                    }
                }
            }
        } else if (declVal instanceof Node declNode && thisVal instanceof Node thisNode) {
            // noinspection ConstantValue
            assert !(declVal instanceof Type);
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

    public static Type typeOf(Object v) {
        return v instanceof Type type ? type : v instanceof Node node ? node.type() : null;
    }

    public Node set(Variable var, Object val) {
        return setBinding(declaration(), Map.of(Entry.of(var, val)), false);
    }

    public Node setBinding(Map<Variable, Object> vars) {
        return setBinding(declaration(), vars, false);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Node setTypeArgs(Map<Variable, Type> typeArgs) {
        return setBinding(declaration(), (Map) typeArgs, true);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected Node setBinding(Node declaration, Map<Variable, Object> vars, boolean setFunctorOrType) {
        Object[] array = null;
        for (int i = 0; i < length(); i++) {
            Object thisVal = get(i);
            Object bound = setBinding(declaration.get(i), thisVal, vars, i, setFunctorOrType);
            if (!Objects.equals(bound, thisVal)) {
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
                if (var != null) {
                    if (vars.get(var) instanceof Type to) {
                        return thisVar.setType(from.rewrite(to));
                    }
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

    public Node setVariables() {
        Map<Variable, Object> vars = getBinding();
        vars = vars.replaceAll(e -> e.getValue() instanceof Type ? Entry.of(e.getKey(), e.getKey()) : e);
        return setBinding(vars);
    }

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    public Node makeVariablesUnique() throws ParseException {
        assert this == declaration();
        int id = COUNTER.getAndIncrement();
        return replace(n -> {
            if (n instanceof Variable v) {
                return v.makeUnique(id);
            }
            return n;
        }).resetDeclaration();
    }

    public Node setTypes() {
        return setBinding(getBinding());
    }

    public final Node replace(ThrowingFunction<Node, Node> replacer) throws ParseException {
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
        return replacer.apply(array != null ? setArgs(array) : this);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object replace(Object from, ThrowingFunction<Node, Node> replacer) throws ParseException {
        if (from instanceof Node fromNode) {
            return fromNode.replace(replacer);
        } else if (from instanceof ContainingCollection fromList) {
            ContainingCollection toList = fromList.clear();
            for (Object e : fromList) {
                toList = toList.add(replace(e, replacer));
            }
            return fromList.equals(toList) ? fromList : toList;
        }
        return from;
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
                TokenType tt = type.tokenType();
                if (tt != null) {
                    next = new MatchState<>(tt, next);
                } else {
                    next = new MatchState<>(type, next);
                }
                break;
            }
            case Variable var -> {
                Type type = var.type();
                TokenType tt = type.tokenType();
                if (tt != null) {
                    next = new MatchState<>(tt, next);
                } else {
                    next = new MatchState<>(type, next);
                }
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

    public Node init(KnowledgeBase knowledgeBase, ParseContext ctx, ConstructionReason reason) throws ParseException {
        Node rewrite = this;
        if (reason == ConstructionReason.parsing && Type.ROOT.isAssignableFrom(type())) {
            MutableMap<Variable, Type> typeArgs = MutableMap.of(Map.of());
            for (Transform transform : knowledgeBase.getTransforms(this, typeArgs)) {
                rewrite = transform.transform(transform.source(), this, rewrite, knowledgeBase, ctx);
            }
            if (!typeArgs.isEmpty()) {
                rewrite = rewrite.setTypeArgs(typeArgs.get());
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
        return this; // type.equals(type()) ? this : setFunctorOrType(type);
    }

}
