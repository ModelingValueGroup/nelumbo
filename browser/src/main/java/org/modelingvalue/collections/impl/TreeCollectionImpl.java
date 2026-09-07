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

package org.modelingvalue.collections.impl;

// OVERLAY of immutable-collections TreeCollectionImpl (from ../immutable-collections, API-compatible with 6.0.2)
// for the TeaVM (browser) build - deltas: split() never splits, getIntStream uses IntStream.range.

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterator.OfInt;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.modelingvalue.collections.Collection;
import org.modelingvalue.collections.ContainingCollection;
import org.modelingvalue.collections.StreamCollection;
import org.modelingvalue.collections.util.*;

public abstract class TreeCollectionImpl<T> extends CollectionImpl<T> implements ContainingCollection<T> {
    private static final long         serialVersionUID   = 7999808719969099597L;
    protected static final int        CHARACTERISTICS    = Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE | Spliterator.NONNULL;
    private static final int          SPLIT_START        = Integer.getInteger("SPLIT_START", 64);
    private static final Predicate<?> ALL_INTERNABLE     = e -> e instanceof Internable && ((Internable) e).isInternable();
    private static final int          MAX_NO_STREAM_SIZE = Integer.getInteger("MAX_NO_STREAM_SIZE", 8);

    protected static boolean split(int amount) {
        return false; // single-threaded under TeaVM: never split
    }

    transient protected Object value;

    @Override
    public boolean isEmpty() {
        return value == null;
    }

    @Override
    protected Stream<T> baseStream() {
        return new StreamCollectionImpl<>(spliterator(), isParallel());
    }

    @Override
    public Collection<T> reverse() {
        return new StreamCollectionImpl<>(reverseSpliterator(), isParallel());
    }

    @Override
    public int hashCode() {
        return hash(value);
    }

    @Override
    public int size() {
        return size(value);
    }

    protected static int hash(Object v) {
        return v == null ? 0 : v instanceof MultiValue ? ((MultiValue) v).hash : v.hashCode();
    }

    protected static byte depth(Object v) {
        return v == null ? 1 : v instanceof MultiValue ? ((MultiValue) v).depth : 1;
    }

    protected static int size(Object v) {
        return v == null ? 0 : v instanceof MultiValue ? ((MultiValue) v).size : 1;
    }

    protected static int length(Object v) {
        return v == null ? 0 : v instanceof MultiValue ? ((MultiValue) v).values.length : 1;
    }

    protected static byte max(byte a, byte b) {
        return (a >= b) ? a : b;
    }

    protected static byte min(byte a, byte b) {
        return (a <= b) ? a : b;
    }

