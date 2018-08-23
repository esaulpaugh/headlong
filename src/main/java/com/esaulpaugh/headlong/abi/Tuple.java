package com.esaulpaugh.headlong.abi;

public class Tuple {

    public static final Tuple EMPTY = new Tuple();

    public final Object[] elements;

//    public final int byteLen;

    public Tuple(Object... elements) {
        this.elements = elements;
//        this.byteLen = 32 * elements.length; // TODO calc fo realz
    }
}
