package com.esaulpaugh.headlong.abi.beta;

import java.math.BigInteger;
import java.nio.ByteBuffer;

class BigIntegerType extends AbstractUnitType<BigInteger> {

    private static final String CLASS_NAME = BigInteger.class.getName();
//    private static final String ARRAY_CLASS_NAME_STUB = BigInteger[].class.getName().replaceFirst("\\[", "");

    BigIntegerType(String canonicalType, int bitLength, boolean unsigned) {
        super(canonicalType, bitLength, unsigned);
    }

    @Override
    String className() {
        return CLASS_NAME;
    }

    @Override
    BigInteger decode(ByteBuffer bb, byte[] unitBuffer) {
        bb.get(unitBuffer, 0, UNIT_LENGTH_BYTES);
        BigInteger bi = new BigInteger(unitBuffer);
        validateBigIntBitLen(bi);
        return bi;
    }

    @Override
    int typeCode() {
        return TYPE_CODE_BIG_INTEGER;
    }

    @Override
    void validate(Object object) {
        super.validate(object);
        validateBigIntBitLen((BigInteger) object);
    }
}