    protected static Object get(Object v, int i) {
        if (v instanceof MultiValue) {
            return ((MultiValue) v).values[i];
        } else if (v != null && i == 0) {
            return v;
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!getClass().equals(obj.getClass())) {
            return false;
        }
        @SuppressWarnings("rawtypes")
        TreeCollectionImpl other = (TreeCollectionImpl) obj;
        if (value == other.value) {
            return true;
        } else if (!equalsWithStop(value, other.value, new boolean[1])) {
            return false;
        } else if (IdentityRank.rank(value) < IdentityRank.rank(other.value)) {
            other.value = value;
            return true;
        } else {
            value = other.value;
            return true;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected static boolean equalsWithStop(Object v1, Object v2, boolean[] stop) {
        if (v1 instanceof MultiValue) {
            return ((MultiValue) v1).equalsWithStop(v2, stop);
        } else {
            return Objects.equals(v1, v2);
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new CollectionIterator<>(value, 0);
    }

    @Override
    public ListIterator<T> listIterator() {
        return new CollectionIterator<>(value, 0);
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return new CollectionIterator<>(value, index);
    }

    @Override
    public ListIterator<T> listIteratorAtEnd() {
        return new CollectionIterator<>(value, length(value));
    }

    protected static IntStream getIntStream(int min, int max, boolean[] stop, int total) {
        // TeaVM lacks StreamSupport.intStream; stop[]-based short-circuiting is lost (results unaffected)
        return IntStream.range(min, max);
    }

    private static final class IntSpliterator implements OfInt {
        private static final int INT_CHARACTERISTICS = Spliterator.DISTINCT | Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE | Spliterator.NONNULL;

        private final boolean[]  stop;
        private int              min, total;

        private final int        max;

        private IntSpliterator(int min, int max, boolean[] stop, int total) {
            this.stop = stop;
            this.min = min;
            this.max = max;
            this.total = total;
        }

        @Override
        public boolean tryAdvance(Consumer<? super Integer> action) {
            if (action instanceof IntConsumer) {
                return tryAdvance((IntConsumer) action);
            } else {
                if (min < max) {
                    action.accept(min++);
                    return true;
                } else {
                    return false;
                }
            }
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            if (min < max) {
                action.accept(min++);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void forEachRemaining(IntConsumer action) {
            for (; min < max && !stop[0]; min++) {
                action.accept(min);
            }
        }

        @Override
        public void forEachRemaining(Consumer<? super Integer> action) {
            if (action instanceof IntConsumer) {
                forEachRemaining((IntConsumer) action);
            } else {
                for (; min < max && !stop[0]; min++) {
                    action.accept(min);
                }
            }
        }

        @Override
        public OfInt trySplit() {
            int delta = max - min;
            if (delta > 1 && split(total) && !stop[0]) {
                int half = min + delta / 2;
                total /= 2;
                OfInt prefix = new IntSpliterator(min, half, stop, total);
                min = half;
                assert (min >= 0 && max > min);
                return prefix;
            }
            return null;
        }

        @Override
        public long estimateSize() {
            return max - min;
        }

        @Override
        public int characteristics() {
            return INT_CHARACTERISTICS;
        }
    }

    @SuppressWarnings("unchecked")
    protected static <T> void visit(Object v, Consumer<? super T> visitor) {
        if (v instanceof MultiValue) {
            ((MultiValue) v).visit(visitor);
        } else if (v != null) {
            visitor.accept((T) v);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @SuppressWarnings("unchecked")
    protected static <T> void reverseVisit(Object v, Consumer<? super T> visitor) {
        if (v instanceof MultiValue) {
            ((MultiValue) v).reverseVisit(visitor);
        } else if (v != null) {
            visitor.accept((T) v);
        } else {
            throw new IllegalArgumentException();
        }
    }

    protected static abstract class CollectionSpliterator<T> implements Spliterator<T> {

        private Object        value;
        private int           min, max, size;
        private final boolean reverse;

        protected CollectionSpliterator(Object value, int min, int max, int size, boolean reverse) {
            this.value = value;
            this.min = min;
            this.max = max;
            this.size = size;
            this.reverse = reverse;
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean tryAdvance(Consumer<? super T> visitor) {
            if (min < max) {
                CollectionIterator<T> ct;
                if (value instanceof CollectionIterator) {
                    ct = (CollectionIterator<T>) value;
                } else {
                    ct = new CollectionIterator<>(value, reverse ? max : min);
                    value = ct;
                }
                if (!reverse && ct.index[0] < max && ct.hasNext()) {
                    visitor.accept(ct.next());
                    return true;
                } else if (reverse && ct.index[0] > min && ct.hasPrevious()) {
                    visitor.accept(ct.previous());
                    return true;
                }
            }
            return false;
        }

        @Override
        public void forEachRemaining(Consumer<? super T> visitor) {
            if (!reverse) {
                for (int i = min; i < max; i++) {
                    visit(get(value, i), visitor);
                }
            } else {
                for (int i = max - 1; i >= min; i--) {
                    reverseVisit(get(value, i), visitor);
                }
            }
        }

        @Override
        public Spliterator<T> trySplit() {
            if (max - min > 1 && TreeCollectionImpl.split(size)) {
                MultiValue multi = (MultiValue) value;
                int half = size / 2;
                int amount = 0;
                for (int i = min; i < max - 1;) {
                    amount += size(multi.values[i++]);
                    if (amount >= half && amount < size) {
                        Spliterator<T> prefix;
                        if (reverse) {
                            if (max - i == 1) {
                                Object element = multi.values[i];
                                prefix = split(element, 0, length(element), size - amount, true);
                            } else {
                                prefix = split(multi, i, max, size - amount, true);
                            }
                            if (i - min == 1) {
                                value = multi.values[min];
                                min = 0;
                                max = length(value);
                            } else {
                                max = i;
                            }
                            size = amount;
                        } else {
                            if (i - min == 1) {
                                Object element = multi.values[min];
                                prefix = split(element, 0, length(element), amount, false);
                            } else {
                                prefix = split(multi, min, i, amount, false);
                            }
                            if (max - i == 1) {
                                value = multi.values[i];
                                min = 0;
                                max = length(value);
                            } else {
                                min = i;
                            }
                            size -= amount;
                        }
                        return prefix;
                    }
                }
            }
            return null;
        }

        @Override
        public long estimateSize() {
            return size;
        }

        protected abstract Spliterator<T> split(Object element, int start, int end, int amount, boolean reverse);
    }

    private static final class CollectionIterator<T> implements ListIterator<T> {

        private final int[]    index;
        private final Object[] stack;
        private int            level;

        private CollectionIterator(Object value, int cursor) {
            index = new int[depth(value)];
            stack = new Object[index.length];
            stack[0] = value;
            index[0] = cursor;
        }

        @Override
        public boolean hasNext() {
            while (true) {
                if (index[level] < length(stack[level])) {
                    return true;
                } else if (level > 0) {
                    stack[level] = null;
                    index[--level]++;
                } else {
                    return false;
                }
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public T next() {
            if (hasNext()) {
                while (stack[level] instanceof MultiValue) {
                    stack[level + 1] = ((MultiValue) stack[level]).values[index[level]];
                    index[++level] = 0;
                }
                index[level]++;
                return (T) stack[level];
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public boolean hasPrevious() {
            while (true) {
                if (index[level] > 0) {
                    return true;
                } else if (level > 0) {
                    stack[level] = null;
                    index[--level]--;
                } else {
                    return false;
                }
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public T previous() {
            if (hasPrevious()) {
                while (stack[level] instanceof MultiValue) {
                    stack[level + 1] = ((MultiValue) stack[level]).values[index[level] - 1];
                    index[++level] = length(stack[level]);
                }
                index[level]--;
                return (T) stack[level];
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public int nextIndex() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int previousIndex() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(T e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(T e) {
            throw new UnsupportedOperationException();
        }
    }

    protected abstract static class MultiValue implements Serializable {

        private static final long serialVersionUID = -901414039518935454L;

        protected final Object[]  values;
        protected final int       size;
        protected final int       hash;
        protected final byte      depth;

        protected MultiValue(Object[] values, int size, int hash, byte depth) {
            this.values = values;
            this.size = size;
            this.hash = hash;
            this.depth = depth;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        protected abstract boolean equalsWithStop(Object obj, boolean[] stop);

        @Override
        public boolean equals(Object obj) {
            return equalsWithStop(obj, new boolean[1]);
        }

        protected static int size(Object obj) {
            return TreeCollectionImpl.size(obj);
        }

        protected static int hash(Object obj) {
            return TreeCollectionImpl.hash(obj);
        }

        protected static byte depth(Object obj) {
            return TreeCollectionImpl.depth(obj);
        }

        protected <T> void visit(Consumer<? super T> visitor) {
            for (Object o : values) {
                TreeCollectionImpl.visit(o, visitor);
            }
        }

        protected <T> void reverseVisit(Consumer<? super T> visitor) {
            for (int i = values.length - 1; i >= 0; i--) {
                TreeCollectionImpl.reverseVisit(values[i], visitor);
            }
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            b.append('[');
            for (int i = 0; i < values.length; i++) {
                if (values[i] != null) {
                    b.append(StringUtil.toString(values[i]));
                }
                if (i < values.length - 1) {
                    b.append(",");
                }
            }
            return b.append(']').toString();
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    public T get(int index) {
        return (T) getDeep(value, index);
    }

    protected static Object getDeep(Object obj, int idx) {
        if (idx < 0) {
            throw new IndexOutOfBoundsException();
        }
        outer:
        while (true) {
            if (obj instanceof MultiValue) {
                int len = 0;
                for (Object val : ((MultiValue) obj).values) {
                    int valSize = size(val);
                    if (len + valSize > idx) {
                        obj = val;
                        idx = idx - len;
                        continue outer;
                    }
                    len += valSize;
                }
            } else if (obj != null && idx == 0) {
                return obj;
            }
            throw new IndexOutOfBoundsException();
        }
    }

    abstract protected TreeCollectionImpl<T> create(Object val);

    abstract protected StreamCollection<Object[]> getCompareStream(ContainingCollection<? extends T> toCompare);

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <R extends ContainingCollection<T>> StreamCollection<R[]> compare(R other) {
        Class<? extends TreeCollectionImpl> cls = getClass();
        return (StreamCollection<R[]>) getCompareStream(other).map(a -> createCompare(a, (R[]) Array.newInstance(cls, 2), this));
    }

    @SuppressWarnings("unchecked")
    private static <R extends ContainingCollection<T>, T> R[] createCompare(Object[] values, R[] compare, TreeCollectionImpl<T> base) {
        next:
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null) {
                for (int ii = 0; ii < i; ii++) {
                    if (values[ii] == values[i]) {
                        compare[i] = compare[ii];
                        continue next;
                    }
                }
                compare[i] = (R) base.create(values[i]);
            } else {
                compare[i] = null;
            }
        }
        return compare;
    }

    @Override
    public <R> Collection<R> linked(TriFunction<T, T, T, R> function) {
        int size = size();
        return Collection.of(getIntStream(0, size, new boolean[1], size)).map(i -> function.apply(i > 0 ? get(i - 1) : null, get(i), i < size - 1 ? get(i + 1) : null));
    }

    @Override
    public void linked(TriConsumer<T, T, T> consumer) {
        int size = size();
        Collection.of(getIntStream(0, size, new boolean[1], size)).forEachOrdered(i -> consumer.accept(i > 0 ? get(i - 1) : null, get(i), i < size - 1 ? get(i + 1) : null));
    }

    @Override
    public <R> Collection<R> indexed(BiFunction<T, Integer, R> function) {
        int size = size();
        return Collection.of(getIntStream(0, size, new boolean[1], size)).map(i -> function.apply(get(i), i));
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean isInternable() {
        return allMatch((Predicate<? super T>) ALL_INTERNABLE);
    }

    @Override
    public void javaSerialize(Serializer s) {
        s.writeInt(size());
        for (Object e : this) {
            s.writeObject(e);
        }
    }

    @Override
    public void javaDeserialize(Deserializer s) {
        int size = s.readInt();
        for (int i = 0; i < size; i++) {
            T e = s.readObject();
            TreeCollectionImpl<T> newSet = (TreeCollectionImpl<T>) add(e);
            this.value = newSet.value;
        }
    }

    @Override
    public Collection<T> filter(Predicate<? super T> predicate) {
        if (size() > MAX_NO_STREAM_SIZE && isParallel()) {
            return super.filter(predicate);
        } else {
            ContainingCollection<T> result = clear();
            for (T t : this) {
                if (predicate.test(t)) {
                    result = result.add(t);
                }
            }
            return result;
        }
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        if (size() > MAX_NO_STREAM_SIZE && isParallel()) {
            super.forEach(action);
        } else {
            for (T t : this) {
                action.accept(t);
            }
        }
    }

    @Override
    public void forEachOrdered(Consumer<? super T> action) {
        if (size() > MAX_NO_STREAM_SIZE && isParallel()) {
            super.forEachOrdered(action);
        } else {
            for (T t : this) {
                action.accept(t);
            }
        }
    }

    @Override
    public boolean anyMatch(Predicate<? super T> predicate) {
        if (size() > MAX_NO_STREAM_SIZE && isParallel()) {
            return super.anyMatch(predicate);
        } else {
            for (T t : this) {
                if (predicate.test(t)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public boolean allMatch(Predicate<? super T> predicate) {
        if (size() > MAX_NO_STREAM_SIZE && isParallel()) {
            return super.allMatch(predicate);
        } else {
            for (T t : this) {
                if (!predicate.test(t)) {
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    public boolean noneMatch(Predicate<? super T> predicate) {
        if (size() > MAX_NO_STREAM_SIZE && isParallel()) {
            return super.noneMatch(predicate);
        } else {
            for (T t : this) {
                if (predicate.test(t)) {
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    public T reduce(T identity, BinaryOperator<T> accumulator) {
        if (size() > MAX_NO_STREAM_SIZE && isParallel()) {
            return super.reduce(identity, accumulator);
        } else {
            T r = identity;
            for (T t : this) {
                r = accumulator.apply(r, t);
            }
            return r;
        }

    }

    @Override
    public Optional<T> reduce(BinaryOperator<T> accumulator) {
        if (size() > MAX_NO_STREAM_SIZE && isParallel()) {
            return super.reduce(accumulator);
        } else {
            T r = null;
            for (T t : this) {
                r = r == null ? t : accumulator.apply(r, t);
            }
            return r == null ? Optional.empty() : Optional.of(r);
        }
    }

    @Override
    public Optional<T> min(Comparator<? super T> comparator) {
        if (size() > MAX_NO_STREAM_SIZE && isParallel()) {
            return super.min(comparator);
        } else {
            T r = null;
            for (T t : this) {
                r = r == null ? t : comparator.compare(r, t) > 0 ? t : r;
            }
            return r == null ? Optional.empty() : Optional.of(r);
        }
    }

    @Override
    public Optional<T> max(Comparator<? super T> comparator) {
        if (size() > MAX_NO_STREAM_SIZE && isParallel()) {
            return super.max(comparator);
        } else {
            T r = null;
            for (T t : this) {
                r = r == null ? t : comparator.compare(r, t) < 0 ? t : r;
            }
            return r == null ? Optional.empty() : Optional.of(r);
        }
    }

    @Override
    public Optional<T> findAny(Predicate<? super T> predicate) {
        if (size() > MAX_NO_STREAM_SIZE && isParallel()) {
            return super.findAny(predicate);
        } else {
            for (T t : this) {
                if (predicate.test(t)) {
                    return Optional.of(t);
                }
            }
            return Optional.empty();
        }
    }

    @Override
    public Optional<T> findFirst(Predicate<? super T> predicate) {
        if (size() > MAX_NO_STREAM_SIZE && isParallel()) {
            return super.findFirst(predicate);
        } else {
            for (T t : this) {
                if (predicate.test(t)) {
                    return Optional.of(t);
                }
            }
            return Optional.empty();
        }
    }

    @Override
    public long count() {
        return size();
    }

    @Override
    public Optional<T> findFirst() {
        return isEmpty() ? Optional.empty() : Optional.of(get(0));
    }

    @Override
    public Optional<T> findAny() {
        return findFirst();
    }

}
