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

import java.util.AbstractList;
import java.util.Arrays;
import java.util.RandomAccess;

/**
 * An ordered list of objects whose types should correspond to some {@link TupleType}. {@link Function}s encode/decode
 * {@link Tuple}s containing arguments satisfying its parameters/return type. {@link Tuple}s can contain other tuples.
 */
public final class Tuple extends AbstractList<Object> implements RandomAccess {

    public static final Tuple EMPTY = new Tuple();

    final Object[] elements;

    public Tuple(Object... elements) {
        this.elements = Arrays.copyOf(elements, elements.length); // shallow copy
    }

    @Override
    public Object get(int index) {
        return elements[index];
    }

    @Override
    public int size() {
        return elements.length;
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
        return Arrays.deepToString(elements);
    }

    public Tuple subtuple(int startIndex, int endIndex) {
        return new Tuple(Arrays.copyOfRange(elements, startIndex, endIndex));
    }

    public static Tuple of(Object... elements) {
        return new Tuple(elements);
    }

    public static Tuple singleton(Object element) {
        return new Tuple(element);
    }
}
