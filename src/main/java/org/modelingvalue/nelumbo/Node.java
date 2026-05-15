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
import java.util.Objects;
import java.util.Optional;

import org.modelingvalue.collections.ContainingCollection;
import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.struct.impl.StructImpl;
import org.modelingvalue.collections.util.StringUtil;
import org.modelingvalue.nelumbo.collections.NList;
import org.modelingvalue.nelumbo.lang.Functor;
import org.modelingvalue.nelumbo.lang.FunctorOrType;
import org.modelingvalue.nelumbo.lang.Transform;
import org.modelingvalue.nelumbo.lang.Type;
import org.modelingvalue.nelumbo.lang.Variable;
import org.modelingvalue.nelumbo.logic.Predicate;
import org.modelingvalue.nelumbo.syntax.ParseContext;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ThrowingFunction;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class Node extends StructImpl implements AstElement {
    @Serial
    private static final long serialVersionUID = 7315776001191198132L;
    //
    private final FunctorOrType    functorOrType;
    private final List<AstElement> elements;
    private final Node             declaration;
    //
    private Map<Variable, Object> binding;
    private boolean               hashCodeIsCached;
    private int                   hashCodeCache;

    @NelumboConstructor
    public Node(FunctorOrType functorOrType, List<AstElement> elements, Node declaration, Object... args) {
        super(array(args));
        this.functorOrType = functorOrType;
        this.elements = elements;
        this.declaration = declaration == null ? this : declaration;
    }

    public Node declaration() {
        return declaration;
    }

    public Node resetDeclaration() {
        Object[] array = toArray();
        for (int i = 0; i < array.length; i++) {
            array[i] = resetDeclaration(array[i]);
        }
        return struct(array, functorOrType, elements, null);
    }

    private Object resetDeclaration(Object from) {
        if (from instanceof Node node) {
            return node.resetDeclaration();
        } else if (from instanceof List<?> list) {
            List<Object> l = List.of();
            for (Object e : list) {
                l = l.add(resetDeclaration(e));
            }
            return l;
        }
        return from;
    }

    private static Object[] array(Object[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Optional<?> opt) {
                args[i] = opt.orElse(null);
            }
        }
        return args;
    }

    public Node setFunctor(Functor functor) {
        return struct(toArray(), functor, elements, declaration);
    }

    public Node setAstElements(List<AstElement> elements) {
        return struct(toArray(), functorOrType, elements, declaration);
    }

    public Type type() {
        FunctorOrType tf = functorOrType();
        return tf instanceof Functor ? ((Functor) tf).resultType() : (Type) tf;
    }

    public Functor functor() {
        FunctorOrType tf = functorOrType();
        return tf instanceof Functor ? (Functor) tf : null;
    }

    public FunctorOrType functorOrType() {
        return functorOrType;
    }

    public final List<AstElement> astElements() {
        return elements;
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
        if (!hashCodeIsCached) {
            int r = 1;
            for (int i = 0; i < length(); i++) {
                Object e = get(i);
                r = 31 * r + (e == null ? 0 : e.hashCode());
            }
            r = 31 * r + typeForEquals().hashCode();
            hashCodeCache = r == 0 ? 1 : r;
            hashCodeIsCached = true;
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
        } else if (super.equals(obj)) {
            return true;
        } else {
            if (!typeForEquals().equals(other.typeForEquals())) {
                return false;
            } else if (length() != other.length()) {
                return false;
            } else {
                for (int i = 0; i < length(); i++) {
                    if (!Objects.equals(get(i), other.get(i))) {
                        return false;
                    }
                }
                return true;
            }
        }
    }

    protected Object typeForEquals() {
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
        return array != null ? struct(array) : this;
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
        return struct(array);
    }

    public final Node struct(Object[] array) {
        return struct(array, functorOrType, elements, declaration);
    }

    protected Node struct(Object[] args, FunctorOrType functorOrType, List<AstElement> elements, Node declaration) {
        return new Node(functorOrType, elements, declaration, args);
    }

    public final Set<Variable> allLocalVars() {
        Set<Variable> allLocalVars = localVars().asSet();
        for (int i = 0; i < length(); i++) {
            allLocalVars = allLocalVars(get(i), allLocalVars);
        }
        return allLocalVars;
    }

    private Set<Variable> allLocalVars(Object val, Set<Variable> allLocalVars) {
        if (val instanceof Node node) {
            allLocalVars = allLocalVars.addAll(node.allLocalVars());
        } else if (val instanceof List<?> list) {
            for (Object e : list) {
                allLocalVars = allLocalVars(e, allLocalVars);
            }
        }
        return allLocalVars;
    }

    public final Map<Variable, Object> getBinding() {
        if (binding == null) {
            binding = getBinding(declaration);
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
                    if (thisVal instanceof Node thisNode) {
                        vars = vars.putAll(thisNode.getBinding().replaceAll(e -> {
                            Variable nodeVar = e.getKey();
                            return Entry.of(nodeVar.rename("$" + nodeVar.name()), e.getValue());
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

    protected Node set(Variable var, Object val) {
        return setBinding(declaration, Map.of(Entry.of(var, val)));
    }

    public Node setBinding(Map<Variable, Object> vars) {
        return setBinding(declaration, vars);
    }

    protected Node setBinding(Node declaration, Map<Variable, Object> vars) {
        Object[] array = null;
        for (int i = 0; i < length(); i++) {
            Object thisVal = get(i);
            Object bound = setBinding(declaration.get(i), thisVal, vars, i);
            if (!Objects.equals(bound, thisVal)) {
                if (array == null) {
                    array = toArray();
                }
                array[i] = bound;
            }
        }
        return array != null ? struct(array, functorOrType, elements, declaration) : this;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected final Object setBinding(Object declVal, Object thisVal, Map<Variable, Object> vars, int i) {
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
            return thisNode.setBinding(declNode, vars);
        } else if (declVal instanceof ContainingCollection declList && thisVal instanceof ContainingCollection thisList
                && //
                declList.size() == thisList.size()) {
            ContainingCollection list = declList.clear();
            for (int ii = 0; ii < declList.size(); ii++) {
                list = list.add(setBinding(declList.get(ii), thisList.get(ii), vars, i));
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
                return thisType.setBinding(declType, vars);
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

    public final Node replace(ThrowingFunction<Node, Node> replacer) throws ParseException {
        Node to = replacer.apply(this);
        if (to != this) {
            return to;
        } else {
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
            return array != null ? struct(array) : this;
        }
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

    public <E> MatchState<E> state(MatchState<E> state) {
        for (Object arg : args().reverse()) {
            switch (arg) {
            case Type type    -> {
                TokenType tt = type.tokenType();
                if (tt != null) {
                    state = new MatchState<>(tt, state);
                } else {
                    Variable var = type.variable();
                    if (var != null) {
                        state = new MatchState<>(var.type(), state);
                    } else {
                        state = new MatchState<>(type, state);
                    }
                }
            }
            case Variable var -> {
                Type type = var.type();
                TokenType tt = type.tokenType();
                if (tt != null) {
                    state = new MatchState<>(tt, state);
                } else {
                    state = new MatchState<>(type, state);
                }
            }
            case Node node    -> {
                state = node.state(state);
            }
            default           -> {
                state = new MatchState<>(arg.getClass(), state);
            }
            }
        }
        Functor functor = functor();
        assert functor != null;
        return new MatchState<>(functor, state);
    }

    public Node init(KnowledgeBase knowledgeBase, ParseContext ctx, ConstructionReason reason) throws ParseException {
        Node rewrite = this;
        if (reason == ConstructionReason.parsing && Type.ROOT.isAssignableFrom(type())) {
            for (Transform transform : knowledgeBase.getTransforms(this)) {
                rewrite = transform.transform(transform.source(), this, rewrite, knowledgeBase, ctx);
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

    protected final Predicate predicate(int i) {
        return (Predicate) get(i);
    }

    public Node add(Node added) {
        return new NList(Type.ROOT, List.of(this, added), List.of(this, added));
    }

}
