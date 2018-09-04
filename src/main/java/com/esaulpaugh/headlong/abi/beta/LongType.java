package com.esaulpaugh.headlong.abi.beta;

import java.math.BigInteger;
import java.util.Arrays;

class LongType extends AbstractInt256Type<Long> {

    private static final String CLASS_NAME = Long.class.getName();
    private static final String ARRAY_CLASS_NAME_STUB = long[].class.getName().replaceFirst("\\[", "");

    LongType(String canonicalType, int bitLength, boolean signed) {
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
    Long decode(byte[] buffer, int index) {
        BigInteger bi = new BigInteger(Arrays.copyOfRange(buffer, index, index + INT_LENGTH_BYTES));
        long l = bi.longValueExact();
        System.out.println(bi.bitLength());
        validateLongBitLen(l);
        return l;
    }

    @Override
    void validate(Object object) {
        super.validate(object);
        final long longVal = ((Number) object).longValue();
        validateLongBitLen(longVal);
    }
}
