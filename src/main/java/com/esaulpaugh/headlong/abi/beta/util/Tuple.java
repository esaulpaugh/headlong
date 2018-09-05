package com.esaulpaugh.headlong.abi.beta.util;

import java.util.Arrays;

public class Tuple {

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

    @Override
    public int hashCode() {
        int result = 1;
        for (Object obj : elements) {
            result = 31 * result + obj.hashCode();
        }
        return result;
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof Tuple
                && Arrays.deepEquals(this.elements, ((Tuple) object).elements);
    }
}
