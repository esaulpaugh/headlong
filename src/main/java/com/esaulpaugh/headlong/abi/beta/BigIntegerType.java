package com.esaulpaugh.headlong.abi.beta;

import com.esaulpaugh.headlong.abi.beta.util.Utils;

import java.math.BigInteger;
import java.nio.ByteBuffer;

class BigIntegerType extends AbstractUnitType<BigInteger> {

    static final String CLASS_NAME = BigInteger.class.getName();
    static final String ARRAY_CLASS_NAME_STUB = Utils.getNameStub(BigInteger[].class);

    BigIntegerType(String canonicalType, int bitLength, boolean unsigned) {
        super(canonicalType, bitLength, unsigned);
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
    int typeCode() {
        return TYPE_CODE_BIG_INTEGER;
    }

    @Override
    int validate(Object object) {
        super.validate(object);
        validateBigIntBitLen((BigInteger) object);
        return UNIT_LENGTH_BYTES;
    }

    @Override
    BigInteger decode(ByteBuffer bb, byte[] unitBuffer) {
        bb.get(unitBuffer, 0, UNIT_LENGTH_BYTES);
        BigInteger bi = new BigInteger(unitBuffer);
        validateBigIntBitLen(bi);
        return bi;
    }
}
