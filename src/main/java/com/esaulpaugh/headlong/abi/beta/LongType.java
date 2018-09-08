package com.esaulpaugh.headlong.abi.beta;

import java.math.BigInteger;
import java.nio.ByteBuffer;

class LongType extends AbstractUnitType<Long> {

    private static final String CLASS_NAME = Long.class.getName();
//    private static final String ARRAY_CLASS_NAME_STUB = long[].class.getName().replaceFirst("\\[", "");

    LongType(String canonicalType, int bitLength, boolean unsigned) {
        super(canonicalType, bitLength, unsigned);
    }

    @Override
    String className() {
        return CLASS_NAME;
    }

    @Override
    Long decode(ByteBuffer bb, byte[] unitBuffer) {
        bb.get(unitBuffer, 0, UNIT_LENGTH_BYTES);
        BigInteger bi = new BigInteger(unitBuffer);
        long l = bi.longValueExact();
        validateLongBitLen(l);
        return l;
    }

    @Override
    int typeCode() {
        return TYPE_CODE_LONG;
    }

    @Override
    void validate(Object object) {
        super.validate(object);
        final long longVal = ((Number) object).longValue();
        validateLongBitLen(longVal);
    }
}
