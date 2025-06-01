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

package org.modelingvalue.nelumbo.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;

import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.Set;
import org.modelingvalue.collections.util.StringUtil;
import org.modelingvalue.nelumbo.Logic.Constant;
import org.modelingvalue.nelumbo.Logic.Functor;
import org.modelingvalue.nelumbo.Logic.RenderLambda;
import org.modelingvalue.nelumbo.Logic.Structure;

public abstract class StructureImpl<F extends Structure> extends org.modelingvalue.collections.struct.impl.StructImpl implements InvocationHandler, Comparable<StructureImpl<F>> {
    private static final long      serialVersionUID = 7315776001191198132L;

    protected static final boolean TRACE_NELUMBO    = Boolean.getBoolean("TRACE_NELUMBO");
    protected static final boolean PRETTY_NELUMBO   = Boolean.getBoolean("PRETTY_NELUMBO");
    protected static final boolean RANDOM_NELUMBO   = Boolean.getBoolean("RANDOM_NELUMBO");
    protected static final boolean REVERSE_NELUMBO  = Boolean.getBoolean("REVERSE_NELUMBO");

    private static final Method    EQUALS;
    private static final Method    HASHCODE;
    private static final Method    TO_STRING;
    static {
        try {
            EQUALS = Object.class.getMethod("equals", Object.class);
            HASHCODE = Object.class.getMethod("hashCode");
            TO_STRING = Object.class.getMethod("toString");
        } catch (NoSuchMethodException | SecurityException e) {
            throw new Error(e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.equals(EQUALS)) {
            if (proxy == args[0]) {
                return true;
            } else if (args[0] == null) {
                return false;
            } else if (args[0].getClass() != proxy.getClass()) {
                return false;
            } else {
                return super.equals(unproxy(args[0]));
            }
        } else if (method.equals(HASHCODE)) {
            return super.hashCode();
        } else if (method.equals(TO_STRING)) {
            return toString();
        } else {
            throw new Error("No handler for " + method);
        }
    }

    private final int hashCode;

    protected StructureImpl(Functor<F> functor, Object... args) {
        super(unproxy(functor, args));
        this.hashCode = getHashCode();
        init();
    }

    protected StructureImpl(FunctorImpl<F> functor, Object... args) {
        super(array(functor, args));
        this.hashCode = getHashCode();
        init();
    }

    protected StructureImpl(Class<F> type, Object... args) {
        super(array(type, args));
        this.hashCode = getHashCode();
        init();
    }

    protected StructureImpl(Object[] args) {
        super(args);
        this.hashCode = getHashCode();
    }

    private int getHashCode() {
        int r = 1;
        for (int i = 1; i < length(); i++) {
            Object e = get(i);
            r = 31 * r + (e == null ? 0 : e.hashCode());
        }
        return 31 * r + get(0).hashCode();
    }

    private void init() {
        for (int i = 1; i < length(); i++) {
            Object e = get(i);
            if (e instanceof PredicateImpl) {
                ((PredicateImpl<?>) e).init(this, i);
            }
        }
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public String toString() {
        FunctorImpl<F> f = PRETTY_NELUMBO ? functor() : null;
        RenderLambda rl = f != null ? f.renderLambda() : null;
        if (rl != null) {
            return rl.apply((StructureImpl) this);
        }
        String string = superToString();
        string = string.substring(1, string.length() - 1);
        int i = string.indexOf(',');
        return i >= 0 ? string.substring(0, i) + "(" + string.substring(i + 1) + ")" : string + "()";
    }

    private String superToString() {
        return super.toString();
    }

    public String toString(StructureImpl<?> parent) {
        return toString();
    }

    public final String toString(int i) {
        return StringUtil.toString(get(i));
    }

    private static final Object[] array(Object functor, Object[] args) {
        Object[] result = new Object[args.length + 1];
        StructureImpl.noProxy(functor);
        result[0] = functor;
        for (int i = 0; i < args.length; i++) {
            StructureImpl.noProxy(args[i]);
            result[i + 1] = args[i];
        }
        return result;
    }

    @SuppressWarnings("rawtypes")
    private static final Object[] unproxy(Functor functor, Object[] args) {
        Object[] result = new Object[args.length + 1];
        result[0] = StructureImpl.unproxy(functor);
        for (int i = 0; i < args.length; i++) {
            result[i + 1] = StructureImpl.unproxy(args[i]);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public F proxy() {
        Class<F> type = type();
        return (F) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, this);
    }

    @SuppressWarnings("unchecked")
    protected Class<F> type() {
        Object t = get(0);
        return t instanceof FunctorImpl ? ((FunctorImpl<F>) t).functType() : (Class<F>) t;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public FunctorImpl<F> functor() {
        Object t = get(0);
        return t instanceof FunctorImpl ? (FunctorImpl<F>) t : null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public int compareTo(StructureImpl<F> o) {
        int r = length() - o.length();
        if (r != 0) {
            return r;
        }
        for (int i = 0; i < length(); i++) {
            Object tv = get(i);
            Object ov = o.get(i);
            if (tv instanceof Comparable && tv.getClass().equals(ov.getClass())) {
                r = ((Comparable) tv).compareTo(ov);
                if (r != 0) {
                    break;
                }
            } else {
                String ts = StringUtil.toString(tv);
                String os = StringUtil.toString(ov);
                r = ts.compareTo(os);
                if (r != 0) {
                    break;
                }
            }
        }
        return r;
    }

    protected StructureImpl<F> eq(StructureImpl<F> other) {
        if (equals(other)) {
            return this;
        } else if (length() != other.length()) {
            return null;
        }
        Object[] array = null;
        for (int i = 0; i < length(); i++) {
            Object thisVal = get(i);
            Object eq = eq(thisVal, other.get(i));
            if (eq == null) {
                return null;
            } else if (!Objects.equals(eq, thisVal)) {
                if (array == null) {
                    array = toArray();
                }
                array[i] = eq;
            }
        }
        return array != null ? struct(array) : this;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object eq(Object thisVal, Object otherVal) {
        if (thisVal != otherVal) {
            if (thisVal instanceof StructureImpl && otherVal instanceof StructureImpl) {
                return ((StructureImpl) thisVal).eq((StructureImpl) otherVal);
            } else if (thisVal instanceof StructureImpl && otherVal instanceof Class) {
                return ((Class) otherVal).isAssignableFrom(((StructureImpl) thisVal).type()) ? thisVal : null;
            } else if (thisVal instanceof Class && otherVal instanceof StructureImpl) {
                return ((Class) thisVal).isAssignableFrom(((StructureImpl) otherVal).type()) ? otherVal : null;
            } else if (!(thisVal instanceof Class) && otherVal instanceof Class) {
                return ((Class) otherVal).isAssignableFrom(thisVal.getClass()) ? thisVal : null;
            } else if (thisVal instanceof Class && !(otherVal instanceof Class)) {
                return ((Class) thisVal).isAssignableFrom(otherVal.getClass()) ? otherVal : null;
            } else if (!Objects.equals(thisVal, otherVal)) {
                return null;
            }
        }
        return thisVal;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Map<VariableImpl, Object> variables() {
        Map<VariableImpl, Object> vars = Map.of();
        for (int i = 1; i < length(); i++) {
            Object val = get(i);
            if (val instanceof VariableImpl) {
                vars = vars.put((VariableImpl) val, ((VariableImpl) val).type());
            } else if (val instanceof StructureImpl) {
                vars = vars.putAll(((StructureImpl) val).variables());
            }
        }
        return vars;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Map<StructureImpl, int[]> constants() {
        Map<StructureImpl, int[]> preds = Map.of();
        for (int i = 1; i < length(); i++) {
            Object val = get(i);
            if (val instanceof StructureImpl && !(val instanceof VariableImpl)) {
                if (Constant.class.isAssignableFrom(((StructureImpl) val).type())) {
                    preds = preds.put((StructureImpl) val, new int[]{i});
                } else {
                    int ii = i;
                    preds = preds.putAll(((StructureImpl<?>) val).constants().replaceAll(e -> {
                        int[] idx = new int[e.getValue().length + 1];
                        System.arraycopy(e.getValue(), 0, idx, 1, e.getValue().length);
                        idx[0] = ii;
                        return Entry.of(e.getKey(), idx);
                    }));
                }
            }
        }
        return preds;

    }

    @SuppressWarnings("unchecked")
    public <V> V getVal(int... is) {
        Object val = this;
        for (int i : is) {
            val = ((StructureImpl<?>) val).get(i);
            if (val instanceof Class || val instanceof VariableImpl) {
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

    public StructureImpl<F> set(int f, Object... a) {
        Object[] array = setArray(f, a);
        return array != null ? struct(array) : this;
    }

    public StructureImpl<F> set(int[] idx, Object val) {
        return set(0, idx, val);
    }

    @SuppressWarnings("rawtypes")
    private StructureImpl<F> set(int ii, int[] idx, Object val) {
        Object[] array = toArray();
        if (ii < idx.length - 1) {
            StructureImpl s = (StructureImpl) array[idx[ii]];
            array[idx[ii]] = s.set(ii + 1, idx, val);
        } else {
            array[idx[ii]] = val;
        }
        return struct(array);
    }

    @SuppressWarnings("rawtypes")
    protected StructureImpl<?> replace(StructureImpl<?> from, StructureImpl<?> to) {
        if (equals(from)) {
            return to;
        } else {
            Object[] array = null;
            for (int i = 0; i < length(); i++) {
                Object thisVal = get(i);
                if (thisVal instanceof StructureImpl) {
                    StructureImpl<?> toVal = ((StructureImpl<?>) thisVal).replace(from, to);
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
    protected abstract StructureImpl<F> struct(Object[] array);

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected final Object get(StructureImpl<F> declaration, VariableImpl var) {
        for (int i = 1; i < length(); i++) {
            Object thisVal = get(i);
            Object declVal = declaration.get(i);
            if (declVal.equals(var)) {
                return thisVal;
            } else if (thisVal instanceof StructureImpl) {
                Object varVal = ((StructureImpl) thisVal).get((StructureImpl) declVal, var);
                if (varVal != null) {
                    return varVal;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    protected final Map<VariableImpl, Object> getBinding(StructureImpl<F> declaration, Map<VariableImpl, Object> vars, boolean check) {
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
    private static Map<VariableImpl, Object> getBinding(Object declVal, Object thisVal, Map<VariableImpl, Object> vars, boolean check) {
        Class thisType = typeOf(thisVal);
        thisVal = thisVal instanceof Class ? null : thisVal;
        if (declVal instanceof VariableImpl) {
            VariableImpl var = (VariableImpl) declVal;
            Object varVal = vars.get(var);
            Class varType = typeOf(varVal);
            varVal = varVal instanceof Class ? null : varVal;
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
        } else if (declVal instanceof StructureImpl) {
            StructureImpl declStruct = (StructureImpl) declVal;
            if (thisVal != null) {
                if (thisVal instanceof StructureImpl) {
                    vars = ((StructureImpl) thisVal).getBinding(declStruct, vars, check);
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

    @SuppressWarnings("rawtypes")
    public static Class typeOf(Object v) {
        return v instanceof StructureImpl ? ((StructureImpl) v).type() : v instanceof Class ? (Class) v : null;
    }

    @SuppressWarnings("rawtypes")
    protected final StructureImpl<F> set(StructureImpl<F> declaration, VariableImpl var, Object val) {
        return setBinding(declaration, Map.of(Entry.of(var, val)));
    }

    @SuppressWarnings("rawtypes")
    protected final StructureImpl<F> setBinding(StructureImpl<F> declaration, Map<VariableImpl, Object> vars) {
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
    private static Object setBinding(Object declVal, Object thisVal, Map<VariableImpl, Object> vars) {
        if (declVal instanceof VariableImpl) {
            Object varVal = vars.get((VariableImpl) declVal);
            if (varVal != null) {
                return varVal;
            }
        } else if (declVal instanceof StructureImpl) {
            if (thisVal instanceof StructureImpl) {
                return ((StructureImpl) thisVal).setBinding((StructureImpl) declVal, vars);
            } else if (thisVal instanceof Class && ((Class) thisVal).isAssignableFrom((((StructureImpl) declVal).type()))) {
                return ((StructureImpl) declVal).setBinding((StructureImpl) declVal, vars);
            }
        }
        return thisVal;
    }

    @SuppressWarnings("rawtypes")
    protected final boolean isFullyBound() {
        for (int i = 1; i < length(); i++) {
            Object val = get(i);
            if (val instanceof Class) {
                return false;
            } else if (val instanceof StructureImpl && !((StructureImpl) val).isFullyBound()) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("rawtypes")
    protected final int nrOfUnbound() {
        int nr = 0;
        for (int i = 1; i < length(); i++) {
            Object val = get(i);
            if (val instanceof Class) {
                nr++;
            } else if (val instanceof StructureImpl) {
                nr += ((StructureImpl) val).nrOfUnbound();
            }
        }
        return nr;
    }

    @SuppressWarnings("rawtypes")
    protected final int nrOfVariables() {
        int nr = 0;
        for (int i = 1; i < length(); i++) {
            Object val = get(i);
            if (val instanceof VariableImpl) {
                nr++;
            } else if (val instanceof StructureImpl) {
                nr += ((StructureImpl) val).nrOfVariables();
            }
        }
        return nr;
    }

    @SuppressWarnings("unchecked")
    public static final <T extends Structure, R extends StructureImpl<?>> R unproxy(T object) {
        return (R) Proxy.getInvocationHandler(object);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final Object unproxy(Object object) {
        if (object instanceof Structure) {
            return Proxy.getInvocationHandler(object);
        } else if (object instanceof List) {
            return ((List) object).replaceAll(StructureImpl::unproxy);
        } else {
            Objects.requireNonNull(object);
            return object;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static final void noProxy(Object object) {
        if (object instanceof Structure) {
            throw new IllegalArgumentException();
        } else if (object instanceof List) {
            ((List) object).forEach(StructureImpl::noProxy);
        }
    }

    protected final int depth() {
        int result = 1;
        for (int i = 1; i < length(); i++) {
            Object v = get(i);
            if (v instanceof TypedImpl && !((TypedImpl<?, ?>) v).atomic()) {
                result = Math.max(result, ((StructureImpl<?>) v).depth() + 1);
            }
        }
        return result;
    }

    protected final Object[] signatureArray(int depth) {
        Object[] array = null;
        for (int i = 1; i < length(); i++) {
            Object v = get(i);
            Object r = v;
            if (depth > 1 && v instanceof TypedImpl && !((TypedImpl<?, ?>) v).atomic()) {
                r = ((TypedImpl<?, ?>) v).signature(depth - 1);
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected final Set<StructureImpl<F>> doGeneralize() {
        Set<StructureImpl<F>> result = Set.of();
        for (int i = 1; i < length(); i++) {
            Object v = get(i);
            if (v instanceof TypedImpl) {
                Set<StructureImpl> gen = ((TypedImpl) v).doGeneralize();
                for (StructureImpl s : gen) {
                    result = result.add(setTyped(i, s));
                }
                if (gen.isEmpty()) {
                    result = result.add(setType(i, typeOf(v)));
                }
            } else if (v instanceof Class) {
                List<Class> args = functor().args();
                for (Class s : KnowledgeBaseImpl.generalizations((Class) v, args.get(i - 1))) {
                    result = result.add(setType(i, s));
                }
            }
        }
        return result;
    }

    protected StructureImpl<F> setType(int i, Class<?> type) {
        return set(i, type);
    }

    protected StructureImpl<F> setTyped(int i, StructureImpl<?> typed) {
        return set(i, typed);
    }

}
