package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.abi.util.ClassNames;

import java.math.BigInteger;
import java.nio.ByteBuffer;

class ByteType extends UnitType<Byte> {

    static final Class<?> CLASS = Byte.class;
    private static final String ARRAY_CLASS_NAME_STUB = ClassNames.getArrayClassNameStub(byte[].class);

    private static final int MAX_BIT_LEN = 8;

    static final ByteType SIGNED = new ByteType("int8", false);
    static final ByteType UNSIGNED = new ByteType("uint8", true);

    private ByteType(String canonicalType, boolean unsigned) {
        super(canonicalType, CLASS, MAX_BIT_LEN, unsigned);
    }

    @Override
    String arrayClassNameStub() {
        return ARRAY_CLASS_NAME_STUB;
    }

    @Override
    int typeCode() {
        return TYPE_CODE_BYTE;
    }

    @Override
    int byteLengthPacked(Object value) {
        return 1;
    }

    @Override
    public Byte parseArgument(String s) {
        Byte b = Byte.parseByte(s);
        validate(b);
        return b;
    }

    @Override
    public int validate(Object value) {
        validateClass(value);
        return UNIT_LENGTH_BYTES;
    }

    @Override
    Byte decode(ByteBuffer bb, byte[] unitBuffer) {
        bb.get(unitBuffer, 0, UNIT_LENGTH_BYTES);
        BigInteger bi = new BigInteger(unitBuffer);
        validateBigIntBitLen(bi);
        return bi.byteValue();
    }
}
