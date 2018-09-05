package com.esaulpaugh.headlong.abi.beta;

import java.math.BigInteger;

class ByteType extends AbstractInt256Type<Byte> {

    private static final String CLASS_NAME = Byte.class.getName();
    private static final String ARRAY_CLASS_NAME_STUB = byte[].class.getName().replaceFirst("\\[", "");

    private static final int MAX_BIT_LEN = 8;

//    static final ByteType SIGNED_BYTE_OBJECT = new ByteType("int8", true);
//    static final ByteType SIGNED_BYTE_PRIMITIVE = new ByteType("int8", "B", true);
    static final ByteType UNSIGNED_BYTE_OBJECT = new ByteType("uint8", false);
//    static final ByteType UNSIGNED_BYTE_PRIMITIVE = new ByteType("uint8", "B", false);

    ByteType(String canonicalType, boolean signed) {
        super(canonicalType, MAX_BIT_LEN, signed);
    }

    @Override
    Byte decode(byte[] buffer, int index) {
        byte[] copy = new byte[INT_LENGTH_BYTES];
        System.arraycopy(buffer, index, copy, 0, INT_LENGTH_BYTES);
        BigInteger bi = new BigInteger(copy);
        return bi.byteValueExact();
    }

    @Override
    String className() {
        return CLASS_NAME;
    }

    @Override
    String arrayClassNameStub() {
        return ARRAY_CLASS_NAME_STUB;
    }
}
