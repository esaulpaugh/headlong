package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.abi.util.ClassNames;

import java.math.BigInteger;
import java.nio.ByteBuffer;

class IntType extends UnitType<Integer> {

    static final Class<?> CLASS = Integer.class;
    private static final String ARRAY_CLASS_NAME_STUB = ClassNames.getArrayClassNameStub(int[].class);

    IntType(String canonicalType, int bitLength, boolean unsigned) {
        super(canonicalType, CLASS, bitLength, unsigned);
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
    int byteLengthPacked(Object value) {
        return bitLength >> 3; // div 8
    }

    @Override
    public Integer parseArgument(String s) {
        Integer in = Integer.parseInt(s);
        validate(in);
        return in;
    }

    @Override
    public int validate(Object value) {
        validateClass(value);
        final long longVal = ((Number) value).longValue();
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
