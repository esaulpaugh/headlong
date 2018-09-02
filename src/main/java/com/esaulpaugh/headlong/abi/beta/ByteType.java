package com.esaulpaugh.headlong.abi.beta;

import java.math.BigInteger;
import java.util.Arrays;

import static com.esaulpaugh.headlong.abi.beta.ArrayType.roundUp;

class ByteType extends AbstractInt256Type<Byte> {

    static final String CLASS_NAME = Byte.class.getName();
    static final String CLASS_NAME_ELEMENT = byte[].class.getName().replaceFirst("\\[", "");

    private static final int MAX_BIT_LEN = 8;

    static final ByteType SIGNED_BYTE_OBJECT = new ByteType("int8", CLASS_NAME, true);
    static final ByteType SIGNED_BYTE_PRIMITIVE = new ByteType("int8", "B", true);

    static final ByteType UNSIGNED_BYTE_OBJECT = new ByteType("uint8", CLASS_NAME, false);
    static final ByteType UNSIGNED_BYTE_PRIMITIVE = new ByteType("uint8", "B", false);

    ByteType(String canonicalAbiType, String className, boolean signed) {
        super(canonicalAbiType, className, MAX_BIT_LEN, signed);
    }

    @Override
    Byte decode(byte[] buffer, int index) {
        BigInteger bi = new BigInteger(Arrays.copyOfRange(buffer, index, index + INT_LENGTH_BYTES));
        return bi.byteValueExact();
    }

    @Override
    int byteLength(Object value) {
        return roundUp(1);
    }
}
