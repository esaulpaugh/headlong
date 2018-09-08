package com.esaulpaugh.headlong.abi.beta;

import java.math.BigInteger;
import java.nio.ByteBuffer;

class ShortType extends AbstractUnitType<Short> {

    private static final String CLASS_NAME = Short.class.getName();
//    private static final String ARRAY_CLASS_NAME_STUB = short[].class.getName().replaceFirst("\\[", "");

    private static final int SHORT_LENGTH_BITS = 16;

    ShortType(String canonicalType, boolean signed) {
        super(canonicalType, SHORT_LENGTH_BITS, signed);
    }

    @Override
    String className() {
        return CLASS_NAME;
    }

    @Override
    Short decode(ByteBuffer bb, byte[] unitBuffer) {
        bb.get(unitBuffer, 0, UNIT_LENGTH_BYTES);
        BigInteger bi = new BigInteger(unitBuffer);
        return bi.shortValueExact();
    }
}
