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
import java.util.Objects;
import java.util.function.Function;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.struct.impl.StructImpl;
import org.modelingvalue.collections.util.StringUtil;
import org.modelingvalue.nelumbo.syntax.Token;

public class Node extends StructImpl {
    @Serial
    private static final long serialVersionUID = 7315776001191198132L;

    private         int                   hashCode    = 0;
    private         Map<Variable, Object> variables;
    private         int                   nrOfUnbound = -1;
    protected final int                   start;

    public Node(Functor functor, Token[] tokens, Object... args) {
        super(array(functor, tokens, args));
        start = tokens.length + 1;
    }

    public Node(Type type, Token[] tokens, Object... args) {
        super(array(type, tokens, args));
        start = tokens.length + 1;
    }

    private static Object[] array(Object functor, Token[] tokens, Object[] args) {
        Object[] result = new Object[1 + tokens.length + args.length];
        result[0] = functor;
        System.arraycopy(tokens, 0, result, 1, tokens.length);
        System.arraycopy(args, 0, result, 1 + tokens.length, args.length);
        return result;
    }

    protected Node(Object[] args, int start) {
        super(args);
        this.start = start;
        if (get(0) instanceof Token) {
            System.err.println("WARNING: Node.get(0) is a Token... is this an error??");
        }
    }

    public Node setTokens(Token... tokens) {
        Object[] result = new Object[1 + tokens.length + length()];
        result[0] = typeOrFunctor();
        System.arraycopy(tokens, 0, result, 1, tokens.length);
        for (int i = 0; i < length(); i++) {
            result[1 + tokens.length + i] = get(i);
        }
        return struct(result, tokens.length + 1);
    }

    @Override
    public int length() {
        return super.length() - start;
    }

