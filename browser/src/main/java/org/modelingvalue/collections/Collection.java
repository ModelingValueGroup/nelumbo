//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//  (C) Copyright 2018-2026 Modeling Value Group B.V. (http://modelingvalue.org)                                         ~
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

package org.modelingvalue.collections;

// OVERLAY of immutable-collections Collection (from ../immutable-collections, API-compatible with 6.0.2)
// for the TeaVM (browser) build - delta: PARALLELISM = 1 (no ForkJoinPool under TeaVM).

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.BaseStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.modelingvalue.collections.impl.StreamCollectionImpl;
import org.modelingvalue.collections.mutable.Mutable;
import org.modelingvalue.collections.util.Mergeable;
import org.modelingvalue.collections.util.SerializableFunction;
import org.modelingvalue.collections.util.TriConsumer;
import org.modelingvalue.collections.util.TriFunction;

@SuppressWarnings("unused")
public interface Collection<T> extends Stream<T>, Iterable<T>, Serializable {

    int                  PARALLELISM     = 1;

    ThreadLocal<Boolean> SEQUENTIAL_ONLY = ThreadLocal.withInitial(() -> false);

    static Runnable sequential(Runnable runnable) {
        return () -> {
            boolean old = SEQUENTIAL_ONLY.get();
            SEQUENTIAL_ONLY.set(true);
            try {
                runnable.run();
            } finally {
                SEQUENTIAL_ONLY.set(old);
            }
        };
    }

    static <T> T getSequential(Supplier<T> supplier) {
        boolean old = SEQUENTIAL_ONLY.get();
        SEQUENTIAL_ONLY.set(true);
        try {
            return supplier.get();
        } finally {
            SEQUENTIAL_ONLY.set(old);
        }
    }

    @Override
    Spliterator<T> spliterator();

    @Override
    Iterator<T> iterator();

    int size();

    boolean isEmpty();

    boolean contains(Object e);

    default boolean notContains(Object e) {
        return !contains(e);
    }

    <F extends T> Collection<F> filter(Class<F> type);

    Collection<T> notNull();

    Collection<T> requireNonNull();

    @Override
    Collection<T> filter(Predicate<? super T> predicate);

    default Collection<T> exclude(Predicate<? super T> predicate) {
        return filter(predicate.negate());
    }

    Optional<T> findAny(Predicate<? super T> predicate);

    Optional<T> findFirst(Predicate<? super T> predicate);

    @Override
    <R> Collection<R> map(Function<? super T, ? extends R> mapper);

    @Override
    <R> Collection<R> flatMap(Function<? super T, ? extends java.util.stream.Stream<? extends R>> mapper);

    @Override
    Collection<T> distinct();

    @Override
    Collection<T> sorted();

    Collection<T> random();

    @Override
    Collection<T> sorted(Comparator<? super T> comparator);

    @SuppressWarnings({"rawtypes", "unchecked"})
    default <C extends Comparable> Collection<T> sortedBy(Function<T, C> by) {
        return sorted((o1, o2) -> by.apply(o1).compareTo(by.apply(o2)));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    default <C extends Comparable> Collection<T> sortedByDesc(Function<T, C> by) {
        return sorted((o1, o2) -> by.apply(o2).compareTo(by.apply(o1)));
    }

    @Override
    Collection<T> peek(Consumer<? super T> action);

    @Override
    Collection<T> limit(long maxSize);

    @Override
    Collection<T> skip(long n);

    @Override
    Collection<T> sequential();

    @Override
    Collection<T> parallel();

    @Override
    Collection<T> unordered();

    @Override
    Collection<T> onClose(Runnable closeHandler);

    @Override
    void forEach(Consumer<? super T> action);

    default Set<T> asSet() {
        return reduce(Set.of(), Set::add, Set::addAll);
    }

    default List<T> asList() {
        return reduce(List.of(), List::append, List::appendList);
    }

    default <K, V> Map<K, V> asMap(Function<T, Entry<K, V>> entry) {
        return reduce(Map.of(), (s, a) -> s.put(entry.apply(a)), Map::putAll);
    }

    default <K, V> DefaultMap<K, V> asDefaultMap(SerializableFunction<K, V> defaultFunction, Function<T, Entry<K, V>> entry) {
        return reduce(DefaultMap.of(defaultFunction), (s, a) -> s.put(entry.apply(a)), DefaultMap::putAll);
    }

    @SuppressWarnings("unchecked")
    default <K, V> QualifiedSet<K, V> asQualifiedSet(SerializableFunction<V, K> qualifier) {
        return reduce(QualifiedSet.of(qualifier), (s, a) -> s.add((V) a), QualifiedSet::addAll);
    }

    @SuppressWarnings("rawtypes")
    static <T> Collection<T> of(BaseStream<T, ? extends BaseStream> base) {
        return new StreamCollectionImpl<>(base);
    }

    static <T> Collection<T> of(Stream<T> base) {
        return base instanceof Collection ? (Collection<T>) base : new StreamCollectionImpl<>(base);
    }

    static <T> Collection<T> of(Spliterator<T> base) {
        return new StreamCollectionImpl<>(base);
    }

    @SuppressWarnings("unchecked")
    static <T> Collection<T> of(Iterable<T> base) {
        return base instanceof Collection ? (Collection<T>) base : base instanceof Mutable ? ((Mutable<T>) base).toImmutable() : new StreamCollectionImpl<>(base);
    }

    static <T> Collection<T> of(Supplier<T> base) {
        return new StreamCollectionImpl<>(Stream.generate(base));
    }

    @SafeVarargs
    static <T> Collection<T> of(T... elements) {
        return new StreamCollectionImpl<>(Stream.of(elements));
    }

    static Collection<Integer> range(int from, int to) {
        return of(IntStream.range(from, to));
    }

    static Collection<Integer> range(int size) {
        return range(0, size);
    }

    <U extends Mergeable<U>> U reduce(U identity, BiFunction<U, ? super T, U> accumulator);

    <R> Collection<R> linked(TriFunction<T, T, T, R> function);

    void linked(TriConsumer<T, T, T> consumer);

    <R> Collection<R> indexed(BiFunction<T, Integer, R> function);

    static <E> Collection<E> concat(Collection<? extends E> a, Collection<? extends E> b, Collection<? extends E> c, Collection<? extends E> d) {
        return Collection.of(Stream.concat(Stream.concat(Stream.concat(a, b), c), d));
    }

    static <E> Collection<E> concat(Collection<? extends E> a, Collection<? extends E> b, Collection<? extends E> c) {
        return Collection.of(Stream.concat(Stream.concat(a, b), c));
    }

    static <E> Collection<E> concat(Collection<? extends E> a, Collection<? extends E> b) {
        return Collection.of(Stream.concat(a, b));
    }

    static <E> Collection<E> concat(Collection<? extends E> a, E b) {
        return Collection.of(Stream.concat(a, Collection.of(b)));
    }

    static <E> Collection<E> concat(E a, Collection<? extends E> b) {
        return Collection.of(Stream.concat(Collection.of(a), b));
    }

}
