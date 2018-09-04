package com.esaulpaugh.headlong.abi.beta;

import java.math.BigInteger;
import java.util.Arrays;

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
    Short decodeStatic(byte[] buffer, int index) {
        BigInteger bi = new BigInteger(Arrays.copyOfRange(buffer, index, index + INT_LENGTH_BYTES));
        return bi.shortValueExact();
    }
}
