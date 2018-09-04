package com.esaulpaugh.headlong.abi.beta;

import java.math.BigInteger;
import java.util.Arrays;

class BigIntegerType extends AbstractInt256Type<BigInteger> {

    private static final String CLASS_NAME = BigInteger.class.getName();
    private static final String ARRAY_CLASS_NAME_STUB = BigInteger[].class.getName().replaceFirst("\\[", "");

    BigIntegerType(String canonicalType, int bitLength, boolean signed) {
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
    BigInteger decodeStatic(byte[] buffer, int index) {
        BigInteger bi = new BigInteger(Arrays.copyOfRange(buffer, index, index + INT_LENGTH_BYTES));
        validateBigIntBitLen(bi);
        return bi;
    }

    @Override
    void validate(Object object) {
        super.validate(object);
        validateBigIntBitLen((BigInteger) object);
    }
}
