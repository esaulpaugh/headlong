package com.esaulpaugh.headlong.abi.util;

import java.io.Serializable;
import java.util.Arrays;

public class Tuple implements Serializable {

    public static final Tuple EMPTY = new Tuple();

    public final Object[] elements;

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
        return new Tuple(Arrays.copyOfRange(elements, startIndex, endIndex));
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
