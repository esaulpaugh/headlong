package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.abi.util.Utils;

import java.math.BigInteger;
import java.nio.ByteBuffer;

class IntType extends AbstractUnitType<Integer> {

    static final String CLASS_NAME = Integer.class.getName();
    static final String ARRAY_CLASS_NAME_STUB = Utils.getNameStub(int[].class);

    static final int MAX_BIT_LEN = 32;

    IntType(String canonicalType, int bitLength, boolean unsigned) {
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
        return TYPE_CODE_INT;
    }

    @Override
    int validate(Object object) {
        super.validate(object);
        final long longVal = ((Number) object).longValue();
        validateLongBitLen(longVal);
        return UNIT_LENGTH_BYTES;
    }

    @Override
    Integer decode(ByteBuffer bb, byte[] unitBuffer) {
        bb.get(unitBuffer, 0, UNIT_LENGTH_BYTES);
        BigInteger bi = new BigInteger(unitBuffer);
        validateBigIntBitLen(bi);
        return bi.intValue();
    }
}