    @Override
    public Object get(int i) {
        return super.get(i + start);
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

    public Token[] tokens() {
        Token[] tokens = new Token[start - 1];
        for (int i = 1; i < start; i++) {
            tokens[i - 1] = (Token) super.get(i);
        }
        return tokens;
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            int r = 1;
            for (int i = 0; i < length(); i++) {
                Object e = get(i);
                r = 31 * r + (e == null ? 0 : e.hashCode());
            }
            r        = 31 * r + super.get(0).hashCode();
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
        if (!typeOrFunctor().equals(other.typeOrFunctor())) {
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

    public final Map<Variable, Object> variables() {
        if (variables == null) {
            Map<Variable, Object> vars = Map.of();
            for (int i = 0; i < length(); i++) {
                Object val = get(i);
                if (val instanceof Variable) {
                    vars = vars.put((Variable) val, ((Variable) val).type());
                } else if (val instanceof Node && !(val instanceof Type)) {
                    vars = vars.putAll(((Node) val).variables());
                }
            }
            variables = vars;
        }
        return variables;
    }

    protected final int nrOfUnbound() {
        if (nrOfUnbound < 0) {
            int nr = 0;
            for (int i = 0; i < length(); i++) {
                Object val = get(i);
                if (val instanceof Type) {
                    nr++;
                } else if (val instanceof Node && !(val instanceof Terminal)) {
                    nr += ((Node) val).nrOfUnbound();
                }
            }
            nrOfUnbound = nr;
        }
        return nrOfUnbound;
    }

    protected final boolean isFullyBound() {
        return nrOfUnbound() == 0;
    }

    public Function<Node, String> render() {
        Functor functor = functor();
        return functor != null ? functor.render() : null;
    }

    public int precedence() {
        return functor().precedence();
    }

    @Override
    public String toString() {
        Function<Node, String> render = render();
        if (render != null) {
            return render.apply(this);
        }
        Functor       functor = functor();
        StringBuilder sb      = new StringBuilder((functor != null ? functor.name() : type().name()) + "(");
        String        sep     = "";
        for (int i = 0; i < length(); i++) {
            sb.append(sep).append(toString(i));
            sep = ",";
        }
        return sb.append(")").toString();
    }

    public final String toString(int i) {
        Object v      = get(i);
        String string = StringUtil.toString(v);
        if (v instanceof Node && render() != null && ((Node) v).render() != null) {
            if (!functor().equals(((Node) v).functor()) && precedence() >= ((Node) v).precedence()) {
                return "(" + string + ")";
            }
        }
        return string;
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
                array[i + f + start] = a[i];
            }
        }
        return array;
    }

    public Node set(int f, Object... a) {
        Object[] array = setArray(f, a);
        return array != null ? struct(array, start) : this;
    }

    public Node set(int[] idx, Object val) {
        return set(0, idx, val);
    }

    private Node set(int ii, int[] idx, Object val) {
        Object[] array = toArray();
        int      i     = idx[ii] + start;
        if (ii < idx.length - 1) {
            Node s = (Node) array[i];
            array[i] = s.set(ii + 1, idx, val);
        } else {
            array[i] = val;
        }
        return struct(array, start);
    }

    protected Node struct(Object[] array, int start) {
        return new Node(array, start);
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

    protected final Map<Variable, Object> getBinding(Node declaration, Map<Variable, Object> vars, boolean check) {
        if (typeOrFunctor().equals(declaration.typeOrFunctor())) {
            for (int i = 0; i < length(); i++) {
                vars = getBinding(declaration.get(i), get(i), vars, check);
                if (vars == null) {
                    return null;
                }
            }
            return vars;
        } else {
            return null;
        }
    }

    private static Map<Variable, Object> getBinding(Object declVal, Object thisVal, Map<Variable, Object> vars, boolean check) {
        Type thisType = typeOf(thisVal);
        thisVal = thisVal instanceof Type ? null : thisVal;
        if (declVal instanceof Variable var) {
            Object varVal  = vars.get(var);
            Type   varType = typeOf(varVal);
            varVal = varVal instanceof Type ? null : varVal;
            if (varVal != null) {
                if (thisVal != null && !thisVal.equals(varVal)) {
                    return null;
                }
            } else if (thisVal != null) {
                if (var.type().isAssignableFrom(thisType)) {
                    vars = vars.put(var, thisVal);
                } else {
                    return null;
                }
            } else if (thisType == null || !var.type().isAssignableFrom(thisType)) {
                return null;
            } else if (varType != null && !varType.equals(thisType)) {
                return null;
            } else {
                vars = vars.put(var, thisType);
            }
        } else if (declVal instanceof Node declStruct && !(declVal instanceof Type)) {
            if (thisVal != null) {
                if (thisVal instanceof Node && !(thisVal instanceof Type)) {
                    vars = ((Node) thisVal).getBinding(declStruct, vars, check);
                } else {
                    return null;
                }
            } else if (thisType == null || !declStruct.type().isAssignableFrom(thisType)) {
                return null;
            }
        } else if (check && thisVal != null && !thisVal.equals(declVal)) {
            return null;
        }
        return vars;
    }

    public static Type typeOf(Object v) {
        return v instanceof Type ? (Type) v : v instanceof Node ? ((Node) v).type() : null;
    }

    protected final Node set(Node declaration, Variable var, Object val) {
        return setBinding(declaration, Map.of(Entry.of(var, val)));
    }

    protected final Node setBinding(Node declaration, Map<Variable, Object> vars) {
        Object[] array = null;
        for (int i = 0; i < length(); i++) {
            Object thisVal = get(i);
            Object bound   = setBinding(declaration.get(i), thisVal, vars);
            if (!Objects.equals(bound, thisVal)) {
                if (array == null) {
                    array = toArray();
                }
                array[i + start] = bound;
            }
        }
        return array != null ? struct(array, start) : this;
    }

    private static Object setBinding(Object declVal, Object thisVal, Map<Variable, Object> vars) {
        if (declVal instanceof Variable) {
            Object varVal = vars.get((Variable) declVal);
            if (varVal != null) {
                return varVal;
            }
        } else if (declVal instanceof Node && !(declVal instanceof Type)) {
            if (thisVal instanceof Type && ((Type) thisVal).isAssignableFrom((((Node) declVal).type()))) {
                return ((Node) declVal).setBinding((Node) declVal, vars);
            } else if (thisVal instanceof Node && !(thisVal instanceof Type)) {
                return ((Node) thisVal).setBinding((Node) declVal, vars);
            }
        }
        return thisVal;
    }

    protected final int depth() {
        int result = 1;
        for (int i = 0; i < length(); i++) {
            Object v = get(i);
            if (v instanceof Node && !(v instanceof Type) && !((Node) v).atomic()) {
                result = Math.max(result, ((Node) v).depth() + 1);
            }
        }
        return result;
    }

    protected final Object[] signatureArray(int depth) {
        Object[] array = null;
        for (int i = 0; i < length(); i++) {
            Object v = get(i);
            Object r;
            if (depth > 1 && v instanceof Node && !(v instanceof Type) && !((Node) v).atomic()) {
                r = ((Node) v).signature(depth - 1);
            } else {
                r = typeOf(v);
            }
            if (!Objects.equals(v, r)) {
                if (array == null) {
                    array = toArray();
                }
                array[i + start] = r;
            }
        }
        return array;
    }

    protected Node signature(int depth) {
        Object[] array = signatureArray(depth);
        return array != null ? struct(array, start) : this;
    }

    protected Set<? extends Node> generalize(boolean full) {
        Set<Node> result = Set.of();
        for (int i = 0; i < length(); i++) {
            Object v = get(i);
            if (v instanceof Type) {
                if (full) {
                    List<Type> args = functor().args();
                    for (Type s : KnowledgeBase.generalizations((Type) v, args.get(i))) {
                        result = result.add(setType(i, s));
                    }
                }
            } else if (v instanceof Node) {
                Set<? extends Node> gen = ((Node) v).generalize(full);
                for (Node s : gen) {
                    result = result.add(setTyped(i, s));
                }
                if (gen.isEmpty()) {
                    result = result.add(setType(i, typeOf(v)));
                }
            }
        }
        return result;
    }

    protected Node setType(int i, Type type) {
        return set(i, type);
    }

    protected Node setTyped(int i, Node typed) {
        return set(i, typed);
    }

    protected boolean atomic() {
        for (int i = 0; i < length(); i++) {
            Object v = get(i);
            if (!(v instanceof Node) && !(v instanceof Type)) {
                return true;
            }
        }
        return false;
    }

}
