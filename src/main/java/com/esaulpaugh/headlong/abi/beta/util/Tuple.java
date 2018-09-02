package com.esaulpaugh.headlong.abi.beta.util;

import java.util.Arrays;

public class Tuple {

    public static final Tuple EMPTY = new Tuple();

    public final Object[] elements;

    public Tuple(Object... elements) {
        this.elements = elements;
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
                && Arrays.deepEquals(elements, ((Tuple) object).elements);
    }
}
