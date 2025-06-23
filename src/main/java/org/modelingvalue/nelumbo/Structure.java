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

import java.util.Objects;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.struct.impl.StructImpl;
import org.modelingvalue.collections.util.StringUtil;

public class Structure extends StructImpl {
    private static final long      serialVersionUID = 7315776001191198132L;
    public static final Type       TYPE             = new Type(Structure.class);

    protected static final boolean TRACE_NELUMBO    = java.lang.Boolean.getBoolean("TRACE_NELUMBO");
    protected static final boolean RANDOM_NELUMBO   = java.lang.Boolean.getBoolean("RANDOM_NELUMBO");
    protected static final boolean REVERSE_NELUMBO  = java.lang.Boolean.getBoolean("REVERSE_NELUMBO");

    private int                    hashCode         = 0;
    private Map<Variable, Object>  variables;
    private int                    nrOfUnbound      = -1;

    public Structure(Functor functor, Object... args) {
        super(array(functor, args));
        init();
    }

    protected Structure(Type type, Object... args) {
        super(array(type, args));
        init();
    }

    protected Structure(Object[] args) {
        super(args);
    }

    private void init() {
        for (int i = 1; i < length(); i++) {
            Object e = get(i);
            if (e instanceof Predicate) {
                ((Predicate) e).init(this, i);
            }
        }
    }

    @Override
    public final int hashCode() {
        if (hashCode == 0) {
            int r = 1;
            for (int i = 1; i < length(); i++) {
                Object e = get(i);
                r = 31 * r + (e == null ? 0 : e.hashCode());
            }
            r = 31 * r + get(0).hashCode();
            hashCode = r == 0 ? 1 : r;
        }
        return hashCode;
    }

    public final Map<Variable, Object> variables() {
        if (variables == null) {
            Map<Variable, Object> vars = Map.of();
            for (int i = 1; i < length(); i++) {
                Object val = get(i);
                if (val instanceof Variable) {
                    vars = vars.put((Variable) val, ((Variable) val).type());
                } else if (val instanceof Structure) {
                    vars = vars.putAll(((Structure) val).variables());
                }
            }
            variables = vars;
        }
        return variables;
    }

    protected final int nrOfUnbound() {
        if (nrOfUnbound < 0) {
            int nr = 0;
            for (int i = 1; i < length(); i++) {
                Object val = get(i);
                if (val instanceof Type) {
                    nr++;
                } else if (val instanceof Structure) {
                    nr += ((Structure) val).nrOfUnbound();
                }
            }
            nrOfUnbound = nr;
        }
        return nrOfUnbound;
    }

    protected final boolean isFullyBound() {
        return nrOfUnbound() == 0;
    }

    @Override
    public String toString() {
        String string = super.toString();
        string = string.substring(1, string.length() - 1);
        int i = string.indexOf(',');
        return i >= 0 ? string.substring(0, i) + "(" + string.substring(i + 1) + ")" : string + "()";
    }

    public final String toString(int i) {
        return StringUtil.toString(get(i));
    }

    private static final Object[] array(Object functor, Object[] args) {
        Object[] result = new Object[args.length + 1];
        result[0] = functor;
        for (int i = 0; i < args.length; i++) {
            result[i + 1] = args[i];
        }
        return result;
    }

    protected Type type() {
        Object t = get(0);
        return t instanceof Functor ? ((Functor) t).resultType() : (Type) t;
    }

    public Functor functor() {
        Object t = get(0);
        return t instanceof Functor ? (Functor) t : null;
    }

    public Structure is(Structure other) {
        if (equals(other)) {
            return this;
        } else if (length() != other.length()) {
            return null;
        }
        Object[] array = null;
        for (int i = 0; i < length(); i++) {
            Object thisVal = get(i);
            Object is = is(thisVal, other.get(i));
            if (is == null) {
                return null;
            } else if (!Objects.equals(is, thisVal)) {
                if (array == null) {
                    array = toArray();
                }
                array[i] = is;
            }
        }
        return array != null ? struct(array) : this;
    }

    private static Object is(Object thisVal, Object otherVal) {
        if (thisVal != otherVal) {
            if (thisVal instanceof Structure && otherVal instanceof Type) {
                return ((Type) otherVal).isAssignableFrom(((Structure) thisVal).type()) ? thisVal : null;
            } else if (thisVal instanceof Type && otherVal instanceof Structure) {
                return ((Type) thisVal).isAssignableFrom(((Structure) otherVal).type()) ? otherVal : null;
            } else if (thisVal instanceof Structure && otherVal instanceof Structure) {
                return ((Structure) thisVal).is((Structure) otherVal);
            } else if (!(thisVal instanceof Type) && otherVal instanceof Type) {
                return ((Type) otherVal).isAssignableFrom(thisVal.getClass()) ? thisVal : null;
            } else if (thisVal instanceof Type && !(otherVal instanceof Type)) {
                return ((Type) thisVal).isAssignableFrom(otherVal.getClass()) ? otherVal : null;
            } else if (!Objects.equals(thisVal, otherVal)) {
                return null;
            }
        }
        return thisVal;
    }

