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

import java.io.Serial;
import java.util.Objects;
import java.util.Optional;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.struct.impl.StructImpl;
import org.modelingvalue.collections.util.StringUtil;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ThrowingFunction;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;

@SuppressWarnings("unused")
public class Node extends StructImpl implements AstElement {
    @Serial
    private static final   long                  serialVersionUID = 7315776001191198132L;
    protected static final int                   START            = 2;
    //
    private final          Node                  declaration;
    private                Map<Variable, Object> binding;
    private                boolean               hashCodeIsCached;
    private                int                   hashCodeCache;

    private Map<Functor, List<Integer>> branches;
    private int                         cycleDepth;

    public Node(Functor functor, List<AstElement> elements, Object... args) {
        super(array(functor, elements, args));
        this.declaration = this;
        init(elements);
    }

    public Node(Type type, List<AstElement> elements, Object... args) {
        super(array(type, elements, args));
        this.declaration = this;
        init(elements);
    }

    protected Node(Object[] args, Node declaration) {
        super(args);
        this.declaration = declaration == null ? this : declaration;
    }

    public Node declaration() {
        return declaration;
    }

    public Node resetDeclaration() {
        Object[] array = toArray();
        for (int i = START; i < array.length; i++) {
            array[i] = resetDeclaration(array[i]);
        }
        return struct(array, null);
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

    private static Object[] array(Object functor, List<AstElement> elements, Object[] args) {
        Object[] result = new Object[START + args.length];
        result[0] = functor;
        result[1] = elements;
        System.arraycopy(args, 0, result, START, args.length);
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Optional<?> opt) {
                result[i + START] = opt.orElse(null);
            }
        }
        return result;
    }

    public Node setFunctor(Functor functor) {
        Object[] array = toArray();
        array[0] = functor;
        return struct(array, null);
    }

    public Node setAstElements(List<AstElement> elements) {
        Object[] array = toArray();
        array[1] = elements;
        Node node = struct(array);
        node.init(elements);
        return node;
    }

    private void init(List<AstElement> elements) {
        for (AstElement e : elements) {
            if (e instanceof Token token) {
                token.setNode(this);
            }
        }
    }

    @Override
    public int length() {
        return super.length() - START;
    }

    @Override
    public Object get(int i) {
        return super.get(i + START);
    }

    public Type type() {
        Node tf = typeOrFunctor();
        return tf instanceof Functor ? ((Functor) tf).resultType() : (Type) tf;
    }

    public Functor functor() {
        Node tf = typeOrFunctor();
        return tf instanceof Functor ? (Functor) tf : null;
    }

    public Node typeOrFunctor() {
        return (Node) super.get(0);
    }

    @SuppressWarnings("unchecked")
    public final List<AstElement> astElements() {
        return (List<AstElement>) super.get(1);
    }

    @SuppressWarnings("unchecked")
    public List<Object> args() {
        List<Object> args = List.of();
        for (int i = 0; i < length(); i++) {
            Object a = get(i);
            args = args.add(a == null ? Optional.empty() : a);
        }
        return args;
    }

    @Override
    public int hashCode() {
        if (!hashCodeIsCached) {
            int r = 1;
            for (int i = 0; i < length(); i++) {
                Object e = get(i);
                r = 31 * r + (e == null ? 0 : e.hashCode());
            }
            r                = 31 * r + typeForEquals().hashCode();
            hashCodeCache    = r == 0 ? 1 : r;
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

    protected Node typeForEquals() {
        return typeOrFunctor();
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

    public Map<Terminal, int[]> terminals() {
        Map<Terminal, int[]> terminals = Map.of();
        for (int i = 0; i < length(); i++) {
            Object val = get(i);
            if (val instanceof Terminal terminal) {
                terminals = terminals.put(terminal, new int[]{i});
            } else if (val instanceof Node node && !(node instanceof Variable) && !(node instanceof Type)) {
                int ii = i;
                terminals = terminals.putAll(node.terminals().replaceAll(e -> {
                    int[] value = e.getValue();
                    int[] idx   = new int[value.length + 1];
                    System.arraycopy(value, 0, idx, 1, value.length);
                    idx[0] = ii;
                    return Entry.of(e.getKey(), idx);
                }));
            }
        }
        return terminals;
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
                array[i + f + START] = a[i];
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
        int      i     = idx[ii] + START;
        if (ii < idx.length - 1) {
            Node s = (Node) array[i];
            array[i] = s.set(ii + 1, idx, val);
        } else {
            array[i] = val;
        }
        return struct(array);
    }

    public final Node struct(Object[] array) {
        return struct(array, declaration);
    }

    protected Node struct(Object[] array, Node declaration) {
        return new Node(array, declaration);
    }

    public Object get(Variable var) {
        return get(declaration, var);
    }

    protected final Object get(Node declaration, Variable var) {
        for (int i = 0; i < length(); i++) {
            Object thisVal = get(i);
            Object declVal = declaration.get(i);
            if (declVal.equals(var)) {
                return thisVal;
            } else if (thisVal instanceof Node && !(thisVal instanceof Type)) {
                Object varVal = ((Node) thisVal).get((Node) declVal, var);
                if (varVal != null) {
                    return varVal;
                }
            }
        }
        return null;
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
        } else if (declVal instanceof Node declNode && !(declVal instanceof Type) && //
                   thisVal instanceof Node thisNode && !(thisVal instanceof Type)) {
            vars = thisNode.getBinding(declNode, vars);
        } else if (declVal instanceof List<?> declList && thisVal instanceof List<?> thisList && //
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
            Object bound   = setBinding(declaration.get(i), thisVal, vars, i);
            if (!Objects.equals(bound, thisVal)) {
                if (array == null) {
                    array = toArray();
                }
                array[i + START] = bound;
            }
        }
        return array != null ? struct(array, declaration) : this;
    }

    protected final Object setBinding(Object declVal, Object thisVal, Map<Variable, Object> vars, int i) {
        if (declVal instanceof Variable declVar) {
            Object varVal = vars.get(declVar);
            if (varVal == null) {
                String name = declVar.name();
                if (name.startsWith("<")) {
                    declVar = declVar.rename(name.substring(1, name.length() - 1));
                    varVal  = vars.get(declVar);
                    if (varVal instanceof Variable valVar) {
                        varVal = valVar.rename("<" + valVar.name() + ">");
                    }
                }
            }
            if (varVal != null && doSetBinding(varVal, i)) {
                return varVal;
            }
            if (thisVal instanceof Variable thisVar) {
                Type     from = thisVar.type();
                Variable var  = from.variable();
                if (var != null) {
                    if (vars.get(var) instanceof Type to) {
                        return thisVar.setType(from.rewrite(to));
                    }
                }
            }
        } else if (declVal instanceof Node declNode && !(declNode instanceof Type) && //
                   thisVal instanceof Node thisNode && !(thisNode instanceof Type)) {
            return thisNode.setBinding(declNode, vars);
        } else if (declVal instanceof List<?> declList && thisVal instanceof List<?> thisList && //
                   declList.size() == thisList.size()) {
            List<Object> list = List.of();
            for (int ii = 0; ii < declList.size(); ii++) {
                list = list.add(setBinding(declList.get(ii), thisList.get(ii), vars, i));
            }
            return thisList.equals(list) ? thisList : list;
        } else if (declVal instanceof Type declType && thisVal instanceof Type thisType) {
            Variable declVar = declType.variable();
            if (declVar != null) {
                Object varVal = vars.get(declVar);
                if (varVal == null) {
                    String name = declVar.name();
                    if (name.startsWith("<")) {
                        declVar = declVar.rename(name.substring(1, name.length() - 1));
                        varVal  = vars.get(declVar);
                    }
                }
                if (varVal instanceof Type type) {
                    return thisType.rewrite(type);
                } else if (varVal instanceof Variable valVar) {
                    return new Type(valVar);
                }
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

    protected Node replace(ThrowingFunction<Node, Node> replacer) throws ParseException {
        Node to = replacer.apply(this);
        if (to != this) {
            return to;
        } else {
            Object[] array = null;
            for (int i = 0; i < length(); i++) {
                Object fromVal = get(i);
                Object toVal   = replace(fromVal, replacer);
                if (toVal != fromVal) {
                    if (array == null) {
                        array = toArray();
                    }
                    array[i + START] = toVal;
                }
            }
            return array != null ? struct(array) : this;
        }
    }

    private Object replace(Object from, ThrowingFunction<Node, Node> replacer) throws ParseException {
        if (from instanceof Node fromNode) {
            return fromNode.replace(replacer);
        } else if (from instanceof List fromList) {
            List<Object> toList = List.of();
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

    @Override
    public boolean isMeta() {
        return false;
    }

    @Override
    public int getCycleDepth() {
        return cycleDepth;
    }

    @Override
    public void setCycleDepth(int cycleDepth) {
        this.cycleDepth = cycleDepth;
    }

    @Override
    public List<Integer> getBranches(Functor functor) {
        return branches.get(functor);
    }

    @Override
    public void setBranches(Map<Functor, List<Integer>> branches) {
        this.branches = branches;
    }

    public <E> MatchState<E> state(MatchState<E> state) {
        for (Object arg : args().reverse()) {
            switch (arg) {
                case Type type -> {
                    TokenType tt = type.tokenType();
                    if (tt != null) {
                        state = new MatchState<>(tt, state);
                    } else {
                        state = new MatchState<>(type, state);
                    }
                }
                case Variable var -> {
                    Type      type = var.type();
                    TokenType tt   = type.tokenType();
                    if (tt != null) {
                        state = new MatchState<>(tt, state);
                    } else {
                        state = new MatchState<>(type, state);
                    }
                }
                case Node node -> {
                    state = node.state(state);
                }
                default -> {
                    state = new MatchState<>(arg.getClass(), state);
                }
            }
        }
        Functor functor = functor();
        assert functor != null;
        return new MatchState<>(functor, state);
    }

    public Node init(KnowledgeBase knowledgeBase) throws ParseException {
        for (Transform transform : knowledgeBase.getTransforms(this)) {
            transform.rewrite(transform.source(), this, knowledgeBase);
        }
        return this;
    }

    public Variable variable() {
        return null;
    }

}
