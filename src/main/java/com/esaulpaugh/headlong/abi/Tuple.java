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

/**
 * An ordered list of objects whose types should correspond to some {@link TupleType}. {@link Function}s encode/decode
 * {@link Tuple}s containing arguments/return values. {@link Tuple}s can contain other tuples.
 */
public final class Tuple implements Iterable<Object> {

    public static final Tuple EMPTY = new Tuple();
    private static final String SKIPPED = "_";

    final Object[] elements;

    Tuple(Object... elements) {
        this.elements = elements;
    }

    public static Tuple of(Object... elements) {
        final Object[] shallowCopy = new Object[elements.length];
        for (int i = 0; i < elements.length; i++) {
            Object e = elements[i];
            checkNotNull(e, i);
            shallowCopy[i] = e;
        }
        return new Tuple(shallowCopy);
    }

    public static Tuple singleton(Object element) {
        checkNotNull(element, 0);
        return new Tuple(element);
    }

    private static void checkNotNull(Object e, int index) {
        if (e == null) {
            throw new IllegalArgumentException("tuple index " + index + " is null");
        }
    }

    /**
     * Returns the element at the specified position in this tuple.
     *
     * @param index index of the element to return
     * @return  the element at the specified position
     * @param <T>   the element's type
     * @throws NoSuchElementException if the element is absent due to being skipped during decode
     */
    @SuppressWarnings("unchecked")
    public <T> T get(int index) {
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
    public boolean elementIsPresent(int index) {
        return elements[index] != null;
    }

    public int size() {
        return elements.length;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(elements);
    }

    @Override
    public boolean equals(Object o) {
        return o == this || (o instanceof Tuple && Arrays.deepEquals(((Tuple) o).elements, this.elements));
    }

    @Override
    public String toString() {
        final Object[] copy = new Object[elements.length];
        for (int i = 0; i < elements.length; i++) {
            copy[i] = normalize(elements[i]);
        }
        return Arrays.deepToString(copy);
    }

    private static Object normalize(Object element) {
        if(element == null) return SKIPPED;
        return SKIPPED.equals(element.toString()) ? '"' + SKIPPED + '"' : element;
    }

    @Override
    public Iterator<Object> iterator() {
        return Arrays.asList(elements).iterator();
    }
}
