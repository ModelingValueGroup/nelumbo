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

package org.modelingvalue.collections.mutable;

// OVERLAY of immutable-collections MutableMap (from ../immutable-collections, API-compatible with 6.0.2)
// for the TeaVM (browser) build - delta: compareAndSet instead of weakCompareAndSetVolatile.

import java.lang.reflect.Array;
import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.modelingvalue.collections.Collection;
import org.modelingvalue.collections.Entry;
import org.modelingvalue.collections.Map;

public abstract class MutableMap<K, V> extends AbstractMap<K, V> implements Mutable<Entry<K, V>> {

    public static <K, V> MutableMap<K, V> of(Map<K, V> map) {
        return new Impl<K, V>(map);
    }

    public static <K, V> MutableMap<K, V> concurrent(Map<K, V> map) {
        return new ConcurrentImpl<K, V>(map);
    }

    public abstract Map<K, V> get();

    public abstract boolean set(UnaryOperator<Map<K, V>> oper);

    public abstract Map<K, V> getAndSet(UnaryOperator<Map<K, V>> oper);

    @Override
    public Map<K, V> toImmutable() {
        return get();
    }

    @Override
    public int size() {
        return get().size();
    }

    @Override
    public boolean isEmpty() {
        return get().isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean containsKey(Object key) {
        return get().containsKey((K) key);
    }

    @Override
    public boolean containsValue(Object value) {
        return get().toValues().anyMatch(v -> Objects.equals(v, value));
    }

    @SuppressWarnings("unchecked")
    @Override
    public V get(Object key) {
        return get().get((K) key);
    }

    @Override
    public V put(K key, V value) {
        return getAndSet(map -> map.put(key, value)).get(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public V remove(Object key) {
        return getAndSet(map -> map.removeKey((K) key)).get((K) key);
    }

    @Override
    public void putAll(java.util.Map<? extends K, ? extends V> m) {
        Map<? extends K, ? extends V> all = Map.fromMutable(m);
        set(map -> map.putAll(all));
    }

    @Override
    public void clear() {
        set(Map::clear);
    }

    @Override
    public java.util.Set<K> keySet() {
        return new java.util.Set<K>() {

            @Override
            public int size() {
                return get().size();
            }

            @Override
            public boolean isEmpty() {
                return get().isEmpty();
            }

            @SuppressWarnings("unchecked")
            @Override
            public boolean contains(Object o) {
                return get().containsKey((K) o);
            }

            @Override
            public Iterator<K> iterator() {
                return new Iterator<K>() {
                    private final Iterator<org.modelingvalue.collections.Entry<K, V>> it   = get().iterator();
                    private org.modelingvalue.collections.Entry<K, V>                 last = null;

                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public K next() {
                        last = it.next();
                        return last.getKey();
                    }

                    @Override
                    public void remove() {
                        set(map -> map.remove(last));
                    }
                };
            }

            @Override
            public Object[] toArray() {
                return get().toKeys().toArray();
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> T[] toArray(T[] a) {
                return get().toKeys().toArray(i -> (T[]) Array.newInstance(a.getClass().getComponentType(), i));
            }

            @SuppressWarnings("unchecked")
            @Override
            public boolean containsAll(java.util.Collection<?> c) {
                Map<K, V> map = get();
                return Stream.of(c).map(k -> (K) k).allMatch(map::containsKey);
            }

            @SuppressWarnings("unchecked")
            @Override
            public boolean remove(Object o) {
                return set(map -> map.removeKey((K) o));
            }

            @Override
            public boolean retainAll(java.util.Collection<?> c) {
                return set(map -> map.removeAllKey(map.toKeys().exclude(c::contains)));
            }

            @Override
            public boolean removeAll(java.util.Collection<?> c) {
                return set(map -> map.removeAllKey(Collection.of(c)));
            }

            @Override
            public void clear() {
                set(Map::clear);
            }

            @Override
            public boolean add(K e) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean addAll(java.util.Collection<? extends K> c) {
                throw new UnsupportedOperationException();
            }

        };
    }

    @Override
    public java.util.Collection<V> values() {
        return new java.util.Collection<V>() {

            @Override
            public int size() {
                return get().size();
            }

            @Override
            public boolean isEmpty() {
                return get().isEmpty();
            }

            @Override
            public boolean contains(Object o) {
                return get().toValues().anyMatch(v -> Objects.equals(v, o));
            }

            @Override
            public Iterator<V> iterator() {
                return new Iterator<V>() {
                    private final Iterator<org.modelingvalue.collections.Entry<K, V>> it   = get().iterator();
                    private org.modelingvalue.collections.Entry<K, V>                 last = null;

                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public V next() {
                        last = it.next();
                        return last.getValue();
                    }

                    @Override
                    public void remove() {
                        set(map -> map.remove(last));
                    }
                };
            }

            @Override
            public Object[] toArray() {
                return get().toValues().toArray();
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> T[] toArray(T[] a) {
                return get().toValues().toArray(i -> (T[]) Array.newInstance(a.getClass().getComponentType(), i));
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean containsAll(java.util.Collection<?> c) {
                Map<K, V> map = get();
                return Stream.of(c).allMatch(v -> map.anyMatch(e -> Objects.equals(e.getValue(), v)));
            }

            @SuppressWarnings("unchecked")
            @Override
            public boolean remove(Object o) {
                return set(map -> {
                    Optional<org.modelingvalue.collections.Entry<K, V>> found = map.filter(e -> Objects.equals(e.getValue(), o)).findFirst();
                    return found.isPresent() ? map.removeKey(found.get().getKey()) : map;
                });
            }

            @Override
            public boolean retainAll(java.util.Collection<?> c) {
                return set(map -> map.removeAll(map.exclude(e -> c.contains(e.getValue()))));
            }

            @Override
            public boolean removeAll(java.util.Collection<?> c) {
                return set(map -> map.removeAll(map.filter(e -> c.contains(e.getValue()))));
            }

            @Override
            public void clear() {
                set(Map::clear);
            }

            @Override
            public boolean add(V e) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean addAll(java.util.Collection<? extends V> c) {
                throw new UnsupportedOperationException();
            }

        };
    }

    @Override
    public java.util.Set<java.util.Map.Entry<K, V>> entrySet() {
        return new java.util.Set<java.util.Map.Entry<K, V>>() {

            @Override
            public int size() {
                return get().size();
            }

            @Override
            public boolean isEmpty() {
                return get().isEmpty();
            }

            @Override
            public boolean contains(Object o) {
                return get().contains(o);
            }

            @Override
            public Iterator<java.util.Map.Entry<K, V>> iterator() {
                return new Iterator<java.util.Map.Entry<K, V>>() {
                    private final Iterator<org.modelingvalue.collections.Entry<K, V>> it   = get().iterator();
                    private org.modelingvalue.collections.Entry<K, V>                 last = null;

                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public java.util.Map.Entry<K, V> next() {
                        last = it.next();
                        return new EntryImpl(last.getKey(), last.getValue());
                    }

                    @Override
                    public void remove() {
                        set(map -> map.remove(last));
                    }
                };
            }

            @Override
            public Object[] toArray() {
                return get().map(null).toArray();
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> T[] toArray(T[] a) {
                return get().map(e -> new EntryImpl(e.getKey(), e.getValue())).toArray(i -> (T[]) Array.newInstance(a.getClass().getComponentType(), i));
            }

            @SuppressWarnings("unchecked")
            @Override
            public boolean remove(Object o) {
                if (o instanceof java.util.Map.Entry) {
                    java.util.Map.Entry<K, V> entry = (java.util.Map.Entry<K, V>) o;
                    return set(map -> map.remove(org.modelingvalue.collections.Entry.of(entry.getKey(), entry.getValue())));
                } else {
                    return false;
                }

            }

            @Override
            public boolean containsAll(java.util.Collection<?> c) {
                Map<K, V> map = get();
                return Stream.of(c).allMatch(map::contains);
            }

            @Override
            public boolean retainAll(java.util.Collection<?> c) {
                return set(map -> map.removeAll(map.exclude(e -> c.contains(new EntryImpl(e.getKey(), e.getValue())))));
            }

            @SuppressWarnings("rawtypes")
            @Override
            public boolean removeAll(java.util.Collection<?> c) {
                return set(map -> map.removeAll(Collection.of(c).map(e -> e instanceof java.util.Map.Entry ? org.modelingvalue.collections.Entry.of(((java.util.Map.Entry) e).getKey(), ((java.util.Map.Entry) e).getValue()) : null).notNull()));
            }

            @Override
            public void clear() {
                set(Map::clear);
            }

            @Override
            public boolean add(java.util.Map.Entry<K, V> e) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean addAll(java.util.Collection<? extends java.util.Map.Entry<K, V>> c) {
                throw new UnsupportedOperationException();
            }

        };
    }

    @Override
    public String toString() {
        return get().toString();
    }

    private final class EntryImpl extends java.util.AbstractMap.SimpleEntry<K, V> {
        private static final long serialVersionUID = 1L;

        private EntryImpl(K key, V value) {
            super(key, value);
        }

        @Override
        public V setValue(V value) {
            return getAndSet(map -> map.put(getKey(), value)).get(getKey());
        }
    }

    private static class Impl<K, V> extends MutableMap<K, V> {

        private Map<K, V> map;

        private Impl(Map<K, V> map) {
            this.map = map;
        }

        @Override
        public Map<K, V> get() {
            return map;
        }

        @Override
        public boolean set(UnaryOperator<Map<K, V>> oper) {
            Map<K, V> pre = map;
            map = oper.apply(pre);
            return pre != map;
        }

        @Override
        public Map<K, V> getAndSet(UnaryOperator<Map<K, V>> oper) {
            Map<K, V> pre = map;
            map = oper.apply(pre);
            return pre;
        }

    }

    private static class ConcurrentImpl<K, V> extends MutableMap<K, V> {

        private AtomicReference<Map<K, V>> ref;

        public ConcurrentImpl(Map<K, V> map) {
            this.ref = new AtomicReference<>(map);
        }

        @Override
        public Map<K, V> get() {
            return ref.get();
        }

        @Override
        public boolean set(UnaryOperator<Map<K, V>> oper) {
            Map<K, V> prev = ref.get(), next = null;
            for (boolean haveNext = false;;) {
                if (!haveNext)
                    next = oper.apply(prev);
                if (ref.compareAndSet(prev, next)) // TeaVM lacks weakCompareAndSetVolatile
                    return prev != next;
                haveNext = (prev == (prev = ref.get()));
            }
        }

        @Override
        public Map<K, V> getAndSet(UnaryOperator<Map<K, V>> oper) {
            return ref.getAndUpdate(oper);
        }

    }
}
