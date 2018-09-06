package com.esaulpaugh.headlong.abi.beta;

import java.math.BigInteger;
import java.nio.ByteBuffer;

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
    Short decode(ByteBuffer bb, byte[] elementBuffer) {
        bb.get(elementBuffer);
        BigInteger bi = new BigInteger(elementBuffer);
        return bi.shortValueExact();
    }
}
