package com.esaulpaugh.headlong.abi;

import com.esaulpaugh.headlong.abi.util.ClassNames;

import java.math.BigInteger;
import java.nio.ByteBuffer;

class ByteType extends AbstractUnitType<Byte> {

    static final String CLASS_NAME = Byte.class.getName();
    static final String ARRAY_CLASS_NAME_STUB = ClassNames.getArrayClassNameStub(byte[].class);

    private static final int MAX_BIT_LEN = 8;

//    static final ByteType SIGNED_BYTE_OBJECT = new ByteType("int8", true);
//    static final ByteType SIGNED_BYTE_PRIMITIVE = new ByteType("int8", "B", true);
    static final ByteType UNSIGNED_BYTE_OBJECT = new ByteType("uint8", true);
//    static final ByteType UNSIGNED_BYTE_PRIMITIVE = new ByteType("uint8", "B", false);

    ByteType(String canonicalType, boolean unsigned) {
        super(canonicalType, MAX_BIT_LEN, unsigned);
    }

    @Override
    public String className() {
        return CLASS_NAME;
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
    Byte decode(ByteBuffer bb, byte[] unitBuffer) {
        bb.get(unitBuffer, 0, UNIT_LENGTH_BYTES);
        BigInteger bi = new BigInteger(unitBuffer);
        validateBigIntBitLen(bi);
        return bi.byteValue();
    }
}
