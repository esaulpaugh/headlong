package com.esaulpaugh.headlong.abi.beta;

import java.math.BigInteger;
import java.util.Arrays;

class ByteType extends AbstractInt256Type<Byte> {

    private static final String CLASS_NAME = Byte.class.getName();
    private static final String ARRAY_CLASS_NAME_STUB = byte[].class.getName().replaceFirst("\\[", "");

    private static final int MAX_BIT_LEN = 8;

    static final ByteType SIGNED_BYTE_OBJECT = new ByteType("int8", true);
//    static final ByteType SIGNED_BYTE_PRIMITIVE = new ByteType("int8", "B", true);

    static final ByteType UNSIGNED_BYTE_OBJECT = new ByteType("uint8", false);
//    static final ByteType UNSIGNED_BYTE_PRIMITIVE = new ByteType("uint8", "B", false);

    ByteType(String canonicalType, boolean signed) {
        super(canonicalType, MAX_BIT_LEN, signed);
    }

    @Override
    Byte decode(byte[] buffer, int index) {
        BigInteger bi = new BigInteger(Arrays.copyOfRange(buffer, index, index + INT_LENGTH_BYTES));
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
