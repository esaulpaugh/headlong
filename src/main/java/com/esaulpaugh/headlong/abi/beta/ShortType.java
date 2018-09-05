package com.esaulpaugh.headlong.abi.beta;

import java.math.BigInteger;

class ShortType extends AbstractInt256Type<Short> {

    private static final String CLASS_NAME = Short.class.getName();
    private static final String ARRAY_CLASS_NAME_STUB = short[].class.getName().replaceFirst("\\[", "");

    private static final int SHORT_LENGTH_BITS = 16;

    ShortType(String canonicalType, boolean signed) {
        super(canonicalType, SHORT_LENGTH_BITS, signed);
    }

    @Override
    String className() {
        return CLASS_NAME;
    }

    @Override
    String arrayClassNameStub() {
        return ARRAY_CLASS_NAME_STUB;
    }

    @Override
    Short decode(byte[] buffer, int index) {
        byte[] copy = new byte[INT_LENGTH_BYTES];
        System.arraycopy(buffer, index, copy, 0, INT_LENGTH_BYTES);
        BigInteger bi = new BigInteger(copy);
        return bi.shortValueExact();
    }
}
