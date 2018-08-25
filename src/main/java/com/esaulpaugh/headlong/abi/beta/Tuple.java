package com.esaulpaugh.headlong.abi.beta;

public class Tuple {

    public static final Tuple EMPTY = new Tuple();

    public final Object[] elements;

    public Tuple(Object... elements) {
        this.elements = elements;
    }
}
