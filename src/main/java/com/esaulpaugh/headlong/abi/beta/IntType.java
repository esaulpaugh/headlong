package com.esaulpaugh.headlong.abi.beta;

import java.math.BigInteger;
import java.nio.ByteBuffer;

class IntType extends AbstractInt256Type<Integer> {

    private static final String CLASS_NAME = Integer.class.getName();
    private static final String ARRAY_CLASS_NAME_STUB = int[].class.getName().replaceFirst("\\[", "");

    static final int MAX_BIT_LEN = 32;

    IntType(String canonicalType, int bitLength, boolean signed) {
        super(canonicalType, bitLength, signed);
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
    Integer decode(ByteBuffer bb, byte[] elementBuffer) {
        bb.get(elementBuffer);
        BigInteger bi = new BigInteger(elementBuffer);
        long longVal = bi.longValueExact();
        validateLongBitLen(longVal);
        return (int) longVal;
    }

    @Override
    void validate(Object object) {
        super.validate(object);
        final long longVal = ((Number) object).longValue();
        validateLongBitLen(longVal);
    }
}
