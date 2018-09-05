package com.esaulpaugh.headlong.abi.beta;

import java.math.BigInteger;

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
    Integer decode(byte[] buffer, int index) {
        byte[] copy = new byte[INT_LENGTH_BYTES];
        System.arraycopy(buffer, index, copy, 0, INT_LENGTH_BYTES);
        BigInteger bi = new BigInteger(copy);
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
