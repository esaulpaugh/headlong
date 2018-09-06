package com.esaulpaugh.headlong.abi.beta;

import java.math.BigInteger;
import java.nio.ByteBuffer;

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
    BigInteger decode(ByteBuffer bb, byte[] elementBuffer) {
        bb.get(elementBuffer);
        BigInteger bi = new BigInteger(elementBuffer);
        validateBigIntBitLen(bi);
        return bi;
    }

    @Override
    void validate(Object object) {
        super.validate(object);
        validateBigIntBitLen((BigInteger) object);
    }
}
