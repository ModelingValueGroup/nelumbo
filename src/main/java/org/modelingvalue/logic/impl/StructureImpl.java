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

package org.modelingvalue.logic.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.Map;
import org.modelingvalue.collections.util.StringUtil;
import org.modelingvalue.logic.Logic.Functor;
import org.modelingvalue.logic.Logic.NormalizeLambda;
import org.modelingvalue.logic.Logic.Structure;

public class StructureImpl<F extends Structure> extends org.modelingvalue.collections.struct.impl.StructImpl implements InvocationHandler, Comparable<StructureImpl<F>> {
    private static final long   serialVersionUID = 7315776001191198132L;

    private static final Method EQUALS;
    private static final Method HASHCODE;
    private static final Method TO_STRING;
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

    public StructureImpl(Functor<F> functor, Object... args) {
        super(unproxy(functor, args));
        this.hashCode = getHashCode();
    }

    protected StructureImpl(FunctorImpl<F> functor, Object... args) {
        super(array(functor, args));
        this.hashCode = getHashCode();
    }

    protected StructureImpl(Class<F> type, Object... args) {
        super(array(type, args));
        this.hashCode = getHashCode();
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

    @Override
    public int hashCode() {
        return hashCode;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public String toString() {
        String string = super.toString();
        string = string.substring(1, string.length() - 1);
        int i = string.indexOf(',');
        return i >= 0 ? string.substring(0, i) + "(" + string.substring(i + 1) + ")" : string + "()";
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

    public final StructureImpl<F> eq(StructureImpl<F> other) {
        if (equals(other)) {
            return this;
        } else if (length() != other.length()) {
            return null;
        }
        Object[] array = null;
        for (int i = 0; i < length(); i++) {
            Object tv = get(i);
            Object eq = eq(tv, other.get(i));
            if (eq == null) {
                return null;
            } else if (!Objects.equals(eq, tv)) {
                if (array == null) {
                    array = toArray();
                }
                array[i] = eq;
            }
        }
        return array != null ? struct(array) : this;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object eq(Object tv, Object ov) {
        if (tv != ov) {
            if (tv instanceof StructureImpl && ov instanceof StructureImpl) {
                return ((StructureImpl) tv).eq((StructureImpl) ov);
            } else if (tv instanceof StructureImpl && ov instanceof Class) {
                return ((Class) ov).isAssignableFrom(((StructureImpl) tv).type()) ? tv : null;
            } else if (tv instanceof Class && ov instanceof StructureImpl) {
                return ((Class) tv).isAssignableFrom(((StructureImpl) ov).type()) ? ov : null;
            } else if (!(tv instanceof Class) && ov instanceof Class) {
                return ((Class) ov).isAssignableFrom(tv.getClass()) ? tv : null;
            } else if (tv instanceof Class && !(ov instanceof Class)) {
                return ((Class) tv).isAssignableFrom(ov.getClass()) ? ov : null;
            } else if (!Objects.equals(tv, ov)) {
                return null;
            }
        }
        return tv;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Map<VariableImpl, Object> variables() {
        Map<VariableImpl, Object> vars = Map.of();
        for (int i = 1; i < length(); i++) {
            Object v = get(i);
            if (v instanceof VariableImpl) {
                vars = vars.put((VariableImpl) v, ((VariableImpl) v).type());
            } else if (v instanceof StructureImpl) {
                vars = vars.putAll(((StructureImpl) v).variables());
            }
        }
        return vars;
    }

    @SuppressWarnings("unchecked")
    public <V> V getVal(int... is) {
        Object v = this;
        for (int i : is) {
            v = ((StructureImpl<?>) v).get(i);
            if (v instanceof Class || v instanceof VariableImpl) {
                return null;
            }
        }
        return (V) v;
    }

    public StructureImpl<F> set(int f, Object... a) {
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
        return array != null ? struct(array) : this;
    }

    @SuppressWarnings("unchecked")
    public final StructureImpl<F> normal() {
        FunctorImpl<F> f = functor();
        NormalizeLambda n = f != null ? f.functNormal() : null;
        return n != null ? (StructureImpl<F>) n.apply((StructureImpl<Structure>) this) : this;
    }

    @SuppressWarnings("unchecked")
    protected StructureImpl<F> struct(Object[] array) {
        return new StructureImpl<F>(array).normal();
    }

    @SuppressWarnings("rawtypes")
    public Map<VariableImpl, Object> getBinding(StructureImpl<F> struct, Map<VariableImpl, Object> vars) {
        if (get(0).equals(struct.get(0))) {
            for (int i = 1; i < length(); i++) {
                vars = getBinding(get(i), struct.get(i), vars);
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
    private static Map<VariableImpl, Object> getBinding(Object thisVal, Object structVal, Map<VariableImpl, Object> vars) {
        Class structType = typeOf(structVal);
        structVal = structVal instanceof Class ? null : structVal;
        if (thisVal instanceof VariableImpl) {
            VariableImpl var = (VariableImpl) thisVal;
            Object varVal = vars.get(var);
            Class varType = typeOf(varVal);
            varVal = varVal instanceof Class ? null : varVal;
            if (varVal != null) {
                if (structVal != null && !structVal.equals(varVal)) {
                    return null;
                }
            } else if (structVal != null) {
                if (var.type().isAssignableFrom(structType)) {
                    vars = vars.put(var, structVal);
                } else {
                    return null;
                }
            } else if (structType == null || !var.type().isAssignableFrom(structType)) {
                return null;
            } else if (varType != null && !varType.equals(structType)) {
                return null;
            } else {
                vars = vars.put(var, structType);
            }
        } else if (thisVal instanceof StructureImpl) {
            StructureImpl t = (StructureImpl) thisVal;
            if (structVal != null) {
                if (structVal instanceof StructureImpl) {
                    vars = t.getBinding((StructureImpl) structVal, vars);
                } else {
                    return null;
                }
            } else if (structType == null || !t.type().isAssignableFrom(structType)) {
                return null;
            }
        } else if (structVal != null && !structVal.equals(thisVal)) {
            return null;
        }
        return vars;
    }

    @SuppressWarnings("rawtypes")
    public static Class typeOf(Object v) {
        return v instanceof StructureImpl ? ((StructureImpl) v).type() : v instanceof Class ? (Class) v : null;
    }

    @SuppressWarnings("rawtypes")
    protected StructureImpl setBinding(StructureImpl<F> struct, Map<VariableImpl, Object> vars) {
        Object[] array = null;
        for (int i = 1; i < struct.length(); i++) {
            Object structVal = struct.get(i);
            Object bound = setBinding(get(i), structVal, vars);
            if (!Objects.equals(bound, structVal)) {
                if (array == null) {
                    array = struct.toArray();
                }
                array[i] = bound;
            }
        }
        return array != null ? struct.struct(array) : struct;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object setBinding(Object thisVal, Object structVal, Map<VariableImpl, Object> vars) {
        if (thisVal instanceof VariableImpl) {
            Object varVal = vars.get((VariableImpl) thisVal);
            if (varVal != null) {
                return varVal;
            }
        } else if (thisVal instanceof StructureImpl) {
            if (structVal instanceof StructureImpl) {
                return ((StructureImpl) thisVal).setBinding((StructureImpl) structVal, vars);
            } else if (structVal instanceof Class && ((Class) structVal).isAssignableFrom((((StructureImpl) thisVal).type()))) {
                return ((StructureImpl) thisVal).setBinding((StructureImpl) thisVal, vars);
            }
        }
        return structVal;
    }

    protected boolean isFullyBound() {
        return nrOfUnbound() == 0;
    }

    @SuppressWarnings("rawtypes")
    protected final int nrOfUnbound() {
        int nr = 0;
        for (int i = 1; i < length(); i++) {
            Object v = get(i);
            if (v instanceof Class) {
                nr++;
            } else if (v instanceof StructureImpl) {
                nr += ((StructureImpl) v).nrOfUnbound();
            }
        }
        return nr;
    }

    @SuppressWarnings("rawtypes")
    protected final int nrOfVariables() {
        int nr = 0;
        for (int i = 1; i < length(); i++) {
            Object v = get(i);
            if (v instanceof VariableImpl) {
                nr++;
            } else if (v instanceof StructureImpl) {
                nr += ((StructureImpl) v).nrOfVariables();
            }
        }
        return nr;
    }

    @SuppressWarnings("unchecked")
    public static final <T extends Structure, R extends StructureImpl<T>> R unproxy(T object) {
        return (R) Proxy.getInvocationHandler(object);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static final Object unproxy(Object object) {
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
}
