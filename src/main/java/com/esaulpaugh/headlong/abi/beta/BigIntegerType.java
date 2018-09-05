package com.esaulpaugh.headlong.abi.beta;

import java.math.BigInteger;

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
    BigInteger decode(byte[] buffer, int index) {
        byte[] copy = new byte[INT_LENGTH_BYTES];
        System.arraycopy(buffer, index, copy, 0, INT_LENGTH_BYTES);
        BigInteger bi = new BigInteger(copy);
        validateBigIntBitLen(bi);
        return bi;
    }

    @Override
    void validate(Object object) {
        super.validate(object);
        validateBigIntBitLen((BigInteger) object);
    }
}
