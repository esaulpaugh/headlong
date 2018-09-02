package com.esaulpaugh.headlong.abi.beta;

import java.math.BigInteger;
import java.util.Arrays;

class ShortType extends AbstractInt256Type<Short> {

    static final String CLASS_NAME = Short.class.getName();
    static final String CLASS_NAME_ELEMENT = short[].class.getName().replaceFirst("\\[", "");

    private static final int SHORT_LENGTH_BITS = 16;

    ShortType(String canonicalAbiType, String className) {
        super(canonicalAbiType, className, SHORT_LENGTH_BITS);
    }

    @Override
    Short decode(byte[] buffer, int index) {
        BigInteger bi = new BigInteger(Arrays.copyOfRange(buffer, index, index + INT_LENGTH_BYTES));
        return bi.shortValueExact();
    }
}
