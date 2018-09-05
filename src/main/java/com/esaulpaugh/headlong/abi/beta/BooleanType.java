package com.esaulpaugh.headlong.abi.beta;

import java.math.BigInteger;

class BooleanType extends AbstractInt256Type<Boolean> {

    private static final String CLASS_NAME = Boolean.class.getName();
    private static final String ARRAY_CLASS_NAME_STUB = boolean[].class.getName().replaceFirst("\\[", "");

    BooleanType() {
        super("bool", 1, false);
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
    Boolean decode(byte[] buffer, int index) {
        byte[] copy = new byte[INT_LENGTH_BYTES];
        System.arraycopy(buffer, index, copy, 0, INT_LENGTH_BYTES);
        BigInteger bi = new BigInteger(copy);
        switch (bi.byteValueExact()) {
        case 0: return Boolean.FALSE;
        case 1: return Boolean.TRUE;
        default: throw new ArithmeticException("expected value 0 or 1");
        }
    }
}
