package com.esaulpaugh.headlong.abi;

import java.io.Serializable;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.RandomAccess;

public class Tuple extends AbstractList<Object> implements RandomAccess, Serializable {

    private static final long serialVersionUID = -6849167468060212408L;

    public static final Tuple EMPTY = new Tuple();

    final Object[] elements;

    public Tuple(Object... elements) {
        this.elements = elements;
    }

    public static Tuple singleton(Object element) {
        return new Tuple(element);
    }

    public static Tuple withElements(Object... elements) {
        return new Tuple(elements);
    }

    public static Tuple wrap(Object[] elements) {
        return new Tuple(elements);
    }

    public Tuple subtuple(int startIndex, int endIndex) {
        final int len = endIndex - startIndex;
        Object[] copy = new Object[len];
        System.arraycopy(elements, startIndex, copy, 0, len);
        return new Tuple(copy);
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tuple other = (Tuple) o;
        return Arrays.deepEquals(this.elements, other.elements);
    }
}
