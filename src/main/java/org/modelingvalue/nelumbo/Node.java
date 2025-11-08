//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2025 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
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
import java.util.function.Function;

import org.modelingvalue.collections.Collection;
import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.struct.impl.StructImpl;
import org.modelingvalue.collections.util.StringUtil;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;

@SuppressWarnings("unused")
public class Node extends StructImpl implements AstElement {
    @Serial
    private static final long           serialVersionUID = 7315776001191198132L;

    protected static final int          START            = 2;

    private int                         hashCode         = 0;
    private Map<Variable, Object>       variables;

    private Map<Functor, List<Integer>> branches;
    private int                         cycleDepth;

    public Node(Functor functor, List<AstElement> elements, Object... args) {
        super(array(functor, elements, args));
        init(elements);
    }

    public Node(Type type, List<AstElement> elements, Object... args) {
        super(array(type, elements, args));
        init(elements);
    }

    protected Node(Object[] args) {
        super(args);
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
        return struct(array);
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
        if (hashCode == 0) {
            int r = 1;
            for (int i = 0; i < length(); i++) {
                Object e = get(i);
                r = 31 * r + (e == null ? 0 : e.hashCode());
            }
            r = 31 * r + typeForEquals().hashCode();
            hashCode = r == 0 ? 1 : r;
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (obj.getClass() != getClass()) {
            return false;
        } else if (super.equals(obj)) {
            return true;
        }
        Node other = (Node) obj;
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

    protected Node typeForEquals() {
        return typeOrFunctor();
    }

    public Map<Variable, Object> shallowVariables() {
        Map<Variable, Object> vars = Map.of();
        for (int i = 0; i < length(); i++) {
            vars = shallowVariables(vars, get(i));
        }
        return vars;
    }

    private static Map<Variable, Object> shallowVariables(Map<Variable, Object> vars, Object val) {
        if (val instanceof Variable) {
            vars = vars.put((Variable) val, ((Variable) val).type());
        } else if (val instanceof Node && !(val instanceof Type)) {
            vars = vars.putAll(((Node) val).shallowVariables());
        } else if (val instanceof Collection<?> coll) {
            for (Object e : coll) {
                vars = shallowVariables(vars, e);
            }
        }
        return vars;
    }

    public final Map<Variable, Object> variables() {
        if (variables == null) {
            Map<Variable, Object> vars = Map.of();
            for (int i = 0; i < length(); i++) {
                vars = variables(vars, get(i));
            }
            variables = vars;
        }
        return variables;
    }

    private static Map<Variable, Object> variables(Map<Variable, Object> vars, Object val) {
        if (val instanceof Variable var) {
            vars = vars.put(var, var.type());
        } else if (val instanceof Node node && !(val instanceof Type)) {
            vars = vars.putAll(node.variables());
        } else if (val instanceof List<?> list) {
            for (Object e : list) {
                vars = variables(vars, e);
            }
        }
        return vars;
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
            if (val instanceof Terminal) {
                terminals = terminals.put((Terminal) val, new int[]{i});
            } else if (val instanceof Node && !(val instanceof Variable) && !(val instanceof Type)) {
                int ii = i;
                terminals = terminals.putAll(((Node) val).terminals().replaceAll(e -> {
                    int[] idx = new int[e.getValue().length + 1];
                    System.arraycopy(e.getValue(), 0, idx, 1, e.getValue().length);
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
        int i = idx[ii] + START;
        if (ii < idx.length - 1) {
            Node s = (Node) array[i];
            array[i] = s.set(ii + 1, idx, val);
        } else {
            array[i] = val;
        }
        return struct(array);
    }

    protected Node struct(Object[] array) {
        return new Node(array);
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

    protected final Map<Variable, Object> getBinding(Node declaration, Map<Variable, Object> vars) {
        for (int i = 0; vars != null && i < length(); i++) {
            vars = getBinding(declaration.get(i), get(i), vars, i);
        }
        return vars;
    }

    private Map<Variable, Object> getBinding(Object declVal, Object thisIn, Map<Variable, Object> vars, int i) {
        Object thisVal = thisIn instanceof Type || thisIn instanceof Variable ? null : thisIn;
        if (declVal instanceof Variable var) {
            Object varVal = vars.get(var);
            varVal = varVal instanceof Type ? null : varVal;
            if (varVal != null) {
                if (thisVal != null && !thisVal.equals(varVal)) {
                    return null;
                }
            } else {
                if (thisVal == null) {
                    thisVal = typeOf(thisIn);
                }
                if (thisVal != null && doGetBinding(thisVal, i)) {
                    vars = vars.put(var, thisVal);
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

    protected final Node set(Node declaration, Variable var, Object val) {
        return setBinding(declaration, Map.of(Entry.of(var, val)));
    }

    protected final Node setBinding(Node declaration, Map<Variable, Object> vars) {
        Object[] array = null;
        for (int i = 0; i < length(); i++) {
            Object thisVal = get(i);
            Object bound = setBinding(declaration.get(i), thisVal, vars, i);
            if (!Objects.equals(bound, thisVal)) {
                if (array == null) {
                    array = toArray();
                }
                array[i + START] = bound;
            }
        }
        return array != null ? struct(array) : this;
    }

    private Object setBinding(Object declVal, Object thisVal, Map<Variable, Object> vars, int i) {
        if (declVal instanceof Variable declVar) {
            Object varVal = vars.get(declVar);
            if (varVal != null && doSetBinding(varVal, i)) {
                return varVal;
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
        }
        return thisVal;
    }

    protected boolean doGetBinding(Object varVal, int i) {
        return true;
    }

    protected boolean doSetBinding(Object varVal, int i) {
        return true;
    }

    protected Node replace(Function<Node, Node> replacer) {
        Node to = replacer.apply(this);
        if (to != this) {
            return to;
        } else {
            Object[] array = null;
            for (int i = 0; i < length(); i++) {
                Object thisVal = get(i);
                if (thisVal instanceof Node fromNode) {
                    Node toNode = fromNode.replace(replacer);
                    if (toNode != fromNode) {
                        if (array == null) {
                            array = toArray();
                        }
                        array[i + START] = toNode;
                    }
                }
            }
            return array != null ? struct(array) : this;
        }
    }

    protected Node setType(int i, Type type) {
        return set(i, type);
    }

    protected Node setTyped(int i, Node typed) {
        return set(i, typed);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean atomic() {
        for (int i = 0; i < length(); i++) {
            Object v = get(i);
            if (!(v instanceof Node)) {
                return true;
            }
        }
        return false;
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
    public List<Integer> getBranche(Functor functor) {
        return branches.get(functor);
    }

    @Override
    public void setBranches(Map<Functor, List<Integer>> branches) {
        this.branches = branches;
    }

    public MatchState state(MatchState state) {
        for (Object arg : args().reverse()) {
            if (arg instanceof Type type) {
                state = new MatchState(type, state);
            } else if (arg instanceof Variable var) {
                state = new MatchState(var.type(), state);
            } else if (arg instanceof Node node) {
                state = node.state(state);
            } else {
                state = new MatchState(arg.getClass(), state);
            }
        }
        Functor functor = functor();
        assert functor != null;
        return new MatchState(functor, state);
    }
}
