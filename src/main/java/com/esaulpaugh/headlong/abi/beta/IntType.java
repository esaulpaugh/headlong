package com.esaulpaugh.headlong.abi.beta;

import java.math.BigInteger;
import java.util.Arrays;

class IntType extends AbstractInt256Type<Integer> {

    static final IntType OFFSET_TYPE = new IntType("uint32", IntType.CLASS_NAME, IntType.MAX_BIT_LEN, false);

    static final String CLASS_NAME = Integer.class.getName();
    static final String CLASS_NAME_ELEMENT = int[].class.getName().replaceFirst("\\[", "");

    static final int MAX_BIT_LEN = 32;

    IntType(String canonicalAbiType, String className, int bitLength, boolean signed) {
        super(canonicalAbiType, className, bitLength, signed);
    }

    @Override
    Integer decode(byte[] buffer, int index) {
        BigInteger bi = new BigInteger(Arrays.copyOfRange(buffer, index, index + INT_LENGTH_BYTES));
        int i = bi.intValueExact();
        System.out.println(bi.bitLength());
        validateLongBitLen((long) i);
        return i;
    }

    @Override
    void validate(Object object) {
        super.validate(object);
        final long longVal = ((Number) object).longValue();
        validateLongBitLen(longVal);
    }
}
