package com.esaulpaugh.headlong.abi.beta;

import java.math.BigInteger;
import java.util.Arrays;

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
        BigInteger bi = new BigInteger(Arrays.copyOfRange(buffer, index, index + INT_LENGTH_BYTES));
        switch (bi.byteValueExact()) {
        case 0: return Boolean.FALSE;
        case 1: return Boolean.TRUE;
        default: throw new ArithmeticException("expected value 0 or 1");
        }
    }
}
