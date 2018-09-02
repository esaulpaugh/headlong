package com.esaulpaugh.headlong.abi.beta;

import java.math.BigInteger;
import java.util.Arrays;

class LongType extends AbstractInt256Type<Long> {

    static final String CLASS_NAME = Long.class.getName();
    static final String CLASS_NAME_ELEMENT = long[].class.getName().replaceFirst("\\[", "");

    static final int MAX_BIT_LEN = 64;

    LongType(String canonicalAbiType, String className, int bitLength, boolean signed) {
        super(canonicalAbiType, className, bitLength, signed);
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