    public Map<Terminal, int[]> terminals() {
        Map<Terminal, int[]> terminals = Map.of();
        for (int i = 1; i < length(); i++) {
            Object val = get(i);
            if (val instanceof Terminal) {
                terminals = terminals.put((Terminal) val, new int[]{i});
            } else if (val instanceof Structure && !(val instanceof Variable)) {
                int ii = i;
                terminals = terminals.putAll(((Structure) val).terminals().replaceAll(e -> {
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
            val = ((Structure) val).get(i);
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

    public Structure set(int f, Object... a) {
        Object[] array = setArray(f, a);
        return array != null ? struct(array) : this;
    }

    public Structure set(int[] idx, Object val) {
        return set(0, idx, val);
    }

    private Structure set(int ii, int[] idx, Object val) {
        Object[] array = toArray();
        if (ii < idx.length - 1) {
            Structure s = (Structure) array[idx[ii]];
            array[idx[ii]] = s.set(ii + 1, idx, val);
        } else {
            array[idx[ii]] = val;
        }
        return struct(array);
    }

    protected Structure replace(Structure from, Structure to) {
        if (equals(from)) {
            return to;
        } else {
            Object[] array = null;
            for (int i = 0; i < length(); i++) {
                Object thisVal = get(i);
                if (thisVal instanceof Structure) {
                    Structure toVal = ((Structure) thisVal).replace(from, to);
                    if (toVal != thisVal) {
                        if (array == null) {
                            array = toArray();
                        }
                        array[i] = toVal;
                    }
                }
            }
            return array != null ? struct(array) : this;
        }
    }

    @SuppressWarnings("unchecked")
    protected Structure struct(Object[] array) {
        return new Structure(array);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected final Object get(Structure declaration, Variable var) {
        for (int i = 1; i < length(); i++) {
            Object thisVal = get(i);
            Object declVal = declaration.get(i);
            if (declVal.equals(var)) {
                return thisVal;
            } else if (thisVal instanceof Structure) {
                Object varVal = ((Structure) thisVal).get((Structure) declVal, var);
                if (varVal != null) {
                    return varVal;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    protected final Map<Variable, Object> getBinding(Structure declaration, Map<Variable, Object> vars, boolean check) {
        if (get(0).equals(declaration.get(0))) {
            for (int i = 1; i < length(); i++) {
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Map<Variable, Object> getBinding(Object declVal, Object thisVal, Map<Variable, Object> vars, boolean check) {
        Type thisType = typeOf(thisVal);
        thisVal = thisVal instanceof Type ? null : thisVal;
        if (declVal instanceof Variable) {
            Variable var = (Variable) declVal;
            Object varVal = vars.get(var);
            Type varType = typeOf(varVal);
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
        } else if (declVal instanceof Structure) {
            Structure declStruct = (Structure) declVal;
            if (thisVal != null) {
                if (thisVal instanceof Structure) {
                    vars = ((Structure) thisVal).getBinding(declStruct, vars, check);
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
        return v instanceof Structure ? ((Structure) v).type() : v instanceof Type ? (Type) v : null;
    }

    protected final Structure set(Structure declaration, Variable var, Object val) {
        return setBinding(declaration, Map.of(Entry.of(var, val)));
    }

    @SuppressWarnings("rawtypes")
    protected final Structure setBinding(Structure declaration, Map<Variable, Object> vars) {
        Object[] array = null;
        for (int i = 1; i < length(); i++) {
            Object thisVal = get(i);
            Object bound = setBinding(declaration.get(i), thisVal, vars);
            if (!Objects.equals(bound, thisVal)) {
                if (array == null) {
                    array = toArray();
                }
                array[i] = bound;
            }
        }
        return array != null ? struct(array) : this;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object setBinding(Object declVal, Object thisVal, Map<Variable, Object> vars) {
        if (declVal instanceof Variable) {
            Object varVal = vars.get((Variable) declVal);
            if (varVal != null) {
                return varVal;
            }
        } else if (declVal instanceof Structure) {
            if (thisVal instanceof Type && ((Type) thisVal).isAssignableFrom((((Structure) declVal).type()))) {
                return ((Structure) declVal).setBinding((Structure) declVal, vars);
            } else if (thisVal instanceof Structure) {
                return ((Structure) thisVal).setBinding((Structure) declVal, vars);
            }
        }
        return thisVal;
    }

    protected final int depth() {
        int result = 1;
        for (int i = 1; i < length(); i++) {
            Object v = get(i);
            if (v instanceof Structure && !((Structure) v).atomic()) {
                result = Math.max(result, ((Structure) v).depth() + 1);
            }
        }
        return result;
    }

    protected final Object[] signatureArray(int depth) {
        Object[] array = null;
        for (int i = 1; i < length(); i++) {
            Object v = get(i);
            Object r = v;
            if (depth > 1 && v instanceof Structure && !((Structure) v).atomic()) {
                r = ((Structure) v).signature(depth - 1);
            } else {
                r = typeOf(v);
            }
            if (!Objects.equals(v, r)) {
                if (array == null) {
                    array = toArray();
                }
                array[i] = r;
            }
        }
        return array;
    }

    protected Structure signature(int depth) {
        Object[] array = signatureArray(depth);
        return array != null ? struct(array) : this;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected Set<? extends Structure> generalize(boolean full) {
        Set<Structure> result = Set.of();
        for (int i = 1; i < length(); i++) {
            Object v = get(i);
            if (full && v instanceof Type) {
                List<Type> args = functor().args();
                for (Type s : KnowledgeBase.generalizations((Type) v, args.get(i - 1))) {
                    result = result.add(setType(i, s));
                }
            } else if (v instanceof Structure) {
                Set<? extends Structure> gen = ((Structure) v).generalize(full);
                for (Structure s : gen) {
                    result = result.add(setTyped(i, s));
                }
                if (gen.isEmpty()) {
                    result = result.add(setType(i, typeOf(v)));
                }
            }
        }
        return result;
    }

    protected Structure setType(int i, Type type) {
        return set(i, type);
    }

    protected Structure setTyped(int i, Structure typed) {
        return set(i, typed);
    }

    protected boolean atomic() {
        for (int i = 1; i < length(); i++) {
            Object v = get(i);
            if (!(v instanceof Structure)) {
                return true;
            }
        }
        return false;
    }

}
