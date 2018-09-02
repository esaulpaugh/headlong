package com.esaulpaugh.headlong.abi.beta;

import java.math.BigInteger;
import java.util.Arrays;

import static com.esaulpaugh.headlong.abi.beta.ArrayType.roundUp;

class ByteType extends AbstractInt256Type<Byte> {

    static final String CLASS_NAME = Byte.class.getName();
    static final String CLASS_NAME_ELEMENT = byte[].class.getName().replaceFirst("\\[", "");

    private static final int MAX_BIT_LEN = 8;

    static final ByteType BYTE_OBJECT = new ByteType("uint8", CLASS_NAME);
    static final ByteType BYTE_PRIMITIVE = new ByteType("uint8", "B");

    ByteType(String canonicalAbiType, String className) {
        super(canonicalAbiType, className, MAX_BIT_LEN);
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
