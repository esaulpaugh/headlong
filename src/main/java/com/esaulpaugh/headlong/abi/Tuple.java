/*
   Copyright 2019 Evan Saulpaugh

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.esaulpaugh.headlong.abi;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.IntFunction;

/**
 * An ordered list of objects whose types should correspond to some {@link TupleType}. {@link Function}s encode/decode {@link Tuple}s
 * containing arguments/return values. {@link Tuple}s can contain other tuples. Be warned that changes to elements will affect
 * this {@link Tuple}'s value.
 */
public class Tuple implements Iterable<Object> {

    public static final Tuple EMPTY = new Tuple();
    private static final String SKIPPED = "_";

    final Object[] elements;

    Tuple(Object... elements) {
        this.elements = elements;
    }

    public static Tuple of() {
        return Tuple.EMPTY;
    }

    public static <A, B> Pair<A, B> of(A a, B b) {
        return new Pair<>(requireNoNulls(new Object[] { a, b }));
    }

    public static <A, B, C> Triple<A, B, C> of(A a, B b, C c) {
        return new Triple<>(requireNoNulls(new Object[] { a, b, c }));
    }

    public static <A, B, C, D> Quadruple<A, B, C, D> of(A a, B b, C c, D d) {
        return new Quadruple<>(requireNoNulls(new Object[] { a, b, c, d }));
    }

    public static <A, B, C, D, E> Quintuple<A, B, C, D, E> of(A a, B b, C c, D d, E e) {
        return new Quintuple<>(requireNoNulls(new Object[] { a, b, c, d, e }));
    }

    public static <A, B, C, D, E, F> Sextuple<A, B, C, D, E, F> of(A a, B b, C c, D d, E e, F f) {
        return new Sextuple<>(requireNoNulls(new Object[] { a, b, c, d, e, f }));
    }

    public static Tuple from(Object... elements) {
        return create(copy(new Object[elements.length], i -> requireNotNull(elements[i], i)));
    }

    @SuppressWarnings("unchecked")
    private static <T extends Tuple> T create(Object[] elements) {
        switch (elements.length) {
        case 1: return (T) new Singleton<>(elements);
        case 2: return (T) new Pair<>(elements);
        case 3: return (T) new Triple<>(elements);
        case 4: return (T) new Quadruple<>(elements);
        case 5: return (T) new Quintuple<>(elements);
        case 6: return (T) new Sextuple<>(elements);
        default: return (T) new Tuple(elements);
        }
    }

    private static Object[] requireNoNulls(Object[] elements) {
        for (int i = 0; i < elements.length; i++) {
            requireNotNull(elements[i], i);
        }
        return elements;
    }

    static Object requireNotNull(Object e, int index) {
        if (e == null) {
            throw new IllegalArgumentException("tuple index " + index + " is null");
        }
        return e;
    }

    /**
     * Returns the element at the specified position in this tuple. Be warned that changes to elements will affect this
     * {@link Tuple}'s value. Consider making a {@link #deepCopy()} before calling this method.
     *
     * @param index index of the element to return
     * @return  the element at the specified position
     * @param <T>   the element's type
     * @throws NoSuchElementException if the element is absent due to being skipped during decode
     */
    @SuppressWarnings("unchecked")
    public final <T> T get(int index) {
        Object val = elements[index];
        if (val == null) {
            // an element should only be null as a result of a decode-with-indices in which this index wasn't specified
            throw new NoSuchElementException("" + index);
        }
        return (T) val;
    }

    /**
     * Returns true if and only if the given index is populated with a value. This should always return true unless this
     * {@link Tuple} is the result of a decode-with-indices (such as {@link TupleType#decode(java.nio.ByteBuffer,int...)}), in
     * which case the only elements not present will be those which were deliberately skipped. It is advised to rely on
     * this method only when the populated indices cannot be divined by other means.
     *
     * @param index the position of the element in the Tuple
     * @return  false if the element is absent due to being skipped during decode, true otherwise
     */
    public final boolean elementIsPresent(int index) {
        return elements[index] != null;
    }

    public final int size() {
        return elements.length;
    }

    public final boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public final int hashCode() {
        return Arrays.deepHashCode(elements);
    }

    @Override
    public final boolean equals(Object o) {
        return o == this || (o instanceof Tuple && Arrays.deepEquals(this.elements, ((Tuple) o).elements));
    }

    @Override
    public final String toString() {
        return Arrays.deepToString(copy(new Object[elements.length], i -> {
            Object element = elements[i];
            if (element == null) return SKIPPED;
            String str = element.toString();
            return element instanceof String
                        ? '"' + str + '"'
                        : SKIPPED.equals(str)
                            ? '\\' + SKIPPED
                            : element;
        }));
    }

    private static Object[] copy(Object[] copy, IntFunction<Object> extractor) {
        for (int i = 0; i < copy.length; i++) {
            copy[i] = extractor.apply(i);
        }
        return copy;
    }

    @Override
    public final Iterator<Object> iterator() {
        return Arrays.asList(elements).iterator();
    }

    /**
     * Returns a deep copy of this tuple. Non-array, non-tuple objects are assumed to be immutable and are copied via
     * reference.
     *
     * @return  an independent copy of this tuple
     */
    public final <T extends Tuple> T deepCopy() {
        return create(copy(new Object[elements.length], i -> deepCopyElement(elements[i])));
    }

    /**
     * Returns a shallow copy of the underlying array of elements. Elements may be null if this {@link Tuple} is the result
     * of a decode-with-indices. Be warned that changes to elements will affect this {@link Tuple}'s value. Consider making
     * a {@link #deepCopy()} before calling this method.
     *
     * @return  a shallow copy of the elements array
     */
    public final Object[] toArray() {
        return Arrays.copyOf(elements, elements.length);
    }

    private static Object deepCopyElement(Object e) {
        final Class<?> c = e.getClass();
        if(c.isArray()) {
            if (e instanceof Object[]) {
                final Object[] original = (Object[]) e;
                return copy(ArrayType.createArray(c.getComponentType(), original.length), i -> deepCopyElement(original[i]));
            }
            if (e instanceof byte[]) {
                final byte[] bytes = (byte[]) e;
                return Arrays.copyOf(bytes, bytes.length);
            }
            if (e instanceof boolean[]) {
                final boolean[] booleans = (boolean[]) e;
                return Arrays.copyOf(booleans, booleans.length);
            }
            if (e instanceof int[]) {
                final int[] ints = (int[]) e;
                return Arrays.copyOf(ints, ints.length);
            }
            if (e instanceof long[]) {
                final long[] longs = (long[]) e;
                return Arrays.copyOf(longs, longs.length);
            }
            throw new IllegalArgumentException(); // float, double, char, short
        }
        return e instanceof Tuple ? ((Tuple) e).deepCopy() : e;
    }
}
